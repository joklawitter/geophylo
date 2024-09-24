from parseFiles import DataFileParser
from gurobiFunctions import giveMinLeaderIntersectConfig
import argparse
import json

if __name__ == "__main__":
    ### HANDLE COMMANDLINE ARGUMENTS
    aparser = argparse.ArgumentParser()

    # input/output
    aparser.add_argument(
        "instance", help="Path to JSON file with the geophylo instance."
    )
    aparser.add_argument(
        "-o", "--output", help="Output JSON to a file. (Default is standard out.)"
    )
    aparser.add_argument(
        "-l", "--ltype", help="Leader Type: s (default) or po", default="s"
    )
    aparser.add_argument(
        "-g",
        "--pogap",
        help="Min Gap between Horizontal Lines for PO-Leaders",
        type=float,
        default=0,
    )

    args = aparser.parse_args()

    instanceFileName = args.instance

    # setup: output to file if args say so, otherwise stdout
    if args.output == None:
        from sys import stdout as stdout

        outputStream = stdout
    else:
        outputStream = open(args.output, "w", encoding="utf-8")

    thisParser = DataFileParser()

    thisGeoTree = thisParser.parseFile(
        json.load(open(instanceFileName)), args.ltype, args.pogap
    )
    shouldVerticesTurn, intersections = giveMinLeaderIntersectConfig(thisGeoTree)
    thisOutputJSONString = json.dumps(
        thisParser.giveOutputJSON(
            shouldVerticesTurn,
            thisGeoTree.giveLeafOffsetAfterTurns(shouldVerticesTurn),
            intersections,
            thisGeoTree.lType,
        )
    )

    outputStream.write(thisOutputJSONString)

    print("Done.")
