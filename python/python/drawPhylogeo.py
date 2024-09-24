import json
import svgwrite as svg
import argparse
from pyproj import Transformer
from datauri import DataURI
import tempfile
import requests
import shutil
import os

TRAN_3857_TO_4326 = Transformer.from_crs("EPSG:3857", "EPSG:4326", always_xy=True)


def transformToLonLat(mercatorX, mercatorY):
    return TRAN_3857_TO_4326.transform(mercatorX, mercatorY)


def giveBackgroundMapMercator(pX1, pY1, pX2, pY2, mapWidth, mapHeight, pScale):
    point1 = transformToLonLat(pX1, pY1)
    point2 = transformToLonLat(pX2, pY2)
    return giveBackgroundMap(
        -point1[1],
        point1[0],
        -point2[1],
        point2[0],
        mapWidth * pScale,
        mapHeight * pScale,
    )


def giveBackgroundMap(pLat1, pLng1, pLat2, pLng2, mapWidth, mapHeight):
    geoUrl = ""
    geoBaseUrl = "https://maps.geoapify.com/v1/staticmap?"
    geoUrl += geoBaseUrl

    geoApiKey = os.getenv("GeoAPI")
    geoUrl += "apiKey=" + geoApiKey

    geoStyle = "osm-bright-smooth"
    geoUrl += "&style=" + geoStyle

    geoWidth = mapWidth
    geoUrl += "&width=" + str(round(geoWidth))

    geoHeight = mapHeight
    geoUrl += "&height=" + str(round(geoHeight))

    geoUrl += (
        "&area=rect:"
        + str(pLng1)
        + ","
        + str(pLat1)
        + ","
        + str(pLng2)
        + ","
        + str(pLat2)
    )

    print(geoUrl)

    response = requests.get(geoUrl, stream=True)

    return response.raw


def draw(
    pInstance,
    pSolution,
    pOutput,
    pLeafHeight=10,
    pInternalHeight=3,
    pLType="s",
    pCssFile="../css/geophylo.css",
    pCssMode="link",
    pExtraLeft=0,
    pExtraBot=0,
    pExtraRight=0,
    pBackgroundMode="none",
    pBranchLengthMode="custom",
):
    inst = pInstance
    sol = pSolution
    svg_stream = pOutput

    ### READ THE ORIGINAL INSTANCE (JSON)
    extraLeftAbs = pExtraLeft * inst["map_width"] / 100
    extraRightAbs = pExtraRight * inst["map_width"] / 100
    extraBotAbs = pExtraBot * inst["map_height"] / 100
    map_title = inst["title"]
    n_sites = inst["num_leaves"]
    n_slots = n_sites
    x_max = inst["map_width"]
    y_max = inst["map_height"]
    if "left_coord" in inst.keys():
        x_min = inst["left_coord"]
        y_min = inst["top_coord"]
    else:
        x_min = 0
        y_min = 0
    sites = inst["sites"]
    maxCumBranchLength = inst["maxCumBranchLength"]

    # scale to put leaf number 0 at x=0 and leaf number n_sites at x=x_max
    leaf_x_scale = x_max / (n_sites - 1)
    leaf_x_scale2 = x_max / (n_sites + 1)

    def leaf_x(i):
        # return leaf_x_scale * i
        return leaf_x_scale2 * (i + 1)

    ### READ THE SOLUTION (JSON)
    leaf_pos = sol["leaf_pos"]

    ### draw

    fontSize = leaf_x_scale2 - 0.5

    def draw_tree(self, gr):
        if self["leaf"]:
            leaf_label = gr.add(
                gr.g(
                    id="leaf-{}".format(self["id"]),
                    class_="leaf",
                    style="font-size:" + str(fontSize) + "px !important;",
                )
            )
            real_x = leaf_x(sol["leaf_pos"][str(self["id"])])
            leaf_label.translate(real_x, 0)
            leaf_label.add(gr.text(self["label"], (0, 0), class_="label"))
            # leader
            site = inst["sites"][self["site_id"]]
            leaf_port = sol["leaf_pos"][str(self["id"])]

            if pLType == "s":
                dwg.add(
                    dwg.line(
                        (leaf_x(leaf_port), 0),
                        [site["x"] - x_min, site["y"] - y_min],
                        class_="leader",
                    )
                )
            elif pLType == "po":
                dwg.add(
                    dwg.polyline(
                        [
                            (leaf_x(leaf_port), 0),
                            (leaf_x(leaf_port), site["y"]),
                            [site["x"] - x_min, site["y"] - y_min],
                        ],
                        class_="leader",
                    )
                )

            if pBranchLengthMode == "autoAlign":
                return (real_x, 0)
            else:
                newY = (
                    self["cum_branch_length"] - maxCumBranchLength
                ) * pInternalHeight

                return (real_x, newY)

        else:
            pL = draw_tree(self["left"], gr)
            pR = draw_tree(self["right"], gr)

            if pBranchLengthMode == "autoAlign":
                new_y = min(pL[1], pR[1]) - pInternalHeight

                if self["left"]["leaf"] or self["right"]["leaf"]:
                    # give at least leaf_height space for leafs
                    new_y = min(new_y, -pLeafHeight)
            else:
                new_y = (
                    self["cum_branch_length"] - maxCumBranchLength
                ) * pInternalHeight

            gr.add(
                gr.path(
                    "M {} {} V {} H {} V {}".format(pL[0], pL[1], new_y, pR[0], pR[1]),
                    class_="tree",
                )
            )
            return (0.5 * (pL[0] + pR[0]), new_y)

    dwg = svg.Drawing()

    # how to include stylesheet?
    if pCssMode == "link":
        dwg.add_stylesheet(pCssFile, "geophylo-style")
    elif pCssMode == "import":
        dwg.embed_stylesheet("@import url({}).css);".format(pCssFile))
    elif pCssMode == "embed":
        try:
            with open(pCssFile) as f:
                dwg.embed_stylesheet(f.read())
        except:
            print("Error reading CSS file:", pCssFile, ". Not including any CSS.")
    elif pCssMode == "none":
        pass
    else:
        print("Unknown stylesheet option ({})".format(pCssMode))
        exit()

    site_marker = dwg.g(id="site-marker")
    dwg.defs.add(site_marker)
    site_marker.add(dwg.circle((0, 0), r=1, class_="site-marker-symbol"))

    # background
    if  pBackgroundMode == "osmEmbed":
        extraLeftMerc = (
            pExtraLeft * (inst["mercator_max_x"] - inst["mercator_min_x"]) / 100
        )
        extraRightMerc = (
            pExtraRight * (inst["mercator_max_x"] - inst["mercator_min_x"]) / 100
        )
        extraBotMerc = (
            pExtraBot * (inst["mercator_max_y"] - inst["mercator_min_y"]) / 100
        )
        backgroundImgTempFile = tempfile.NamedTemporaryFile(suffix=".jpg")
        backgroundImg = giveBackgroundMapMercator(
            inst["mercator_min_x"] - extraLeftMerc,
            inst["mercator_min_y"],
            inst["mercator_max_x"] + extraRightMerc,
            inst["mercator_max_y"] + extraBotMerc,
            inst["map_width"] + extraLeftAbs + extraRightAbs,
            inst["map_height"] + extraBotAbs,
            5,
        )

        with open(backgroundImgTempFile.name, "bw") as f:
            shutil.copyfileobj(backgroundImg, f)
            f.seek(0)

        backgroundImgDataURI = DataURI.from_file(backgroundImgTempFile.name)

        backgroundImgDataURIString = str(backgroundImgDataURI)

        dwg.add(
            dwg.image(
                href=backgroundImgDataURIString,
                insert=(-extraLeftAbs, 0),
                size=(
                    inst["map_width"] + extraLeftAbs + extraRightAbs,
                    inst["map_height"] + extraBotAbs,
                ),
            )
        )
    elif pBackgroundMode == "none":
        dwg.add(
            dwg.rect(
                insert=(-extraLeftAbs, 0),
                size=(
                    inst["map_width"] + extraLeftAbs + extraRightAbs,
                    inst["map_height"] + extraBotAbs,
                ),
                class_="geo-background",
            )
        )
    else:
        print("Unknown background option ({})".format(pBackgroundMode))
        exit()

    # tree
    root_pos = draw_tree(inst["tree"], dwg)

    # sites
    for i, site in enumerate(sites):
        site_group = dwg.add(dwg.g(id="site-{}".format(i), class_="site"))
        site_group.translate([site["x"] - x_min, site["y"] - y_min])
        site_group.add(dwg.use(site_marker, insert=(0, 0), class_="marker"))

    # save
    dwg.viewbox(
        -1 - extraLeftAbs,
        root_pos[1] - 1,
        x_max + 2 + extraLeftAbs + extraRightAbs,
        y_max - root_pos[1] + pInternalHeight + 1 + extraBotAbs,
    )
    dwg.write(svg_stream, pretty=True)

if __name__ == "__main__":
    ### HANDLE COMMANDLINE ARGUMENTS

    aparser = argparse.ArgumentParser()
    # input/output
    aparser.add_argument("instance", help="Path to JSON file with the geophylo instance.")
    aparser.add_argument("solution", help="Path to JSON file with the solution from the ILP.")
    aparser.add_argument(
        "-o", "--output", help="Output SVG to a file. (Default is standard out.)"
    )

    # drawing parameters
    aparser.add_argument(
        "--leaf-height", type=float, default=10, help="Height of the stem at leafs."
    )
    aparser.add_argument(
        "-i",
        "--internal-height",
        type=float,
        default=3,
        help="Minimum height of internal stems.",
    )
    aparser.add_argument(
        "-l",
        "--ltype",
        default="s",
        help='Leader Type: "s" for straight S leaders (default) or "po" for PO leaders.',
    )

    # css parameters
    aparser.add_argument(
        "-c",
        "--css-file",
        type=str,
        default="../css/geophylo.css",
        help="CSS file with style information. (Default: ../css/geophylo.css)",
    )
    aparser.add_argument(
        "-m",
        "--css-mode",
        type=str,
        default="embed",
        help="How to use the CSS file?"
        ' // "link" (default): use <xml-stylesheet> tag.'
        ' // "import": use @import in <style> tag.'
        ' // "embed": read the CSS now and copy it into <style> tag.'
        ' // "none": do not add any CSS.',
    )
    aparser.add_argument(
        "-el",
        "--extra-left",
        type=int,
        default=0,
        help="Extra background left of the sites in percent.",
    )
    aparser.add_argument(
        "-eb",
        "--extra-bot",
        type=int,
        default=0,
        help="Extra background below the sites in percent.",
    )
    aparser.add_argument(
        "-er",
        "--extra-right",
        type=int,
        default=0,
        help="Extra background right of the sites in percent.",
    )
    aparser.add_argument(
        "-bm",
        "--background-mode",
        type=str,
        default="none",
        help="Style of the background. Either none or osmEmbed for a static map",
    )
    aparser.add_argument(
        "-blm",
        "--branch-length-mode",
        type=str,
        default="autoAlign",
        help="Should the leafs align (autoAlign) or use the branch length specified in the tree file (custom)",
    )

    args = aparser.parse_args()

    with open(args.instance) as f:
        instanceJson = json.load(f)

    with open(args.solution) as f:
        solutionJson = json.load(f)

    # setup: output to file if args say so, otherwise stdout
    if args.output == None:
        from sys import stdout as stdout

        svg_stream = stdout
    else:
        svg_stream = open(args.output, "w", encoding="utf-8")

    draw(
        instanceJson,
        solutionJson,
        svg_stream,
        args.leaf_height,
        args.internal_height,
        args.ltype,
        args.css_file,
        args.css_mode,
        args.extra_left,
        args.extra_bot,
        args.extra_right,
        args.background_mode,
        args.branch_length_mode,
    )

    print("Done.")
