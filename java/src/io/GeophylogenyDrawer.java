package io;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.SVGUtil;
import model.Tree;
import model.Vertex;
import model.Geophylogeny;
import model.Leader;
import model.Site;

/**
 * This class provides a method to draw a given geophylogeny with a specified
 * (or no) leader type into an svg.
 *
 * @author Jonathan Klawitter
 */
public class GeophylogenyDrawer {

	// configuration
	public static final int PADDING = 5;
	public static final String RECT_STROKE_WIDTH = "2";
	public static final String RECT_STROKE_COLOR = "grey";
	public static final String VERTEX_RADIUS = "2.5";
	public static final String VERTEX_STROKE_WIDTH = "1";
	public static final String EDGE_STROKE_WIDTH = "2";
	public static final int MARKER_SIZE = 5;
	public static final String MARKER_STROKE_WIDTH = "1.5";
	public static final String[] CLUSTER_COLORS = { "#1f78b4", "#33a02c", "#e31a1c", "#ff7f00",
			"#6a3d9a", "#b15928", "#a6cee3", "#b2df8a", "#fb9a99", "#fdbf6f", "#cab2d6",
			"#ffff99" };
	public static final String LEADER_STROKE_WIDTH = "2";
	public static final String LEADER_COLOR = "#666666";
	public static final double LABEL_OFFSET_TREE = -6;
	public static final double LABEL_OFFSET_SITES = 10;
	public static final double TREE_OFFSET = -16;

	// document
	private Document doc;
	private Element svg;
	private String fileName;

	// basic model
	private Geophylogeny geophylogeny;
	private Tree tree;
	private Leader.GeophylogenyLeaderType leaderType;

	// basic measures
	private int svgWidth;
	private int svgHeight;
	private int mapWidth;
	private int mapHeight;

	private int treeCanvasHeight;
	private double yStepTree;

	private double xZero;
	private double yZero;

	public GeophylogenyDrawer(Geophylogeny geophylogeny, String filename) {
		this.geophylogeny = geophylogeny;
		this.tree = geophylogeny.getTree();
		this.mapWidth = geophylogeny.getMapWidth();
		this.mapHeight = geophylogeny.getMapHeight();
		this.leaderType = this.geophylogeny.getLeaderType();

		// this.treeCanvasHeight = (int) (mapHeight * 0.5);
		// this.yStepTree = (treeCanvasHeight - PADDING) / tree.getHeight();
		this.yStepTree = 16;
		this.treeCanvasHeight = PADDING + (int) (tree.getHeight() * yStepTree) + 20;

		this.svgWidth = mapWidth + 2 * PADDING;
		this.xZero = PADDING;
		this.svgHeight = mapHeight + treeCanvasHeight;
		this.yZero = treeCanvasHeight;

		this.fileName = filename;
		try {
			this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			this.svg = SVGUtil.setupSVG(doc, svgWidth, svgHeight);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public GeophylogenyDrawer setLeaderType(Leader.GeophylogenyLeaderType leaderType) {
		this.leaderType = leaderType;
		return this;
	}

	public void drawGeophylogeny() {
//		System.out.println("> draw geophylogeny");

		// draw leaders or labels
		if (leaderType.equals(Leader.GeophylogenyLeaderType.NONE)) {
			drawLabels();
		} else {
			drawLabels();
			drawLeaders();
		}

		// draw map rectangle
		drawBackground();
		
		// draw sites
		Element[] svgSites = drawSites();

		// draw tree
		Element[] svgVertices = drawTree();

		// generate output
		SVGUtil.createSVGFile(doc, fileName);
	}

	private void drawBackground() {
		Element backgroundLayer = doc.createElement("g");
		backgroundLayer.setAttribute("id", "backgroundLayer");
		svg.appendChild(backgroundLayer);

		Element rect = doc.createElement("rect");
		rect.setAttribute("x", xZero + "");
		rect.setAttribute("y", yZero + "");

		rect.setAttribute("width", mapWidth + "");
		rect.setAttribute("height", mapHeight + "");
		rect.setAttribute("stroke-width", RECT_STROKE_WIDTH);
		rect.setAttribute("stroke", RECT_STROKE_COLOR);
		rect.setAttribute("fill", "none");

		backgroundLayer.appendChild(rect);
	}

	private void drawLeaders() {
		Element leaderLayer = doc.createElement("g");
		leaderLayer.setAttribute("id", "leaderLayer");
		svg.appendChild(leaderLayer);

		for (Site site : geophylogeny.getSites()) {
			double xSite = convertMapXToSVGX(site.getX());
			double ySite = convertMapYToSVGY(site.getY());

			Vertex leaf = site.getLeaf();
			double xLeaf = xZero + leaf.getX();
			double yLeaf = yZero;

			Element leader = doc.createElement("path");
			leader.setAttribute("stroke", LEADER_COLOR);
			leader.setAttribute("stroke-width", LEADER_STROKE_WIDTH);
			leader.setAttribute("fill", "none");
			leader.setAttribute("stroke-opacity", "0.5");
			leader.setAttribute("stroke-linecap", "round");
			String d = switch (this.leaderType) {
			case PO:
				yield "M" + xSite + "," + ySite + " " + xLeaf + "," + ySite + " " + xLeaf + ","
						+ yLeaf;
			case S:
				yield "M" + xSite + "," + ySite + " " + xLeaf + "," + yLeaf;
			default:
				throw new IllegalArgumentException(
						"Unexpected leader type value: " + this.leaderType);
			};
			leader.setAttribute("d", d);

			leaderLayer.appendChild(leader);
		}
	}

	private void drawLabels() {
		Element labelLayer = doc.createElement("g");
		labelLayer.setAttribute("id", "labelLayer");
		svg.appendChild(labelLayer);

		for (Site site : geophylogeny.getSites()) {

			Element labelSite = doc.createElement("text");
			double xLabel = convertMapXToSVGX(site.getX());
			double yLabel = convertMapYToSVGY(site.getY()) + LABEL_OFFSET_SITES;
			labelSite.setAttribute("x", xLabel + "");
			labelSite.setAttribute("y", yLabel + "");
			labelSite.setAttribute("text-anchor", "middle");
			labelSite.setAttribute("dominant-baseline", "middle");
			labelSite.setAttribute("style", "font-size: smaller;");
			labelSite.setTextContent(site.getID() + "");
			labelLayer.appendChild(labelSite);

			Element labelLeaf = (Element) labelSite.cloneNode(true);
			xLabel = xZero + site.getLeaf().getX();
			yLabel = getYByHeight(0) + LABEL_OFFSET_TREE;
			labelLeaf.setAttribute("x", xLabel + "");
			labelLeaf.setAttribute("y", yLabel + "");
			labelLayer.appendChild(labelLeaf);
		}
	}

	private Element[] drawSites() {
		Element siteLayer = doc.createElement("g");
		siteLayer.setAttribute("id", "siteLayer");
		svg.appendChild(siteLayer);

		Site[] sites = geophylogeny.getSites();
		Element[] svgSites = new Element[sites.length];
		for (int i = 0; i < sites.length; i++) {
			Site site = sites[i];
			double x = convertMapXToSVGX(site.getX());
			double y = convertMapYToSVGY(site.getY());
			double offset = MARKER_SIZE / 2;
			String d = "M" + (x - offset) + "," + (y - offset) + " " + (x + offset) + ","
					+ (y + offset) + " " + "M " + (x - offset) + "," + (y + offset) + " "
					+ (x + offset) + "," + (y - offset);

			Element svgSite = doc.createElement("path");
			svgSite.setAttribute("id", generateSVGIdOfSite(site));
			String strokeColor = geophylogeny.hasClusters() ? CLUSTER_COLORS[site.getCluster()]
					: "black";
			svgSite.setAttribute("stroke", strokeColor);
			svgSite.setAttribute("stroke-width", MARKER_STROKE_WIDTH);
			svgSite.setAttribute("fill", "none");
			svgSite.setAttribute("d", d);

			siteLayer.appendChild(svgSite);
			svgSites[i] = svgSite;
		}

		return svgSites;
	}

	private Element[] drawTree() {
		Element edgeLayer = doc.createElement("g");
		edgeLayer.setAttribute("id", "edgeLayer");
		svg.appendChild(edgeLayer);
		Element vertexLayer = doc.createElement("g");
		vertexLayer.setAttribute("id", "vertexLayer");
		svg.appendChild(vertexLayer);

		Element[] svgVertices = drawVertices(vertexLayer);
		drawEdges(edgeLayer);

		return svgVertices;
	}

	private Element[] drawVertices(Element vertexLayer) {
		Vertex[] vertices = tree.getVertices();
		Element[] svgVertices = new Element[vertices.length];

		for (int i = 0; i < vertices.length; i++) {
			Vertex vertex = vertices[i];
			Element svgVertex = doc.createElement("circle");
			svgVertex.setAttribute("id", generateSVGIdOfVertex(vertex));

			svgVertex.setAttribute("cx", (xZero + vertex.getX()) + "");
			double y = getYByHeight(vertex.getHeight()) + TREE_OFFSET;
			vertex.setY(y);
			svgVertex.setAttribute("cy", y + "");

			svgVertex.setAttribute("r", VERTEX_RADIUS);
			svgVertex.setAttribute("stroke", "black");
			svgVertex.setAttribute("stroke-width", VERTEX_STROKE_WIDTH);
			if (vertex.isLeaf()) {
				String fillColor = geophylogeny.hasClusters()
						? CLUSTER_COLORS[geophylogeny.getClusterOfVertex(vertex)]
						: "white";
				svgVertex.setAttribute("fill", fillColor);

			} else {
				svgVertex.setAttribute("fill", "black");
			}

			vertexLayer.appendChild(svgVertex);
			svgVertices[i] = svgVertex;
		}

		return svgVertices;
	}

	private void drawEdges(Element edgeLayer) {
		for (Vertex vertex : tree.getVertices()) {
			if (vertex.hasParent()) {
				Vertex parent = vertex.getParent();
				double x1 = xZero + vertex.getX();
				double x2 = xZero + parent.getX();
				double y1 = vertex.getY();
				double y2 = parent.getY();
				String d = "M" + x1 + "," + y1 + " " + x1 + "," + y2 + " " + x2 + "," + y2;

				Element svgEdge = doc.createElement("path");
				svgEdge.setAttribute("stroke", "black");
				svgEdge.setAttribute("stroke-width", EDGE_STROKE_WIDTH);
				svgEdge.setAttribute("fill", "none");
				svgEdge.setAttribute("d", d);
				edgeLayer.appendChild(svgEdge);
			}
		}
	}

	private double getYByHeight(double height) {
		return yZero - height * yStepTree;
	}

	private double convertMapXToSVGX(double mapX) {
		return xZero + mapX;
	}

	private double convertMapYToSVGY(double mapY) {
		return yZero + mapY;
	}

	private String generateSVGIdOfVertex(Vertex vertex) {
		return "v" + vertex.getID();
	}

	private String generateSVGIdOfSite(Site site) {
		return "s" + site.getID();
	}
}
