from parseFiles import DataFileParser
import drawPhylogeo
import io
import json
import math
import os
from sqlalchemy import create_engine, text
from random import random
from flask import (
    Flask,
    render_template,
    request,
    url_for,
    redirect,
    send_file,
)
from celeryConfig import celery


# DB Setup
engine = create_engine("mariadb+mysqlconnector://root:toor@db:3306/phyloptimize")
dbChanges = [
    "CREATE TABLE IF NOT EXISTS solutions ("
    "id INT NOT NULL PRIMARY KEY,"
    "tree LONGTEXT NOT NULL,"
    "geo LONGTEXT NOT NULL,"
    "padding DECIMAL(4,2) NOT NULL,"
    "lType VARCHAR(2) NOT NULL,"
    "solution LONGTEXT,"
    "timeStart BIGINT,"
    "timeEnd BIGINT,"
    "connect TEXT NOT NULL"
    ");",
    "ALTER TABLE solutions ADD COLUMN public BOOL DEFAULT FALSE;",
]

with engine.connect() as conn:
    result = (
        conn.execute(
            text(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_NAME = 'settings';"
            )
        )
        .mappings()
        .all()
    )

    dbVersion = -1

    if len(result) > 0:
        result = (
            conn.execute(
                text(
                    "SELECT settingValue FROM settings WHERE settingKey = 'dbVersion';"
                )
            )
            .mappings()
            .all()
        )
        if len(result) > 0:
            dbVersion = int(result[0]["settingValue"])

    if dbVersion == -1:
        conn.execute(text("DROP TABLE IF EXISTS solutions,settings;"))
        conn.execute(
            text(
                "CREATE TABLE settings ("
                "settingKey VARCHAR(20) NOT NULL PRIMARY KEY,"
                "settingValue TEXT NOT NULL"
                ");"
            )
        )
        conn.execute(
            text(
                "INSERT INTO settings (settingKey,settingValue) VALUES ('dbVersion','0');"
            )
        )
        dbVersion = 0

    for thisIndex in range(dbVersion, len(dbChanges)):
        thisChange = dbChanges[thisIndex]
        print(thisChange)
        conn.execute(text(thisChange))

    conn.execute(
        text(
            "UPDATE settings SET settingValue=:dbVersion WHERE settingKey='dbVersion';"
        ),
        [{"dbVersion": len(dbChanges)}],
    )

    conn.commit()

app = Flask(__name__, static_folder="../web/static", template_folder="../web/templates")
app.config["SECRET_KEY"] = os.getenv("SECRET_KEY")
app.config["MAX_CONTENT_LENGTH"] = float(os.getenv("MAX_CONTENT_LENGTH"))


def giveNewId():
    newId = -1
    with engine.connect() as conn:
        result = conn.execute(text("SELECT id FROM solutions;")).mappings().all()
        isDouble = True
        while isDouble:
            newId = math.floor(random() * int(os.getenv("maxSolutions")))
            isDouble = False
            for thisId in result:
                if thisId.id == newId:
                    isDouble = True
                    break

    return newId


def parseIndexFormInput(phyloTreeFile, geoFile, formPadding, lType, connect):
    if phyloTreeFile.filename == "" or geoFile.filename == "":
        return {
            "errorIn": "A preview of your tree will be shown here.",
            "errorString": "",
        }

    try:
        phyloTreeJson = DataFileParser().newickToJson(io.TextIOWrapper(phyloTreeFile))
    except Exception as e:
        return {"errorIn": "Error while parsing tree file: ", "errorString": str(e)}

    try:
        dotIndex = geoFile.filename.rfind(".")
        # hier checken obw as kaputt geht wenn kein punkt
        extension = geoFile.filename[dotIndex : len(geoFile.filename)]
        if extension.lower() == ".geojson" or extension.lower() == ".json":
            geoJson = json.load(geoFile)
        elif extension.lower() == ".csv":
            geoJson = DataFileParser().csvToGeoJson(
                io.TextIOWrapper(geoFile, newline="")
            )
        else:
            raise Exception("Unsupported File Type")
    except Exception as e:
        return {"errorIn": "Error while parsing geo file: ", "errorString": str(e)}

    try:
        padding = int(formPadding) / 100
    except Exception as e:
        return {"errorIn": "Error while parsing padding: ", "errorString": str(e)}

    try:
        if lType != "po" and lType != "s":
            raise Exception("Leader type can only be 'po' or 's'")
    except Exception as e:
        return {"errorIn": "Error while parsing leader type: ", "errorString": str(e)}

    try:
        thisParser = DataFileParser()
        thisInstanceJson = thisParser.convertTreeAndGeoToInstance(
            phyloTreeJson, geoJson, padding, connect
        )
        thisGeoTree = thisParser.parseFile(thisInstanceJson, lType, 0)
    except Exception as e:
        return {
            "errorIn": "Error while connecting tree-leafs and geo-sites: ",
            "errorString": str(e),
        }

    return {
        "phyloTreeJson": phyloTreeJson,
        "geoJson": geoJson,
        "padding": padding,
        "lType": lType,
        "connect": connect,
        "instanceJson": thisInstanceJson,
        "geoTree": thisGeoTree,
    }


def checkForExistingSolution(pPhyloTreeJson, pGeoJson, pPadding, pLType, pConnect):
    phyloTreeJson = pPhyloTreeJson
    geoJson = pGeoJson
    padding = pPadding
    lType = pLType
    connect = pConnect

    foundId = -1

    with engine.connect() as conn:
        result = (
            conn.execute(
                text(
                    "SELECT id FROM solutions WHERE tree=:treeJson AND geo=:geoJson AND padding=:thisPadding AND lType=:thisLType AND connect=:thisConnect;"
                ),
                [
                    {
                        "treeJson": json.dumps(phyloTreeJson),
                        "geoJson": json.dumps(geoJson),
                        "thisPadding": padding,
                        "thisLType": lType,
                        "thisConnect": connect,
                    }
                ],
            )
            .mappings()
            .all()
        )

        if result:
            foundId = result[0].id

        conn.commit()

    return foundId


@app.route("/", methods=("GET", "POST"))
def index():
    if request.method == "POST":
        phyloTreeFile = request.files["phyloTreeFile"]
        geoFile = request.files["geoFile"]
        formPadding = request.form["sitesPadding"]
        lType = request.form["lType"]
        connect = request.form["connect"]

        indexParseRes = parseIndexFormInput(
            phyloTreeFile, geoFile, formPadding, lType, connect
        )

        if "errorIn" in indexParseRes:
            return "Error"

        phyloTreeJson = indexParseRes["phyloTreeJson"]
        geoJson = indexParseRes["geoJson"]
        padding = indexParseRes["padding"]
        lType = indexParseRes["lType"]
        connect = indexParseRes["connect"]

        returnId = checkForExistingSolution(
            phyloTreeJson, geoJson, padding, lType, connect
        )

        if returnId == -1:
            returnId = giveNewId()
            with engine.connect() as conn:
                conn.execute(
                    text(
                        "INSERT INTO solutions (id,tree,geo,padding,lType,connect,public) VALUES (:solutionId,:treeJson,:geoJson,:thisPadding,:thisLType,:thisConnect,:thisPublic);"
                    ),
                    [
                        {
                            "solutionId": returnId,
                            "treeJson": json.dumps(phyloTreeJson),
                            "geoJson": json.dumps(geoJson),
                            "thisPadding": padding,
                            "thisLType": lType,
                            "thisConnect": connect,
                            "thisPublic": request.form.get("public") != None,
                        }
                    ],
                )
                conn.commit()
            celery.send_task("solve", args=[returnId], kwargs={})

        return redirect(url_for("giveSolvePage") + "?id=" + str(returnId))

    return render_template("index.html")


@app.route("/solve", methods=["GET"])
def giveSolvePage():
    return render_template("solve.html")


@app.route("/checkError", methods=["POST"])
def giveErrorCheckRes():
    phyloTreeFile = request.files["phyloTreeFile"]
    geoFile = request.files["geoFile"]
    formPadding = request.form["sitesPadding"]
    lType = request.form["lType"]
    connect = request.form["connect"]

    indexParseRes = parseIndexFormInput(
        phyloTreeFile, geoFile, formPadding, lType, connect
    )

    if "errorIn" in indexParseRes:
        return render_template(
            "errorBaseless.html",
            errorIn=indexParseRes["errorIn"],
            errorString=indexParseRes["errorString"],
        )
    else:
        return render_template("unlockBtn.html")


@app.route("/preview", methods=["GET", "POST"])
def givePreview():
    thisParser = DataFileParser()

    if request.method == "POST":
        phyloTreeFile = request.files["phyloTreeFile"]
        geoFile = request.files["geoFile"]
        formPadding = request.form["sitesPadding"]
        lType = request.form["lType"]
        connect = request.form["connect"]

        indexParseRes = parseIndexFormInput(
            phyloTreeFile, geoFile, formPadding, lType, connect
        )

        if "errorIn" in indexParseRes:
            return "Error"

        phyloTreeJson = indexParseRes["phyloTreeJson"]
        geoJson = indexParseRes["geoJson"]
        padding = indexParseRes["padding"]
        lType = indexParseRes["lType"]
        connect = indexParseRes["connect"]
        thisInstanceJson = indexParseRes["instanceJson"]
        thisGeoTree = indexParseRes["geoTree"]
    else:
        with engine.connect() as conn:
            result = (
                conn.execute(
                    text("SELECT * FROM solutions WHERE id=:solutionId;"),
                    [{"solutionId": request.args.get("id")}],
                )
                .mappings()
                .all()
            )

            if len(result) == 0:
                return render_template(
                    "errorStandalone.html",
                    error="GeoTree with the given id does not exist",
                )

            phyloTreeJson = json.loads(result[0].tree)
            geoJson = json.loads(result[0].geo)
            padding = float(result[0].padding)
            lType = result[0].lType
            connect = result[0].connect

            conn.commit()

        thisInstanceJson = thisParser.convertTreeAndGeoToInstance(
            phyloTreeJson, geoJson, padding, connect
        )
        thisGeoTree = thisParser.parseFile(thisInstanceJson, lType, 0)

    shouldVerticesTurn, intersections = thisGeoTree.giveNullSolution()
    thisSolutionJson = thisParser.giveOutputJSON(
        shouldVerticesTurn,
        thisGeoTree.giveLeafOffsetAfterTurns(shouldVerticesTurn),
        intersections,
        thisGeoTree.lType,
    )

    thisOutSvg = io.StringIO()
    drawPhylogeo.draw(
        pInstance=thisInstanceJson,
        pSolution=thisSolutionJson,
        pOutput=thisOutSvg,
        pCssMode="embed",
        pLType=thisSolutionJson["lType"],
        pLeafHeight=int(os.getenv("defaultLeafHeight")),
        pInternalHeight=int(os.getenv("defaultInternalHeight")),
        pExtraLeft=int(os.getenv("defaultExtraLeft")),
        pExtraRight=int(os.getenv("defaultExtraRight")),
        pExtraBot=int(os.getenv("defaultExtraBot")),
        pBackgroundMode=os.getenv("defaultBackgroundMode"),
        pBranchLengthMode=os.getenv("defaultBranchLengthMode")
    )

    thisOutSvgAsBytes = io.BytesIO()
    thisOutSvgAsBytes.write(thisOutSvg.getvalue().encode())
    thisOutSvgAsBytes.seek(0)
    thisOutSvg.close()

    return send_file(thisOutSvgAsBytes, download_name="optimizedTree.svg")


@app.route("/loading", methods=["GET"])
def loading():
    with engine.connect() as conn:
        result = (
            conn.execute(
                text("SELECT solution FROM solutions WHERE id=:solutionId;"),
                [{"solutionId": request.args.get("id")}],
            )
            .mappings()
            .all()
        )
        solutionReady = result[0].solution is not None

        conn.commit()

    if solutionReady:
        return redirect(url_for("draw") + "?id=" + str(request.args.get("id")))
    else:
        return render_template("loading.html")


@app.route("/edit", methods=["GET", "POST"])
def edit():
    if request.method == "POST":
        with engine.connect() as conn:
            result = (
                conn.execute(
                    text("SELECT * FROM solutions WHERE id=:solutionId;"),
                    [{"solutionId": request.args.get("id")}],
                )
                .mappings()
                .all()
            )
            if len(result) == 0:
                return render_template(
                    "errorStandalone.html",
                    error="GeoTree with the given id does not exist",
                )

            phyloTreeJson = json.loads(result[0].tree)
            geoJson = json.loads(result[0].geo)
            padding = int(request.form["sitesPadding"]) / 100
            lType = request.form["lType"]
            connect = result[0].connect

            thisPublic = result[0].public

            conn.commit()

        returnId = checkForExistingSolution(
            phyloTreeJson, geoJson, padding, lType, connect
        )

        if returnId == -1:
            returnId = giveNewId()
            with engine.connect() as conn:
                conn.execute(
                    text(
                        "INSERT INTO solutions (id,tree,geo,padding,lType,connect,public) VALUES (:solutionId,:treeJson,:geoJson,:thisPadding,:thisLType,:thisConnect,:thisPublic);"
                    ),
                    [
                        {
                            "solutionId": returnId,
                            "treeJson": json.dumps(phyloTreeJson),
                            "geoJson": json.dumps(geoJson),
                            "thisPadding": padding,
                            "thisLType": lType,
                            "thisConnect": connect,
                            "thisPublic": thisPublic,
                        }
                    ],
                )
                conn.commit()
            celery.send_task("solve", args=[returnId], kwargs={})

        return redirect(url_for("giveSolvePage") + "?id=" + str(returnId))

    with engine.connect() as conn:
        result = (
            conn.execute(
                text("SELECT * FROM solutions WHERE id=:solutionId;"),
                [{"solutionId": request.args.get("id")}],
            )
            .mappings()
            .all()
        )

        if len(result) == 0:
            return render_template(
                "errorStandalone.html",
                error="GeoTree with the given id does not exist",
            )

        lType = result[0].lType
        padding = float(result[0].padding)

        conn.commit()

    return render_template(
        "edit.html",
        defaults={
            "selectedPo": lType == "po",
            "selectedS": lType == "s",
            "padding": math.floor(padding * 100),
        },
    )


@app.route("/example", methods=["GET"])
def impress():
    return render_template("example.html")


@app.route("/list", methods=["GET"])
def treeList():
    solutionsInCookies = request.cookies.get("solutions")

    if solutionsInCookies == None:
        solutionsInCookiesJSON = []
    else:
        solutionsInCookiesJSON = json.loads(solutionsInCookies)

    with engine.connect() as conn:
        result = (
            conn.execute(text("SELECT id FROM solutions WHERE public = TRUE;"))
            .mappings()
            .all()
        )

        conn.commit()

    return render_template(
        "list.html", solutions=result, privateSolutions=solutionsInCookiesJSON
    )

@app.route("/cite", methods=["GET"])
def cite():
    return render_template("cite.html")


@app.route("/draw", methods=["GET"])
def draw():
    with engine.connect() as conn:
        result = (
            conn.execute(
                text("SELECT * FROM solutions WHERE id=:solutionId;"),
                [{"solutionId": request.args.get("id")}],
            )
            .mappings()
            .all()
        )

        if len(result) == 0:
            return render_template(
                "errorStandalone.html",
                error="GeoTree with the given id does not exist",
            )

        thisInstanceJson = DataFileParser().convertTreeAndGeoToInstance(
            json.loads(result[0].tree),
            json.loads(result[0].geo),
            float(result[0].padding),
            result[0].connect,
        )
        thisSolutionJson = json.loads(result[0].solution)

        conn.commit()

    if request.args.get("lh"):
        thisLeafHeight = int(float(request.args.get("lh")))
    else:
        thisLeafHeight = int(os.getenv("defaultLeafHeight"))

    if request.args.get("bm"):
        thisBackgroundMode = request.args.get("bm")
    else:
        thisBackgroundMode = os.getenv("defaultBackgroundMode")

    if request.args.get("ih"):
        thisInternalHeight = int(float(request.args.get("ih")))
    else:
        thisInternalHeight = int(os.getenv("defaultInternalHeight"))

    if request.args.get("el"):
        thisExtraLeft = int(float(request.args.get("el")))
    else:
        thisExtraLeft = int(os.getenv("defaultExtraLeft"))

    if request.args.get("er"):
        thisExtraRight = int(float(request.args.get("er")))
    else:
        thisExtraRight = int(os.getenv("defaultExtraRight"))

    if request.args.get("eb"):
        thisExtraBot = int(float(request.args.get("eb")))
    else:
        thisExtraBot = int(os.getenv("defaultExtraBot"))

    if request.args.get("blm"):
        thisBranchLengthMode = request.args.get("blm")
    else:
        thisBranchLengthMode = os.getenv("defaultBranchLengthMode")

    if (
        thisBranchLengthMode == "custom"
        and thisInstanceJson["maxCumBranchLength"] == math.inf
    ):
        return render_template(
            "errorBaseless.html",
            errorIn="ERROR: Can not draw custom branch length",
            errorString="Branch length missing for some nodes. \nPlease make sure the branch length is set in the Newick tree file",
        )

    thisOutSvg = io.StringIO()
    drawPhylogeo.draw(
        pInstance=thisInstanceJson,
        pSolution=thisSolutionJson,
        pOutput=thisOutSvg,
        pCssMode="embed",
        pLType=thisSolutionJson["lType"],
        pLeafHeight=thisLeafHeight,
        pInternalHeight=thisInternalHeight,
        pExtraLeft=thisExtraLeft,
        pExtraRight=thisExtraRight,
        pExtraBot=thisExtraBot,
        pBackgroundMode=thisBackgroundMode,
        pBranchLengthMode=thisBranchLengthMode,
    )

    thisOutSvgAsBytes = io.BytesIO()
    thisOutSvgAsBytes.write(thisOutSvg.getvalue().encode())
    thisOutSvgAsBytes.seek(0)
    thisOutSvg.close()

    return send_file(thisOutSvgAsBytes, download_name="optimizedTree.svg")
