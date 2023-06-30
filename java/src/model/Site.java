package model;

/**
 * This class represents a site of geophylogeny.
 *
 * @author Jonathan Klawitter
 */
public class Site {

	protected double x;
	protected double y;

	private Vertex leaf;

	private int cluster = 1;

	public Site(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}

	public Site(double x, double y, Vertex leaf) {
		this(x, y);
		this.leaf = leaf;
	}

	public Vertex getLeaf() {
		return leaf;
	}

	public void setLeaf(Vertex leaf) {
		this.leaf = leaf;
	}

	public int getID() {
		if (this.leaf == null) {
			return -1;
		} else {
			return leaf.getID();
		}
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public int getCluster() {
		return cluster;
	}

	public void setCluster(int cluster) {
		this.cluster = cluster;
	}

	/**
	 * Computes the Eucledian distance of this site to the given site.
	 * 
	 * @param other
	 *            site to compute the distance to
	 * @return the Eucledian distance of this site to the given site
	 */
	public double distanceTo(Site other) {
		return Math.sqrt((this.x - other.x) * (this.x - other.x)
				+ (this.getY() - other.getY()) * (this.getY() - other.getY()));
	}

}
