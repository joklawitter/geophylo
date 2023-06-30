package model;

import model.Leader.GeophylogenyLeaderType;

/**
 * This class models a geophylogeny, which consists of a phylogenetic tree, 
 * a map, and a site (location) in the map for each leaf of the tree.
 * 
 * @author Jonathan Klawitter
 */
public class Geophylogeny {

	private String name;
	private GeophylogenyLeaderType leaderType = GeophylogenyLeaderType.NONE;	

	private Tree tree;
	private Site[] sites;

	private int mapWidth;
	private int mapHeight;

	/**
	 * Distance between adjacent leaves and of a left/rightmost leaf and border.
	 */
	private double leafStep;

	/** Number of clusters this geophylogeny has. */
	private int numberOfClusters = 0; 
	
	/** Mapping from vertex index to cluster number. */
	private int[] clusterOfVertex;

	public Geophylogeny(Tree tree, Site[] sites, int mapWidth, int mapHeight, String name) {
		this.tree = tree;
		this.sites = sites;
		this.mapWidth = mapWidth;
		this.leafStep = ((double) mapWidth) / (tree.getNumberOfLeaves() + 1);
		this.mapHeight = mapHeight;
		this.name = name;
	}
	
	public Geophylogeny(Tree tree, Site[] sites, int mapWidth, int mapHeight, String name, GeophylogenyLeaderType leaderType) {
		this(tree, sites, mapWidth, mapHeight, name);
		this.leaderType = leaderType;
	}

	public Tree getTree() {
		return tree;
	}

	public void setTree(Tree tree) {
		this.tree = tree;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GeophylogenyLeaderType getLeaderType() {
		return leaderType;
	}

	public void setLeaderType(GeophylogenyLeaderType leaderType) {
		this.leaderType = leaderType;
	}
	
	public Site[] getSites() {
		return sites;
	}

	public void setSites(Site[] sites) {
		this.sites = sites;
	}

	public Site getSiteOfLeaf(Vertex leaf) {
		return sites[leaf.getIndex()];
	}

	public int getMapWidth() {
		return mapWidth;
	}

	public void setMapWidth(int mapWidth) {
		this.mapWidth = mapWidth;
	}

	public int getMapHeight() {
		return mapHeight;
	}

	public void setMapHeight(int mapHeight) {
		this.mapHeight = mapHeight;
	}

	public boolean hasClusters() {
		return (numberOfClusters > 1);
	}

	public int getNumberOfClusters() {
		return numberOfClusters;
	}
	
	public int getClusterOfVertex(Vertex vertex) {
		return clusterOfVertex[vertex.getIndex()];
	}
	
	public void setClustersByMapping(int[] clustersOfVertex) {
		this.clusterOfVertex = clustersOfVertex;
		int maxClusterId = 0;
		for (int i = 0; i < clustersOfVertex.length; i++) {
			maxClusterId = Math.max(maxClusterId, clustersOfVertex[i]);
		}
		this.numberOfClusters = maxClusterId + 1;
	}

	public void setClustersByTable(Vertex[][] clusters) {
		this.numberOfClusters = clusters.length;
		this.clusterOfVertex = new int[this.tree.getNumberOfLeaves()];
		for (int i = 0; i < clusters.length; i++) {
			for (int j = 0; j < clusters[i].length; j++) {
				int vertexIndex = clusters[i][j].getIndex();
				clusterOfVertex[vertexIndex] = i;
				sites[vertexIndex].setCluster(i);
			}
		}
	}

	/**
	 * Computes and sets the x-coordinates of each vertex in the tree of this geophylogeny
	 * based on the embedding implicitly stored in the tree. 
	 */
	public void computeXCoordinates() {
		Vertex[] leaves = tree.getLeavesInTreeOrder();
		for (int i = 0; i < leaves.length; i++) {
			leaves[i].setX(getXByPosition(i));
		}
		for (Vertex vertex: tree.getInnnerVertices()) {
			vertex.setX((vertex.getLeftChild().getX() + vertex.getRightChild().getX()) / 2);
		}
	}

	public double getXByPosition(int position) {
		return (position + 1) * leafStep;
	}
	
	public int computeNumberOfCrossings() {
		Vertex[] leaves = this.tree.getLeavesInTreeOrder();
		Leader[] leader = new Leader[leaves.length];
		
		for (int i = 0; i < leader.length; i++) {
			leader[i] = new Leader(leaves[i], this.getSiteOfLeaf(leaves[i]), this.getLeaderType());
		}
		
		int numberOfCrossings = 0;
		for (int i = 0; i < leader.length; i++) {
			for (int j = i + 1; j < leader.length; j++) {
				numberOfCrossings += leader[i].crossesLeader(leader[j]) ? 1 : 0; 
			}
		}
		
		return numberOfCrossings;
	}

	public void scale(double scalor) {
		mapWidth = (int) (mapWidth * scalor);
		mapHeight = (int) (mapHeight * scalor);
		leafStep = ((double) mapWidth) / (tree.getNumberOfLeaves() + 1);
		
		for (Site site : sites) {
			site.x = site.x * scalor;
			site.y = site.y * scalor;
		}
	}
}
