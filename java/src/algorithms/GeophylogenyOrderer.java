package algorithms;

import model.Tree;
import model.Geophylogeny;

/**
 * This abstract class describes an abstract algorithm to compute a leaf order
 * for a given geophylogeny.
 *
 * @author Jonathan Klawitter
 */
public abstract class GeophylogenyOrderer {

	protected Geophylogeny geophylogeny;
	protected Tree tree;
	protected int numTaxa;
	protected int numVertices;

	public GeophylogenyOrderer(Geophylogeny geophylogeny) {
		super();
		this.geophylogeny = geophylogeny;
		this.tree = geophylogeny.getTree();
		this.numTaxa = tree.getNumberOfLeaves();
		this.numVertices = tree.getNumberOfVertices();
	}

	public abstract void orderLeaves();

}
