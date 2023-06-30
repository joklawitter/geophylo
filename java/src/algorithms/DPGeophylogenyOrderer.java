package algorithms;

import java.util.ArrayList;
import java.util.Arrays;

import model.Vertex;
import model.Geophylogeny;
import model.Leader;
import model.Site;

/**
 * This class implements a dynamic programming approach to compute a leaf order
 * for a drawing of a geophylogeny. It evaluates a specified function for every
 * subtree rooted at a vertex v if it has its leftmost leaf at a position i. To
 * this end it combines the functions result for the two subtrees of v placed at
 * position i and sufficiently further to the right.
 *
 * @author Jonathan Klawitter
 */
public class DPGeophylogenyOrderer extends GeophylogenyOrderer {

	/**
	 * Storage of values computed by dynamic program. First entry species the
	 * vertex v; second entry the position of the leftmost leaf of tree rooted
	 * at v.
	 */
	private double[][] valueOfVertexAtPosition;

	/**
	 * Store for vertex v at position p whether it would have set left child as
	 * first child.
	 */
	private boolean[][] firstAsLeftOfVertexAtPosition;

	private int[] sitePositionInHorizontalOrder;

	private DPStrategy strategy;

	public DPGeophylogenyOrderer(Geophylogeny geophylogeny, DPStrategy strategy) {
		super(geophylogeny);
		valueOfVertexAtPosition = new double[this.numVertices][this.numTaxa];
		firstAsLeftOfVertexAtPosition = new boolean[this.numVertices][this.numTaxa];

		this.strategy = strategy;

		if (this.strategy == DPStrategy.Hops) {
			int n = this.geophylogeny.getTree().getNumberOfLeaves();
			sitePositionInHorizontalOrder = new int[n];

			Site[] sites = this.geophylogeny.getSites();
			Site[] siteCopies = new Site[n];
			for (int i = 0; i < siteCopies.length; i++) {
				siteCopies[i] = sites[i];
			}
			Arrays.sort(siteCopies, (a, b) -> Double.compare(a.getX(), b.getX()));

			for (int i = 0; i < siteCopies.length; i++) {
				sitePositionInHorizontalOrder[siteCopies[i].getLeaf().getIndex()] = i;
			}
		}
	}

	@Override
	public void orderLeaves() {
		// initialize leaves
		for (Vertex leaf : this.tree.getLeavesInIndexOrder()) {
			for (int position = 0; position < numTaxa; position++) {
				Site site = this.geophylogeny.getSiteOfLeaf(leaf);
				valueOfVertexAtPosition[leaf.getIndex()][position] = switch (strategy) {

				case EuclideanDistance -> {
					double positionX = this.geophylogeny.getXByPosition(position);
					double xDiff = positionX - site.getX();
					yield Math.sqrt(xDiff * xDiff + site.getY() * site.getY());
				}

				case HorizontalDistance -> Math
						.abs(this.geophylogeny.getXByPosition(position) - site.getX());

				case Hops -> Math
						.abs(position - sitePositionInHorizontalOrder[site.getLeaf().getIndex()]);

				case Crossings -> 0;

				default -> Double.MAX_VALUE;
				};
			}
		}

		// compute values for inner vertices
		for (Vertex parent : this.tree.getInnnerVertices()) {
			Vertex firstChild = parent.getFirstChild();
			Vertex secondChild = parent.getSecondChild();


			for (int position = 0; position < numTaxa; position++) {
				if (position + parent.getCladeSize() > numTaxa) {
					valueOfVertexAtPosition[parent.getIndex()][position] = Double.POSITIVE_INFINITY;
					continue;
				}

				double firstChildLeft = valueOfVertexAtPosition[firstChild.getIndex()][position]
						+ valueOfVertexAtPosition[secondChild.getIndex()][position
								+ firstChild.getCladeSize()];
				double secondChildLeft = valueOfVertexAtPosition[secondChild.getIndex()][position]
						+ valueOfVertexAtPosition[firstChild.getIndex()][position
								+ secondChild.getCladeSize()];
				
				if (strategy == DPStrategy.Crossings) {
					firstChildLeft += computeCombinationCosts(firstChild, secondChild, position);
					secondChildLeft += computeCombinationCosts(secondChild, firstChild, position);
				}

				firstAsLeftOfVertexAtPosition[parent
						.getIndex()][position] = (firstChildLeft <= secondChildLeft);
				valueOfVertexAtPosition[parent.getIndex()][position] = Math.min(firstChildLeft,
						secondChildLeft);

			}
		}

		// recover and set order according to result
		recoverOrder(this.tree.getRoot(), 0);
	}

	private double computeCombinationCosts(Vertex leftVertex, Vertex rightVertex, int position) {
		recoverOrder(leftVertex, position);
		recoverOrder(rightVertex, position + leftVertex.getCladeSize());
		
		ArrayList<Leader> leftLeaders = new ArrayList<Leader>(leftVertex.getCladeSize());
		for (Vertex leaf : leftVertex.getClade()) {
			leftLeaders.add(new Leader(leaf, geophylogeny.getSiteOfLeaf(leaf), geophylogeny.getLeaderType()));
		}
		
		ArrayList<Leader> rightLeaders = new ArrayList<Leader>(leftVertex.getCladeSize());
		for (Vertex leaf : rightVertex.getClade()) {
			rightLeaders.add(new Leader(leaf, geophylogeny.getSiteOfLeaf(leaf), geophylogeny.getLeaderType()));
		}
		
		int numCrossings = 0;
		for (Leader leftLeader : leftLeaders) {
			for (Leader rightLeader : rightLeaders) {
				numCrossings += leftLeader.crossesLeader(rightLeader) ? 1 : 0;
			}
		}
		
		return numCrossings;
	}

	private void recoverOrder(Vertex parent, int position) {
		if (parent.isLeaf()) {
			parent.setX(geophylogeny.getXByPosition(position));
			return;
		}
		
		Vertex firstChild = parent.getFirstChild();
		Vertex secondChild = parent.getSecondChild();

		if (firstAsLeftOfVertexAtPosition[parent.getIndex()][position]) {
			parent.setAsLeftChild(firstChild);
			recoverOrder(firstChild, position);
			recoverOrder(secondChild, position + firstChild.getCladeSize());
		} else {
			parent.setAsLeftChild(secondChild);
			recoverOrder(secondChild, position);
			recoverOrder(firstChild, position + secondChild.getCladeSize());
		}
	}

	public enum DPStrategy {
		EuclideanDistance, HorizontalDistance, Hops, Crossings;
	}

}
