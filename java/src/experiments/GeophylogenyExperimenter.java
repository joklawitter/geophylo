package experiments;

import java.util.Random;

import algorithms.DPGeophylogenyOrderer;
import algorithms.GeophylogenyOrderer;
import algorithms.GreedyGeophylogenyOrderOptimizer;
import algorithms.TopDownGeophylogenyOrderer;
import algorithms.DPGeophylogenyOrderer.DPStrategy;
import model.Geophylogeny;
import model.Leader;
import model.Leader.GeophylogenyLeaderType;

/**
 * This class provides a method to test the heuristics on generated examples and
 * the real-world examples.
 * 
 * To use, set parameters and paths as wanted, comment in/out different
 * heuristics, and output. The experiment results are printed to the console.
 *
 * @author Jonathan Klawitter
 */
@SuppressWarnings("unused")
public class GeophylogenyExperimenter {

	private static int START_N = 10;
	private static int END_N = 100;
	private static int STEP_N = 10;
	private static int REPEATS = 1000;
	private static GeophylogenyLeaderType LEADER_TYPE = GeophylogenyLeaderType.S;

	private static final String FILE_PATH = "TBA";
    private static final String FILE_INPUT_PATH = "data/realWorld/";
	private static final String FILE_RW_FROGS = "rwExampleShrubFrogs.json";
	private static final String FILE_RW_FISH = "rwExampleFish.json";
	private static final String FILE_RW_LIZARD = "rwExampleGreenLizards.json";

	public static void main(String[] args) {
		String size = "size, ";
		// heuristics on their own
		String optimizerOnly = "greedyOptimizer, ";
		String topDown = "topDown, ";
		String bottomUp = "bottomUp, ";
		String euclidean = "euclidean, ";
		String horizontal = "horizontal, ";
		String hop = "hop, ";
		// heuristics + greedy optimizer (hill climbing)
		String topDownP = "topDown+, ";
		String bottomUpP = "bottomUp+, ";
		String euclideanP = "euclidean+, ";
		String horizontalP = "horizontal+, ";
		String hopP = "hop+, ";

		int numClusters = 2;
		GeophylogenyOrderer ordered;
		GeophylogenyOrderer optimizer;

		for (int i = START_N; i <= END_N; i += STEP_N) {
			Random seedGenerator = new Random(i);
			if (i % 10 == 0) {
				numClusters++;
			}

			for (int j = 0; j < REPEATS; j++) {
				size += i + ", ";

				long seed = seedGenerator.nextLong();

				String name = "coast-n" + i + "-r" + j + "-s" + seed + "-d"
						+ GeophylogenyInstanceCreater.EXPONENT;

				Geophylogeny geophylo;
				geophylo = GeophylogenyInstanceCreater.generateUniformInstance(500, 300, i,
						name + i, seed);
				geophylo.setLeaderType(LEADER_TYPE);

				/* to store generated instance, comment in the following */
				// GeophylogenyIO.writeGeophylogenyToJSON(geophylo, FILE_PATH +
				// name + ".json");

				/*
				 * to use a real-world example, use the following code instead,
				 * set the path correctly and set the numbers for the
				 * repeats/steps appropriately
				 */
				// geophylo =
				// GeophylogenyIO.readGeophylogenyFromJSON(FILE_INPUT_PATH +
				// FILE_RW_FROGS);

				optimizer = new GreedyGeophylogenyOrderOptimizer(geophylo);
				optimizer.orderLeaves();
				optimizerOnly += geophylo.computeNumberOfCrossings() + ", ";

				ordered = new TopDownGeophylogenyOrderer(geophylo);
				ordered.orderLeaves();
				topDown += geophylo.computeNumberOfCrossings() + ", ";
				optimizer.orderLeaves();
				topDownP += geophylo.computeNumberOfCrossings() + ", ";

				ordered = new DPGeophylogenyOrderer(geophylo, DPStrategy.Crossings);
				ordered.orderLeaves();
				bottomUp += geophylo.computeNumberOfCrossings() + ", ";
				optimizer.orderLeaves();
				bottomUpP += geophylo.computeNumberOfCrossings() + ", ";

				ordered = new DPGeophylogenyOrderer(geophylo, DPStrategy.EuclideanDistance);
				ordered.orderLeaves();
				euclidean += geophylo.computeNumberOfCrossings() + ", ";
				optimizer.orderLeaves();
				euclideanP += geophylo.computeNumberOfCrossings() + ", ";

				ordered = new DPGeophylogenyOrderer(geophylo, DPStrategy.HorizontalDistance);
				ordered.orderLeaves();
				horizontal += geophylo.computeNumberOfCrossings() + ", ";
				optimizer.orderLeaves();
				horizontalP += geophylo.computeNumberOfCrossings() + ", ";

				ordered = new DPGeophylogenyOrderer(geophylo, DPStrategy.Hops);
				ordered.orderLeaves();
				hop += geophylo.computeNumberOfCrossings() + ", ";
				optimizer.orderLeaves();
				hopP += geophylo.computeNumberOfCrossings() + ", ";

				/*
				 * to output drawing, comment out following and set path and
				 * name, and move directly after wanted heuristic(s)
				 */
				// geophylo.computeXCoordinates();
				// GeophylogenyDrawer drawer = new GeophylogenyDrawer(geophylo,
				// FILE_PATH + "HEURISTIC-NAME-TO-ADD" + name + ".svg");
				// drawer.drawGeophylogeny();
			}
		}

		System.out.println(size);
		System.out.println(optimizerOnly);
		System.out.println(topDown);
		System.out.println(bottomUp);
		System.out.println(euclidean);
		System.out.println(horizontal);
		System.out.println(hop);
		System.out.println(topDownP);
		System.out.println(bottomUpP);
		System.out.println(euclideanP);
		System.out.println(horizontalP);
		System.out.println(hopP);
	}
}
