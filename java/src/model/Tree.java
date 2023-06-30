package model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Tree {

	private final int stateNumber;
	private Vertex root;
	private Vertex[] vertices; // indexed by id-1
	
	private double maxDepth = -1;

	private int numberOfVertices;
	private int numberOfLeaves;
	private String name;

	public Tree(Vertex root, int numberOfLeaves, int stateNumber) {
		this.root = root;
		this.setNumberOfLeaves(numberOfLeaves);
		this.stateNumber = stateNumber;

		this.vertices = new Vertex[this.numberOfVertices];
		Queue<Vertex> queue = new LinkedList<Vertex>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Vertex current = queue.poll();
			this.vertices[current.getID() - 1] = current;
			if (!current.isLeaf()) {
				queue.add(current.getFirstChild());
				queue.add(current.getSecondChild());
			}
		}

		initDiscreteDepths(this.root, 0);

	}

	public Tree(Vertex root, int numberOfLeaves) {
		this(root, numberOfLeaves, -1);
	}

	private void setNumberOfLeaves(int numberOfLeaves) {
		this.numberOfLeaves = numberOfLeaves;
		this.numberOfVertices = 2 * numberOfLeaves - 1;
	}

	public int getNumberOfLeaves() {
		return this.numberOfLeaves;
	}

	public int getNumberOfVertices() {
		return this.numberOfVertices;
	}

	public Vertex[] getVertices() {
		return vertices;
	}

	public Vertex[] getInnnerVertices() {
		return Arrays.copyOfRange(vertices, numberOfLeaves, numberOfVertices);
	}

	/**
	 * Return an array containing the leaves of this tree, where the leaves are
	 * ordered based on their indices (and ids).
	 * 
	 * @return an array containing the leaves of this tree, where the leaves are
	 *         ordered based on their indices (and ids).
	 */
	public Vertex[] getLeavesInIndexOrder() {
		return Arrays.copyOfRange(vertices, 0, numberOfLeaves);
	}

	/**
	 * Return an array containing the leaves of this tree, where the leaves are
	 * ordered based on the current implicit embedding of this tree.
	 * 
	 * @return an array containing the leaves of this tree, where the leaves are
	 *         ordered based on the current implicit embedding of this tree
	 */
	public Vertex[] getLeavesInTreeOrder() {
		Vertex[] leaves = new Vertex[this.numberOfLeaves];
		addLeavesInOrder(this.getRoot(), leaves, 0);

		return leaves;
	}

	private void addLeavesInOrder(Vertex vertex, Vertex[] leaves, int position) {
		if (vertex.isLeaf()) {
			leaves[position] = vertex;
		} else {
			addLeavesInOrder(vertex.getLeftChild(), leaves, position);
			addLeavesInOrder(vertex.getRightChild(), leaves,
					position + vertex.getLeftChild().getCladeSize());
		}
	}

	public Vertex getRoot() {
		return root;
	}

	public int getMaxDiscreteDepth() {
		int discreteDepth = 0;
		for (Vertex vertex : vertices) {
			discreteDepth = Math.max(discreteDepth, vertex.getDiscreteDepth());
		}
		return discreteDepth;
	}

	private void initDiscreteDepths(Vertex vertex, int discreteDepth) {
		vertex.setDiscreteDepth(discreteDepth);
		if (!vertex.isLeaf()) {
			initDiscreteDepths(vertex.getFirstChild(), discreteDepth + 1);
			initDiscreteDepths(vertex.getSecondChild(), discreteDepth + 1);
		}
	}

	/**
	 * Initialize the depths of all vertices vertex. Unlike for heights, does
	 * not assume that is a cladogram, but can also be a phylogram.
	 */
	public void initDepths() {
		this.initDepths(root);
	}

	private void initDepths(Vertex vertex) {
		if (vertex.hasParent()) {
			vertex.setDepth(vertex.getParent().getDepth() + vertex.getBranchLengthIncoming());
		}
		if (!vertex.isLeaf()) {
			initDepths(vertex.getFirstChild());
			initDepths(vertex.getSecondChild());
		}
	}

	/**
	 * Returns the maximum depth of all vertices in this tree.
	 * @return the maximum depth of all vertices in this tree
	 */
	public double getMaxDepth() {
		if (maxDepth > 0) {
			return maxDepth;
		}
		
		double runningDepth = 0;
		for (Vertex vertex : vertices) {
			runningDepth = Math.max(runningDepth, vertex.getDepth());
		}
		
		maxDepth = runningDepth;
		
		return maxDepth;
	}

	public double getHeight() {
		return this.root.getHeight();
	}

	public int[] getPositionsByIndex() {
		int[] positionsByIndex = new int[this.numberOfLeaves];
		getPositionByIndexForSubtree(getRoot(), positionsByIndex, 0);

		return positionsByIndex;
	}

	private void getPositionByIndexForSubtree(Vertex vertex, int[] positionsByIndex, int position) {
		if (!vertex.isLeaf()) {
			getPositionByIndexForSubtree(vertex.getLeftChild(), positionsByIndex, position);
			getPositionByIndexForSubtree(vertex.getRightChild(), positionsByIndex,
					position + vertex.getLeftChild().getCladeSize());
		} else {
			positionsByIndex[vertex.getIndex()] = position;
		}
	}

	public int getStateNumber() {
		return stateNumber;
	}

	public void setName(String treeName) {
		this.name = treeName;
	}

	public String getName() {
		return this.name;
	}

	public Vertex getLCA(Vertex first, Vertex second) {
		while (first != second) {
			if (first.getDiscreteDepth() > second.getDiscreteDepth()) {
				first = first.getParent();
			} else {
				second = second.getParent();
			}
		}

		return first;
	}

	public void resetCoordinates() {
		resetHelper(this.root);
	}

	private void resetHelper(Vertex vertex) {
		vertex.setUnfixed();
		vertex.setX(-1);
		vertex.setY(-1);
		if (!vertex.isLeaf()) {
			resetHelper(vertex.getFirstChild());
			resetHelper(vertex.getSecondChild());
		}
	}

	public void randomizeEmbedding(Random random) {
		randomizeEmbeddingHelper(this.root, random);
	}

	private void randomizeEmbeddingHelper(Vertex vertex, Random random) {
		if (random.nextBoolean()) {
			vertex.rotate();
		}
		if (!vertex.isLeaf()) {
			randomizeEmbeddingHelper(vertex.getFirstChild(), random);
			randomizeEmbeddingHelper(vertex.getSecondChild(), random);
		}
	}

	public void fixVerticalOverlaps(double threshold) {
		for (int i = 0; i < vertices.length - 1; i++) {
			Vertex u = vertices[i];

			for (int j = i + 1; j < vertices.length; j++) {
				Vertex v = vertices[j];

				if (Math.abs(u.getX() - v.getX()) < threshold) {
					// horizontal overlap
					Vertex lower, higher;
					if (u.getHeight() < v.getHeight()) {
						lower = u;
						higher = v;
					} else {
						lower = v;
						higher = u;
					}

					if (higher.getHeight() < lower.getParent().getHeight()) {
						// vertical overlap
						if (higher.getParent().getHeight() < lower.getParent().getHeight()) {
							// lower vertical nests higher vertical
							// end of higher determines direction of move
							if (higher.getX() < higher.getParent().getX()) {
								moveLeftRight(lower, higher, threshold);
							} else {
								moveLeftRight(higher, lower, threshold);
							}
						} else {
							// lower vertical and higher vertical overlap
							// end of lower determines direction of move
							if (lower.getX() < lower.getParent().getX()) {
								moveLeftRight(higher, lower, threshold);
							} else {
								moveLeftRight(lower, higher, threshold);
							}
						}
					}
				}
			}
		}
	}

	private void moveLeftRight(Vertex left, Vertex right, double d) {
		if (left.isLeaf()) {
			right.setX(right.getX() + 2 * d);
		} else if (right.isLeaf()) {
			left.setX(left.getX() - 2 * d);
		} else {
			left.setX(left.getX() - d);
			right.setX(right.getX() + d);
		}
	}

	@Override
	public String toString() {
		String s = "Tree" + ((name != null) ? " " + name : "")
				+ ((stateNumber >= 0) ? " #" + stateNumber + ":\n" : ":\n");
		s += " number of vertices: " + numberOfVertices + "\n";
		s += " number of leaves: " + numberOfLeaves + "\n";
		s += " root: " + root.toString();
		return s;
	}
}
