from celeryConfig import celery
from parseFiles import DataFileParser
from gurobiFunctions import giveMinLeaderIntersectConfig
import json
from sqlalchemy import create_engine, text

engine = create_engine("mariadb+mysqlconnector://root:toor@db:3306/phyloptimize")


@celery.task(name="solve")
def solve(pId):
    with engine.connect() as conn:
        result = (
            conn.execute(
                text("SELECT * FROM solutions WHERE id=:solutionId;"),
                [{"solutionId": pId}],
            )
            .mappings()
            .all()
        )
        phyloTreeJson = json.loads(result[0].tree)
        geoJson = json.loads(result[0].geo)
        padding = float(result[0].padding)
        lType = result[0].lType
        connect = result[0].connect
        conn.commit()

    thisParser = DataFileParser()
    thisInstanceJson = thisParser.convertTreeAndGeoToInstance(
        phyloTreeJson, geoJson, padding, connect
    )
    thisGeoTree = thisParser.parseFile(thisInstanceJson, lType, 0)
    shouldVerticesTurn, intersections = giveMinLeaderIntersectConfig(thisGeoTree)
    thisSolutionJson = thisParser.giveOutputJSON(
        shouldVerticesTurn,
        thisGeoTree.giveLeafOffsetAfterTurns(shouldVerticesTurn),
        intersections,
        thisGeoTree.lType,
    )

    with engine.connect() as conn:
        conn.execute(
            text("UPDATE solutions SET solution=:solutionJson WHERE id=:solutionId;"),
            [{"solutionId": pId, "solutionJson": json.dumps(thisSolutionJson)}],
        )
        conn.commit()
