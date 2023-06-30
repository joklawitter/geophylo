package io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.json.*;

import model.Tree;
import model.Vertex;
import model.Geophylogeny;
import model.Site;

/**
 * This class provides methods to read and write geophylogenies.
 *
 * @author Jonathan Klawitter
 */
public class GeophylogenyIO {

	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";
	public static final String MAP_WIDTH = "map_width";
	public static final String MAP_HEIGHT = "map_height";
	public static final String NUM_CLUSTERS = "num_clusters";

	public static final String TREE = "tree";
	public static final String NUM_LEAVES = "num_leaves";
	public static final String ID = "id";
	public static final String LEFT_CHILD = "left";
	public static final String RIGHT_CHILD = "right";
	public static final String IS_LEAF = "leaf";
	public static final String LABEL = "label";
	public static final String SITE_ID = "site_id";

	public static final String SITES = "sites";
	public static final String NUM_SITES = "num_sites";
	public static final String X = "x";
	public static final String Y = "y";
	public static final String CLUSTER = "cluster";

	/**
	 * Writes a given geophylogeny to a json file at the given filepath.
	 * 
	 * @param geophylogeny
	 *            geophylogeny to be stored as json
	 * @param filepath
	 *            complete path "../name.json" for the output file
	 */
	public static void writeGeophylogenyToJSON(Geophylogeny geophylogeny, String filepath) {
		JsonObject jsonTree = writeJSONVertex(geophylogeny.getTree().getRoot());
		JsonArray jsonSites = writeJSONSites(geophylogeny.getSites());
		JsonObject jsonGeophylogeny = Json.createObjectBuilder()//
				.add(TITLE, geophylogeny.getName())//
				// .add(DESCRIPTION, geophylogeny.getDescription)
				.add(MAP_WIDTH, geophylogeny.getMapWidth())//
				.add(MAP_HEIGHT, geophylogeny.getMapHeight())//
				.add(NUM_LEAVES, geophylogeny.getTree().getNumberOfLeaves())//
				.add(TREE, jsonTree)//
				.add(NUM_SITES, geophylogeny.getSites().length)//
				.add(SITES, jsonSites)//
				.add(NUM_CLUSTERS, geophylogeny.getNumberOfClusters())//
				.build();
		JsonWriter writer;
		try {
			writer = Json.createWriter(new FileOutputStream(filepath));
			writer.writeObject(jsonGeophylogeny);
			writer.close();
		} catch (FileNotFoundException e) {
			System.err.println("Error trying to output geophylogeny as json to " + filepath);
		}
	}

	private static JsonObject writeJSONVertex(Vertex vertex) {
		JsonObject jsonVertex = null;
		if (vertex.isLeaf()) {
			jsonVertex = Json.createObjectBuilder()//
					.add(IS_LEAF, true)//
					.add(ID, vertex.getID() - 1)//
					.add(LABEL, (vertex.getID() - 1) + "")//
					.add(SITE_ID, vertex.getID() - 1)//
					.build();
		} else {
			JsonObject jsonLeftChild = writeJSONVertex(vertex.getLeftChild());
			JsonObject jsonRightChild = writeJSONVertex(vertex.getRightChild());
			jsonVertex = Json.createObjectBuilder()//
					.add(IS_LEAF, false)//
					.add(ID, vertex.getID() - 1)//
					.add(LEFT_CHILD, jsonLeftChild)//
					.add(RIGHT_CHILD, jsonRightChild)//
					.build();
		}
		return jsonVertex;
	}

	private static JsonArray writeJSONSites(Site[] sites) {
		JsonArrayBuilder jsonSitesBuilder = Json.createArrayBuilder();
		for (int i = 0; i < sites.length; i++) {
			JsonObject jsonSite = Json.createObjectBuilder()//
					.add(X, sites[i].getX()).add(Y, sites[i].getY())//
					.add(CLUSTER, sites[i].getCluster())//
					.build();
			jsonSitesBuilder.add(jsonSite);
		}
		return jsonSitesBuilder.build();
	}

	/**
	 * Read a geophylogeny from the given file.
	 * 
	 * @param filepath
	 *            complete path to file
	 * @return the read geophylogeny
	 */
	public static Geophylogeny readGeophylogenyFromJSON(String filepath) {
		JsonObject jsonGeophylogeny = null;
		try {
			JsonReader reader = Json.createReader(new FileInputStream(filepath));
			jsonGeophylogeny = reader.readObject();
			reader.close();
		} catch (FileNotFoundException e) {
			System.err.println(
					"Couldn't read JSON file to extract geophylogeny, because file not found: "
							+ filepath);
		}

		String title = jsonGeophylogeny.getString(TITLE);
		// String description = jsonGeophylogeny.getString(DESCRIPTION);
		int numLeaves = jsonGeophylogeny.getInt(NUM_LEAVES);
		int mapWidth = jsonGeophylogeny.getInt(MAP_WIDTH);
		int mapHeight = jsonGeophylogeny.getInt(MAP_HEIGHT);

		JsonObject jsonTree = jsonGeophylogeny.getJsonObject(TREE);
		int[] siteOfLeaf = new int[numLeaves];
		Vertex root = parseJsonVertex(jsonTree, siteOfLeaf);
		Tree tree = new Tree(root, numLeaves);

		JsonArray jsonSites = jsonGeophylogeny.getJsonArray(SITES);
		int numSites = jsonGeophylogeny.getInt(NUM_SITES);
		Site[] sites = new Site[numSites];
		for (int i = 0; i < numSites; i++) {
			JsonObject jsonSite = jsonSites.getJsonObject(i);
			double x = jsonSite.getJsonNumber(X).doubleValue();
			double y = jsonSite.getJsonNumber(Y).doubleValue();
			int cluster = jsonSite.getInt(CLUSTER);
			sites[i] = new Site(x, y);
			sites[i].setCluster(cluster);
		}

		Vertex[] leaves = tree.getLeavesInIndexOrder();
		for (int i = 0; i < leaves.length; i++) {
			sites[siteOfLeaf[i]].setLeaf(leaves[i]);
		}

		Geophylogeny geophylogeny = new Geophylogeny(tree, sites, mapWidth, mapHeight, title);

		if (jsonGeophylogeny.getInt(NUM_CLUSTERS) > 1) {
			int[] clusterOfLeaf = new int[numLeaves];
			for (int i = 0; i < leaves.length; i++) {
				clusterOfLeaf[i] = sites[siteOfLeaf[i]].getCluster();
			}
			geophylogeny.setClustersByMapping(clusterOfLeaf);
		}

		return geophylogeny;
	}

	private static Vertex parseJsonVertex(JsonObject jsonVertex, int[] siteOfLeaf) {
		int id = jsonVertex.getInt(ID) + 1;
		if (jsonVertex.getBoolean(IS_LEAF)) {
			siteOfLeaf[id - 1] = jsonVertex.getInt(SITE_ID);
			return new Vertex(id);
		} else {
			Vertex leftChild = parseJsonVertex(jsonVertex.getJsonObject(LEFT_CHILD), siteOfLeaf);
			Vertex rightChild = parseJsonVertex(jsonVertex.getJsonObject(RIGHT_CHILD), siteOfLeaf);
			return new Vertex(id, leftChild, rightChild);
		}
	}
}
