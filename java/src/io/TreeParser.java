package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Tree;
import model.Vertex;

public class TreeParser {

	/**
	 * Running position indicating the position until where the Newick tree
	 * string has been parsed.
	 */
	private int parsingPosition = 0;
	private int vertexIDCounter = 0;
	public String[] taxaNames;
	public boolean parsePopSize = false;

	private static final Pattern NEWICK_VERTEX = Pattern
			.compile("(?<name>[\\d]*)" + "(\\[\\&(?<attributes>[a-zA-Z0-9\\p{Punct}^:]*?)\\])?"
					+ "(:(?<length>[\\d]*.[\\d]*(E\\-[\\d]+)?))?");
	private Matcher vertexMatcher;

	private static final Pattern NEWICK_POP_ATTRIBUTE = Pattern
			.compile("pop=" + "(?<popAttribute>[\\d]*\\.[\\d]*(E[\\-\\+][\\d]+)?)");
	private Matcher popAttributeMatcher;

	public ArrayList<Tree> readTrees(File treeFile, String treeName) throws IOException {
		FileReader fr = new FileReader(treeFile);
		BufferedReader br = new BufferedReader(fr);

		ArrayList<Tree> trees = new ArrayList<Tree>();
		int numberOfLeaves;

		String line = null;
		br.readLine(); // "#NEXUS"
		br.readLine(); // "/n"
		br.readLine(); // "Begin taxa;"

		System.out.println("parsing trees: ");

		// extract number of leaves
		line = br.readLine(); // "Dimensions ntax=<numberOfLeaves>;"
		line = line.replaceAll("[^0-9]", "");
		numberOfLeaves = Integer.parseInt(line);
		System.out.println(" - number of leaves: " + numberOfLeaves);

		do {
			line = br.readLine().trim();
		} while (!(line.equalsIgnoreCase("Translate")));

		// extract names of taxa
		taxaNames = new String[numberOfLeaves];
		line = br.readLine().trim();
		while (!(line.substring(0, 1).equals(";"))) {
			String token[] = line.split(" ");
			String name = token[1];
			if (name.charAt(name.length() - 1) == ',') {
				name = name.substring(0, token[1].length() - 1);
			}
			taxaNames[Integer.parseInt(token[0]) - 1] = name;
			line = br.readLine().trim();
		}
		System.out.println(" - taxa names parsed");

		line = br.readLine();
		String token[] = line.trim().split(" ");
		while (token[0].equals("tree")) {
			// System.out.println(" - parsing a tree");
			Tree tree = parseTree(token, numberOfLeaves);
			tree.setName(treeName);
			trees.add(tree);
			line = br.readLine();
			if (line == null)
				break;
			token = line.trim().split(" ");
		}

		System.out.println(" - number of trees: " + trees.size());

		br.close();
		return trees;
	}

	private Tree parseTree(String[] token, int numberOfLeaves) {
		int statusNumber = Integer.parseInt(token[1].replaceAll("[^0-9]", ""));
		// System.out.println(" -- parsed status number: " + statusNumber);

		// System.out.println(" -- newick string of tree: " + token[3]);
		String newickTree = token[3];
		vertexMatcher = NEWICK_VERTEX.matcher(newickTree);
		popAttributeMatcher = NEWICK_POP_ATTRIBUTE.matcher(newickTree);

		parsingPosition = 0;
		vertexIDCounter = numberOfLeaves + 1;

		Vertex root = parseVertex(newickTree);

		Tree tree = new Tree(root, numberOfLeaves, statusNumber);

		return tree;
	}

	private Vertex parseVertex(String newickTree) {
		Vertex vertex;
		boolean isLeaf = true;
		Vertex firstChild = null;
		Vertex secondChild = null;
		int id;

		// check if inner vertex, if so progress down
		if (newickTree.charAt(parsingPosition) == '(') {
			isLeaf = false;

			// first child
			parsingPosition++; // skip "("
			firstChild = parseVertex(newickTree);

			// second child
			if (newickTree.charAt(parsingPosition) != ',') {
				System.out.println("ended up at wrong position in string, "
						+ "not between children: ... " + newickTree.substring(parsingPosition));
			}
			parsingPosition++; // skip ","
			secondChild = parseVertex(newickTree);

			// own values
			if (newickTree.charAt(parsingPosition) != ')') {
				System.out.println("ended up at wrong position in string, "
						+ "not end of children: ... " + newickTree.substring(parsingPosition));
			}
			parsingPosition++; // skip ")"
		}

		// now handle this vertex
		vertexMatcher.find(parsingPosition);

		// extract ID if exists
		String stringID = vertexMatcher.group("name");
		if (stringID.equals("")) {
			id = vertexIDCounter++;
		} else {
			id = Integer.parseInt(stringID);
		}

		// create vertex
		if (!isLeaf) {
			vertex = new Vertex(id, firstChild, secondChild);
		} else {
			vertex = new Vertex(id);
			vertex.setTaxonName(taxaNames[id - 1]);
		}

		// extract length of incoming edge (if exists)
		String length = vertexMatcher.group("length");
		if ((length != null) && !length.equals("")) {
			vertex.setBranchLengthIncoming(Double.valueOf(length));
		} else {
			vertex.setBranchLengthIncoming(firstChild.getBranchLengthIncoming());
		}

		// extract pop size (if requested (for species tree))
		if (parsePopSize) {
			popAttributeMatcher.find(parsingPosition);
			String popAttribute = popAttributeMatcher.group("popAttribute");
			if (!popAttribute.equals("")) {
				vertex.setPopulationSize(Double.valueOf(popAttribute));
			}
			// System.out.println(vertex.getPopulationSize());
		}

		// set position in string for next vertex
		parsingPosition = vertexMatcher.end();

		return vertex;
	}

}
