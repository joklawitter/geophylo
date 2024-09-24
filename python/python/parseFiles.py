import argparse
import json
import math
import numpy as np
from Bio import Phylo
from pyproj import Transformer
import csv
from geojson import Feature, Point, FeatureCollection

TRAN_4326_TO_3857 = Transformer.from_crs("EPSG:4326", "EPSG:3857", always_xy=True)


def transformToMercator(lon, lat):
    return TRAN_4326_TO_3857.transform(lon, lat)


def getId(obj):
    return obj[0]


class Vertex(object):
    def __init__(self, pType, pId):
        self.type = pType
        self.children = []
        self.allParents = []
        self.id = pId

    def incSubTreeWidth(self):
        self.subTreeWidth += 1
        if self.type != "root":
            self.allParents[0][0].incSubTreeWidth()

    def addChild(self, pChild):
        self.children.append(pChild)
        pChild.allParents = [[self, len(self.children) == 1]]
        pChild.allParents.extend(self.allParents)
        pChild.geoTree = self.geoTree

        if pChild.type == "leaf":
            self.incSubTreeWidth()
            self.geoTree.leafs.append(pChild)
            self.geoTree.sites.append(pChild.site)
            pChild.totalIndex = len(self.geoTree.leafs) - 1
        else:
            self.geoTree.innerVertices.append(pChild)
            pChild.totalIndex = len(self.geoTree.innerVertices) - 1

    def giveOffsetAfterTurns(self, pTurns):
        offset = 0

        for thisParent in self.allParents:
            if (not thisParent[1]) ^ pTurns[thisParent[0].totalIndex][1]:
                # i Am in Right Subtree After Turns
                otherChildIndex = int(thisParent[1])

                offset += thisParent[0].children[otherChildIndex].subTreeWidth

        return offset

    def __str__(self):
        return "Vertex: " + self.id

    def __repr__(self):
        return self.__str__()

    @property
    def parentCoef(self):
        coef = []

        for thisParent in self.allParents:
            if thisParent[1]:
                # I am in Left Subtree
                coef.append(thisParent[0].children[1].subTreeWidth)
            else:
                # i Am in Right Subtree
                coef.append(-thisParent[0].children[0].subTreeWidth)

        return coef

    @property
    def initialOffset(self):
        offset = 0

        for thisParent in self.allParents:
            if not thisParent[1]:
                # i Am in Right Subtree
                otherChildIndex = 0

                offset += thisParent[0].children[otherChildIndex].subTreeWidth

        return offset

    pass


class InnerVertex(Vertex):
    def __init__(self, pId):
        self.subTreeWidth = 0
        return super().__init__("inner", pId)

    pass


class Leaf(Vertex):
    def __init__(self, pSite, pId):
        self.subTreeWidth = 1
        self.site = pSite
        pSite.leaf = self
        return super().__init__("leaf", pId)

    pass


class RootVertex(Vertex):
    def __init__(self):
        self.subTreeWidth = 0
        self.totalIndex = 0
        return super().__init__("root", 0)

    pass


class Site(object):
    def __init__(self, pPos, pName):
        self.pos = pPos
        self.name = pName

    def __str__(self):
        return "Site: " + self.name

    def __repr__(self):
        return self.__str__()

    pass


class DataFileParser(object):
    def newickToJsonRecursionStep(self, pClade, pMaxId, pMaxSiteId, pCumBranchLength):
        thisBranchLength = pClade.branch_length
        if thisBranchLength is None:
            thisBranchLength = math.inf

        newCumBranchLength = pCumBranchLength + thisBranchLength
        if len(pClade.clades) == 0:
            return [
                {
                    "leaf": True,
                    "id": pMaxId + 1,
                    "label": pClade.name,
                    "site_id": pMaxSiteId + 1,
                    "cum_branch_length": newCumBranchLength,
                },
                pMaxSiteId + 1,
                [pClade.name],
                newCumBranchLength,
            ]
        else:
            outputSubTree = {"leaf": False, "cum_branch_length": newCumBranchLength}
            leftRecursionRes = self.newickToJsonRecursionStep(
                pClade.clades[0], pMaxId, pMaxSiteId, newCumBranchLength
            )
            outputSubTree["left"] = leftRecursionRes[0]
            rightRecursionRes = self.newickToJsonRecursionStep(
                pClade.clades[1],
                outputSubTree["left"]["id"],
                leftRecursionRes[1],
                newCumBranchLength,
            )
            outputSubTree["right"] = rightRecursionRes[0]
            outputSubTree["id"] = outputSubTree["right"]["id"] + 1
            return [
                outputSubTree,
                rightRecursionRes[1],
                leftRecursionRes[2] + rightRecursionRes[2],
                max(leftRecursionRes[3], rightRecursionRes[3]),
            ]

    def newickToJson(self, pNewick, pName=""):
        newickTree = Phylo.read(pNewick, "newick")
        recursionRes = self.newickToJsonRecursionStep(newickTree.root, -1, -1, 0)
        if "name" in pNewick:
            newickName = pNewick.name
        else:
            newickName = pName
        return {
            "title": newickName,
            "tree": recursionRes[0],
            "num_leaves": recursionRes[1] + 1,
            "namesOrder": recursionRes[2],
            "maxCumBranchLength": recursionRes[3],
        }

    def csvToGeoJson(self, pCsv):
        csvGeo = csv.DictReader(pCsv, lineterminator="\r\n")
        features = []

        for row in csvGeo:
            thisFeature = Feature(
                geometry=Point((float(row["lat"]), float(row["lon"]))), properties=row
            )
            features += [thisFeature]

        thisFeatureCollection = FeatureCollection(features)

        return thisFeatureCollection

    def addSubtree(self, pSubtree, pParent):
        if pSubtree["leaf"]:
            pParent.addChild(Leaf(self.sites[pSubtree["site_id"]], pSubtree["id"]))
        else:
            thisChild = InnerVertex(pSubtree["id"])
            pParent.addChild(thisChild)

            self.addSubtree(pSubtree["left"], thisChild)
            self.addSubtree(pSubtree["right"], thisChild)

    def giveOutputJSON(
        self, pShouldInnerVerticesTurn, pLeafPosAfterTurns, pIntersections, pLType
    ):
        should_rotate = {}
        for [id, rot] in pShouldInnerVerticesTurn:
            should_rotate[id] = rot

        outputObject = {
            "num_intersections": pIntersections,
            "leaf_pos": pLeafPosAfterTurns,
            "should_rotate": should_rotate,
            "lType": pLType,
        }

        return outputObject

    def convertTreeAndGeoToInstance(
        self, pPhyloTreeFile, pGeoFile, pRelPadding, pAssignSitesBy
    ):
        thisPhyloTreeJson = pPhyloTreeFile
        thisGeoJson = pGeoFile

        outputObject = {
            "title": thisPhyloTreeJson["title"],
            "tree": thisPhyloTreeJson["tree"],
            "sites": [],
            "num_leaves": thisPhyloTreeJson["num_leaves"],
            "maxCumBranchLength": thisPhyloTreeJson["maxCumBranchLength"],
        }

        # Autofit
        maxX = -math.inf
        maxY = -math.inf
        minX = math.inf
        minY = math.inf

        if pAssignSitesBy == "":
            if len(thisGeoJson["features"]) < thisPhyloTreeJson["num_leaves"]:
                raise Exception("There are not enough sites.")

            for thisFeature in thisGeoJson["features"]:
                mercatorX, mercatorY = transformToMercator(
                    thisFeature["geometry"]["coordinates"][0],
                    thisFeature["geometry"]["coordinates"][1],
                )

                thisSiteObject = {"x": mercatorX, "y": -mercatorY}

                outputObject["sites"].append(thisSiteObject)

                if thisSiteObject["x"] > maxX:
                    maxX = thisSiteObject["x"]
                if thisSiteObject["x"] < minX:
                    minX = thisSiteObject["x"]
                if thisSiteObject["y"] > maxY:
                    maxY = thisSiteObject["y"]
                if thisSiteObject["y"] < minY:
                    minY = thisSiteObject["y"]
        else:
            for thisName in thisPhyloTreeJson["namesOrder"]:
                foundSite = False

                for thisFeature in thisGeoJson["features"]:
                    if (
                        pAssignSitesBy in thisFeature["properties"]
                        and thisFeature["properties"][pAssignSitesBy] == thisName
                    ):
                        foundSite = True
                        mercatorX, mercatorY = transformToMercator(
                            thisFeature["geometry"]["coordinates"][0],
                            thisFeature["geometry"]["coordinates"][1],
                        )

                        thisSiteObject = {"x": mercatorX, "y": -mercatorY}

                        outputObject["sites"].append(thisSiteObject)

                        if thisSiteObject["x"] > maxX:
                            maxX = thisSiteObject["x"]
                        if thisSiteObject["x"] < minX:
                            minX = thisSiteObject["x"]
                        if thisSiteObject["y"] > maxY:
                            maxY = thisSiteObject["y"]
                        if thisSiteObject["y"] < minY:
                            minY = thisSiteObject["y"]

                        break
                if not foundSite:
                    raise Exception(
                        "The leaf "
                        + thisName
                        + " has no corresponding site with "
                        + pAssignSitesBy
                        + "="
                        + thisName
                    )

        padding = max((maxX - minX) * pRelPadding, (maxY - minY) * pRelPadding)
        mapLeft = minX - padding
        mapTop = minY - padding
        mapWidth = maxX - minX + 2 * padding
        mapHeight = maxY - minY + 2 * padding

        scale = min(100 / mapWidth, 100 / mapHeight)

        for thisSite in outputObject["sites"]:
            thisSite["x"] = (thisSite["x"] - mapLeft) * scale
            thisSite["y"] = (thisSite["y"] - mapTop) * scale

        # the mercator bbox is needed for the background map
        outputObject["mercator_min_x"] = mapLeft
        outputObject["mercator_min_y"] = mapTop
        outputObject["mercator_max_x"] = mapLeft + mapWidth
        outputObject["mercator_max_y"] = mapTop + mapHeight
        outputObject["left_coord"] = 0
        outputObject["top_coord"] = 0
        outputObject["map_width"] = mapWidth * scale
        outputObject["map_height"] = mapHeight * scale

        return outputObject

    def parseFile(self, pJson, pLType, pPoGap):
        importedData = pJson
        self.coords = importedData["sites"]

        self.x_max = importedData["map_width"]
        self.y_max = importedData["map_height"]

        n_sites = len(self.coords)
        leaf_x_scale = self.x_max / (n_sites + 1)
        left = leaf_x_scale
        right = n_sites * leaf_x_scale
        resTree = GeoTree([left, 0], [right, 0], pLType, pPoGap)

        self.sites = []
        for thisSite in self.coords:
            self.sites.append(Site([thisSite["x"], thisSite["y"]], ""))

        resTree.innerVertices[0].id = importedData["tree"]["id"]

        self.addSubtree(importedData["tree"]["left"], resTree.innerVertices[0])
        self.addSubtree(importedData["tree"]["right"], resTree.innerVertices[0])

        return resTree


class GeoTree(object):
    def __init__(self, pTopLineStart, pTopLineEnd, pLType, pPoGap):
        self.sites = []
        self.leafs = []
        self.innerVertices = [RootVertex()]
        self.topLineStart = pTopLineStart
        self.topLineEnd = pTopLineEnd
        self.innerVertices[0].geoTree = self
        self.lType = pLType
        self.poGap = pPoGap

    def giveTwoSitesTopLineIntersectIndex(self, pSite1Pos, pSite2Pos):
        intermRes = [
            (self.topLineStart[1] - self.topLineEnd[1]) * (pSite1Pos[0] - pSite2Pos[0])
            - (pSite1Pos[1] - pSite2Pos[1])
            * (self.topLineStart[0] - self.topLineEnd[0])
        ]
        if intermRes[0] == 0:
            # sites are parallel to TopLine. We have to still check which one is left
            if np.sign(pSite2Pos[0] - pSite1Pos[0]) == np.sign(
                self.topLineEnd[0] - self.topLineStart[0]
            ):
                # they point in the same direction -> site 1 is left -> the imaginary intersect index is positive
                return [self.innerVertices[0].subTreeWidth + 1, True]
            else:
                return [-1, True]
        else:
            intersectPercent = (
                (pSite1Pos[1] - pSite2Pos[1]) * (pSite1Pos[0] - self.topLineStart[0])
                - (pSite1Pos[1] - self.topLineStart[1]) * (pSite1Pos[0] - pSite2Pos[0])
            ) / intermRes[0]

            # for determining the order of the sites from left to right, it is important if the direction vector from 1 to 2 has been traversed positive or negative
            intermRes.append(pSite2Pos[0] - pSite1Pos[0])
            if intermRes[1] == 0:
                site1Lower = (
                    self.topLineStart[1]
                    - pSite1Pos[1]
                    + intersectPercent * (self.topLineEnd[1] - self.topLineStart[1])
                ) / (pSite2Pos[1] - pSite1Pos[1]) > 0
            else:
                site1Lower = (
                    self.topLineStart[0]
                    - pSite1Pos[0]
                    + intersectPercent * (self.topLineEnd[0] - self.topLineStart[0])
                ) / (pSite2Pos[0] - pSite1Pos[0]) > 0
            if self.lType == "s":
                return [
                    intersectPercent * (self.innerVertices[0].subTreeWidth - 1),
                    site1Lower,
                ]
            elif self.lType == "po":
                if site1Lower:
                    return [
                        (pSite2Pos[0] - self.topLineStart[0])
                        / (self.topLineEnd[0] - self.topLineStart[0])
                        * (self.innerVertices[0].subTreeWidth - 1),
                        site1Lower,
                    ]
                else:
                    return [
                        (pSite1Pos[0] - self.topLineStart[0])
                        / (self.topLineEnd[0] - self.topLineStart[0])
                        * (self.innerVertices[0].subTreeWidth - 1),
                        site1Lower,
                    ]

    def giveLowestCommonParentVertex(self, pVertex1, pVertex2):
        vertex1Parents = pVertex1.allParents
        vertex2Parents = pVertex2.allParents

        for thisVertex1Parent in vertex1Parents:
            for thisVertex2Parent in vertex2Parents:
                if thisVertex1Parent[0] is thisVertex2Parent[0]:
                    return thisVertex1Parent

        raise Exception("Two Vertices have no common parent")

    def giveLeafOffsetAfterTurns(self, pTurns):
        res = {}

        for thisLeaf in self.leafs:
            res[str(thisLeaf.id)] = thisLeaf.giveOffsetAfterTurns(pTurns)

        return res

    def giveNullSolution(self):
        res = [[], -1]

        for thisInnerVertexIndex in range(len(self.innerVertices)):
            res[0].append(
                [
                    self.innerVertices[thisInnerVertexIndex].id,
                    False,
                ]
            )

        return res

    pass


if __name__ == "__main__":
    ### HANDLE COMMANDLINE ARGUMENTS
    aparser = argparse.ArgumentParser()

    # input/output
    aparser.add_argument("tree", help="Path to NEWICK file with the phylogenetic tree.")
    aparser.add_argument("geo", help="Path to GEOJSON file with the sites.")
    aparser.add_argument(
        "-o", "--output", help="Output JSON to a file. (Default is standard out.)"
    )
    aparser.add_argument(
        "-p",
        "--padding",
        help="Padding around sites in percentage of area enclosed by sites",
        default="20",
    )
    aparser.add_argument(
        "-l", "--ltype", help="Leader Type: s (default) or po", default="s"
    )
    aparser.add_argument(
        "-c",
        "--connect",
        help="Attribute in geo file to connect leafs and sites by. Defaults to order in which they appear.",
        default="",
    )

    args = aparser.parse_args()

    # setup: output to file if args say so, otherwise stdout
    if args.output == None:
        from sys import stdout as stdout

        outputStream = stdout
    else:
        outputStream = open(args.output, "w", encoding="utf-8")

    phyloTreeJson = DataFileParser().newickToJson(args.tree, args.tree)

    dotIndex = args.geo.rfind(".")
    extension = args.geo[dotIndex : len(args.geo)]
    with open(args.geo) as geoFile:
        if extension.lower() == ".geojson" or extension.lower() == ".json":
            geoJson = json.load(geoFile)
        elif extension.lower() == ".csv":
            geoJson = DataFileParser().csvToGeoJson(geoFile)

    padding = int(args.padding) / 100
    connect = args.connect
    lType = args.ltype

    thisParser = DataFileParser()
    thisInstanceJson = thisParser.convertTreeAndGeoToInstance(
        phyloTreeJson, geoJson, padding, connect
    )

    thisOutputJSONString = json.dumps(thisInstanceJson)

    outputStream.write(thisOutputJSONString)

    print("Done.")
