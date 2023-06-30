package model;

import java.awt.geom.*;

/**
 * This class represents a leader in a drawing of a geophylogeny with external labeling.
 * Each leader has a leader type; so far s- and po-leader supported.
 *
 * @author Jonathan Klawitter
 */
public class Leader {

	private Vertex leaf;

	private Site site;

	private GeophylogenyLeaderType type;

	public Leader(Vertex leaf, Site site, GeophylogenyLeaderType type) {
		this.leaf = leaf;
		this.site = site;
		this.type = type;
	}

	/**
	 * Returns whether this leader crosses with the given leader
	 * 
	 * @param otherLeader
	 *            other leader to test if it crosses with this leader; assumed
	 *            to have same leader type
	 * @return whether this leader crosses with the given leader
	 */
	public boolean crossesLeader(Leader otherLeader) {
		// we assume that otherLeader has the same type

		if (type == GeophylogenyLeaderType.S) {
			Line2D segmentThis = new Line2D.Double(this.leaf.getX(), 0, this.site.x,
					this.site.getY());
			Line2D segmentOther = new Line2D.Double(otherLeader.leaf.getX(), 0, otherLeader.site.x,
					otherLeader.site.getY());

			return segmentThis.intersectsLine(segmentOther);
		} else if (type == GeophylogenyLeaderType.PO) {
			Line2D horizontalThis = new Line2D.Double(this.leaf.getX(), this.site.getY(),
					this.site.x, this.site.getY());
			Line2D verticalOther = new Line2D.Double(otherLeader.leaf.getX(), 0,
					otherLeader.leaf.getX(), otherLeader.site.getY());

			Line2D horizontalOther = new Line2D.Double(otherLeader.leaf.getX(),
					otherLeader.site.getY(), otherLeader.site.x, otherLeader.site.getY());
			Line2D verticalThis = new Line2D.Double(this.leaf.getX(), 0, this.leaf.getX(),
					this.site.getY());

			return horizontalThis.intersectsLine(verticalOther)
					|| verticalThis.intersectsLine(horizontalOther);
		} else {
			System.out.println("Unsupported/'NONE' leader type specified.");
			return false;
		}
	}

	public Vertex getLeaf() {
		return leaf;
	}

	public Site getSite() {
		return site;
	}

	public GeophylogenyLeaderType getType() {
		return type;
	}

	public enum GeophylogenyLeaderType {
		PO, S, NONE;
	}
}
