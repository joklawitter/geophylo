package model;

public class Vertex {

	/** index used in model to manage vertices of a graph */
	private int index;

	/**
	 * ID may be obtained from input file or some other unique value, often
	 * index+1.
	 */
	private int ID;

	/** Discrete distance from root (0), i.e. number of edges on path from root. */
	private int discreteDepth;
	
	/** Distance from root (0), i.e. total length of edges on path from root. */
	private double depth = 0;

	/** The number of leaves in the subtree rooted at this vertex. */
	private int cladeSize;

	/** The number of vertices in the subtree rooted at this vertex. */
	private int subtreeSize;

	/**
	 * Whether (inner) vertex is allowed to rotate (or other internal stuff).
	 */
	private boolean fixed = false;

	private boolean isLeaf;
	private double height;
	private double branchLengthIncoming = -1;
	private double populationSize;

	// if it is an inner vertex
	private Vertex parent;
	private Vertex firstChild;
	private Vertex secondChild;
	private boolean firstChildIsLeftChild = true;

	private Vertex[] clade;

	// if it is a leaf
	private String taxonName = null;

	private double x;
	private double y;

	public Vertex(int ID) {
		this.index = ID - 1;
		this.ID = ID;
		this.isLeaf = true;

		this.cladeSize = 1;
		this.subtreeSize = 1;

		initHeight();
	}

	public Vertex(int ID, Vertex firstChild, Vertex secondChild) {
		this(ID);
		this.firstChild = firstChild;
		this.firstChild.setParent(this);
		this.secondChild = secondChild;
		this.secondChild.setParent(this);
		this.isLeaf = false;
		this.initHeight();

		this.cladeSize = this.firstChild.getCladeSize() + this.secondChild.getCladeSize();
		this.subtreeSize = this.firstChild.getSubtreeSize() + this.secondChild.getSubtreeSize() + 1;
	}

	public int getIndex() {
		return this.index;
	}

	public int getID() {
		return this.ID;
	}
	
	public void resetID(int newID) {
		this.ID = newID;
		this.index = ID - 1;
	}

	public Vertex getLeftChild() {
		return firstChildIsLeftChild ? getFirstChild() : getSecondChild();
	}

	public Vertex getRightChild() {
		return firstChildIsLeftChild ? getSecondChild() : getFirstChild();
	}

	public void setAsLeftChild(Vertex toBeLeftVertex) {
		if ((toBeLeftVertex != firstChild) && (toBeLeftVertex != secondChild)) {
			System.err.println("Asked to set vertex (" + toBeLeftVertex.getIndex()
					+ ") as left child, but is not actually child of this vertex ("
					+ this.getIndex() + ").");
			System.out.println("- this vertex: " + this);
			System.out.println("- to set vertex: " + toBeLeftVertex);
			throw new AssertionError();
		}

		if (!this.isFixed()) {
			if (toBeLeftVertex != firstChild) {
				this.firstChildIsLeftChild = false;
			} else {
				this.firstChildIsLeftChild = true;
			}
		} else {
			System.out.println("Request to set left child of fixed vertex! - " + this.toString());
		}
	}

	public void rotate() {
		this.firstChildIsLeftChild = !this.firstChildIsLeftChild;
	}

	public void rotateDeep() {
		this.rotate();
		if (!this.isLeaf()) {
			this.getFirstChild().rotateDeep();
			this.getSecondChild().rotateDeep();
		}
	}

	public Vertex getFirstChild() {
		return firstChild;
	}

	public void setFirstChild(Vertex child) {
		this.firstChild = child;
	}

	public Vertex getSecondChild() {
		return secondChild;
	}

	public void setSecondChild(Vertex firstChild) {
		this.secondChild = firstChild;
	}

	public double getBranchLengthIncoming() {
		return branchLengthIncoming;
	}

	public void setBranchLengthIncoming(double branchLengthIncoming) {
		this.branchLengthIncoming = branchLengthIncoming;
	}

	public double getPopulationSize() {
		return populationSize;
	}

	public void setPopulationSize(double populationSize) {
		this.populationSize = populationSize;
	}

	public String getTaxonName() {
		return this.taxonName;
	}

	public void setTaxonName(String taxonName) {
		this.taxonName = taxonName;
	}

	public boolean isLeaf() {
		return this.isLeaf;
	}

	/**
	 * Initialize the heights of this vertex. Assumes that tree is a cladogram,
	 * that is, all leaves have the same heights and that branch lengths are
	 * consistent.
	 */
	public void initHeight() {
		if (this.isLeaf()) {
			height = 0;
		} else if (this.getLeftChild().getBranchLengthIncoming() > 0) {
			height = this.getLeftChild().getHeight()
					+ this.getLeftChild().getBranchLengthIncoming();
		} else {
			height = Math.max(this.getLeftChild().getHeight(), this.getRightChild().getHeight())
					+ 1;
		}
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public int getDiscreteDepth() {
		return discreteDepth;
	}

	public void setDiscreteDepth(int discreteDepth) {
		this.discreteDepth = discreteDepth;
	}
	
	public double getDepth() {
		return depth;
	}
	
	public void setDepth(double depth) {
		this.depth = depth;
	}

	public boolean hasParent() {
		return (this.parent != null);
	}

	public Vertex getParent() {
		return parent;
	}

	public void setParent(Vertex parent) {
		this.parent = parent;
	}

	public void removeChild(Vertex childToRemove) {
		// note that method leaves vertex properties unclean
		if (this.firstChild == childToRemove) {
			this.firstChild = null;
		} else if (this.secondChild == childToRemove) {
			this.secondChild = null;
		}
	}

	public void replaceChild(Vertex childToReplace, Vertex newChild) {
		// note that method leaves vertex properties unclean
		if (this.firstChild == childToReplace) {
			this.firstChild = newChild;
		} else if (this.secondChild == childToReplace) {
			this.secondChild = newChild;
		}
	}

	@Override
	public String toString() {
		String string;
		if (this.isLeaf) {
			string = "l" + this.ID;
			string += " (len: " + this.branchLengthIncoming + ")";
		} else {
			string = "v" + this.ID;
			string += " (len: " + this.branchLengthIncoming + ", pop: " + this.populationSize;
			string += ", firstChild: " + this.firstChild.getID() + ", secondChild: "
					+ this.secondChild.getID() + ")";
		}
		return string;
	}

	public Vertex copy() {
		Vertex copy = new Vertex(this.getID());
		copy.isLeaf = this.isLeaf;
		copy.taxonName = this.taxonName;
		copy.branchLengthIncoming = this.branchLengthIncoming;

		return copy;
	}

	public int getCladeSize() {
		return cladeSize;
	}

	public Vertex[] getClade() {
		if (this.clade != null) {
			return clade;
		} else {
			this.clade = new Vertex[this.getCladeSize()];
			if (this.isLeaf()) {
				this.clade[0] = this;
			} else {
				int i = 0;
				for (Vertex leaf : this.getLeftChild().getClade()) {
					clade[i++] = leaf;
				}
				for (Vertex leaf : this.getRightChild().getClade()) {
					clade[i++] = leaf;
				}
			}

			return clade;
		}
	}

	public int getSubtreeSize() {
		return subtreeSize;
	}

	public boolean isFixed() {
		return fixed;
	}

	public void setFixed() {
		this.fixed = true;
	}

	public void setUnfixed() {
		this.fixed = false;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}
}
