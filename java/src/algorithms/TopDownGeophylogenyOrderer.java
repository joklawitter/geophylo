package algorithms;

import model.Vertex;
import model.Geophylogeny;

/**
 * This class implements the top-down greedy heuristic to improve the leaf order
 * of a geophylogeny in terms of leader crossings. Going top to bottom, at each
 * vertex it picks the rotation that minimizes the number of leaders crossing
 * the vertical line between its two subtrees.
 *
 * @author Jonathan Klawitter
 */
public class TopDownGeophylogenyOrderer extends GeophylogenyOrderer {

	public TopDownGeophylogenyOrderer(Geophylogeny geophylogeny) {
		super(geophylogeny);
	}

	@Override
	public void orderLeaves() {
		orderVertex(geophylogeny.getTree().getRoot(), 0);
	}

	private void orderVertex(Vertex parent, int position) {
		if (parent.isLeaf()) {
			parent.setX(geophylogeny.getXByPosition(position));
			return;
		}
		Vertex firstChild = parent.getFirstChild();
		Vertex secondChild = parent.getSecondChild();
		int crossingsFirstLeft = computeMidLineCrossings(firstChild, secondChild, position);
		int crossingsSecondLeft = computeMidLineCrossings(secondChild, firstChild, position);

		if (crossingsFirstLeft <= crossingsSecondLeft) {
			parent.setAsLeftChild(firstChild);
			orderVertex(firstChild, position);
			orderVertex(secondChild, position + firstChild.getCladeSize());
		} else {
			parent.setAsLeftChild(secondChild);
			orderVertex(secondChild, position);
			orderVertex(firstChild, position + secondChild.getCladeSize());
		}
	}

	private int computeMidLineCrossings(Vertex leftVertex, Vertex rightVertex, int position) {
		double mid = (geophylogeny.getXByPosition(position + leftVertex.getCladeSize())
				+ geophylogeny.getXByPosition(position + leftVertex.getCladeSize() - 1)) / 2;
		int sitesOppositeMid = 0;
		for (Vertex leaf : leftVertex.getClade()) {
			sitesOppositeMid += (geophylogeny.getSiteOfLeaf(leaf).getX() > mid) ? 1 : 0;
		}
		for (Vertex leaf : rightVertex.getClade()) {
			sitesOppositeMid += (geophylogeny.getSiteOfLeaf(leaf).getX() < mid) ? 1 : 0;
		}
		return sitesOppositeMid;
	}

}
