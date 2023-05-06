package solver.ls.instances;

public class SLSParams {

  /**
   * The minimum value tabu tenure can take.
   */
  public final int minimumTabuTenure = 5;
  /**
   * The maximum value tabu tenure can take.
   */
  public final int maximumTabuTenure = 10;
  /**
   * Timeout to optimize the solution (seconds).
   */
  public final double optimizationTimeout = 5;
  /**
   * Allowed solution time (seconds).
   */
  public final double instanceTimeout = 300;
  /**
   * Increase greediness by grabbing the first solution that is better than the incumbent.
   */
  public final boolean firstBestFirst = false;
  /**
   * Randomly optimizes the incumbent with kOptimize chance.
   */
  public final double optimizationChance = 0.1;
  /**
   * How many feasible/infeasible assignments we should have to start changing the EC penalty.
   */
  public final int excessCapacityPenaltyIncreaseThreshold = 10;
  /**
   * Multiplier for the EC penalty coefficient.
   */
  public final double excessCapacityPenaltyMultiplier = 1.5;
  /**
   * Minimum EC penalty.
   */
  public final double excessCapacityMinPenalty = 0.000001;
  /**
   * Maximum EC penalty.
   */
  public final double excessCapacityMaxPenalty = 1000000;
  /**
   * Base EC penalty.
   */
  public final double excessCapacityBasePenalty = 1;
  /**
   * How many iterations of steady incumbent has to pass before we start changing the CU penalty.
   */
  public final int customerUsePenaltyIncreaseThreshold = 10;
  /**
   * Multiplier for the CU penalty coefficient.
   */
  public final double customerUsePenaltyMultiplier = 1.25;
  /**
   * Minimum CU penalty.
   */
  public final double customerUseMinPenalty = 10;
  /**
   * Maximum CU penalty.
   */
  public final double customerUseMaxPenalty = 10000;
  /**
   * Base CU penalty.
   */
  public final double customerUseBasePenalty = 10;
  /**
   * How many iterations of steady incumbent has to pass before we start changing the neighborhood
   * size.
   */
  public final int largeNeighborhoodSizeIncreaseThreshold = 100;
  /**
   * By how much to increase the neighborhood size once the incumbent does not change.
   */
  public final double largeNeighborhoodSizeMultiplier = 1.01;
  /**
   * Minimum number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodMinSize = 1000;
  /**
   * Maximum number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodMaxSize = 1000000;
  /**
   * Base number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodBaseSize = 100000;

  public SLSParams() {
  }
}
