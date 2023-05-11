package solver.ls.instances;

public class SLSParams {

  /**
   * The minimum tabu tenure multiplier.
   */
  public final double minimumTabuTenureMultiplier = 0.8;
  /**
   * The maximum tabu tenure multiplier.
   */
  public final double maximumTabuTenureMultiplier = 1.2;
  /**
   * Timeout to optimize the solution (seconds).
   */
  public final double optimizationTimeout = 1;
  /**
   * Allowed solution time (seconds).
   */
  public final double instanceTimeout = 300;
  /**
   * Increase greediness by grabbing the first solution that is better than the incumbent.
   */
  public final boolean firstBestFirst = false;
  /**
   * Randomly optimizes the incumbent with some chance.
   */
  public final double optimizationChance = 0.05;
  /**
   * Randomly optimizes the current route list with some chance.
   */
  public final double randomOptimizationChance = 0.01;
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
  public final int customerUsePenaltyIncreaseThreshold = 20;
  /**
   * Multiplier for the CU penalty coefficient.
   */
  public final double customerUsePenaltyMultiplier = 1.05;
  /**
   * Minimum CU penalty.
   */
  public final double customerUseMinPenalty = 1;
  /**
   * Maximum CU penalty.
   */
  public final double customerUseMaxPenalty = 100;
  /**
   * Base CU penalty.
   */
  public final double customerUseBasePenalty = 1;
  /**
   * How many iterations of steady incumbent has to pass before we start changing the neighborhood
   * size.
   */
  public final int largeNeighborhoodSizeIncreaseThreshold = 5;
  /**
   * By how much to increase the neighborhood size once the incumbent does not change.
   */
  public final double largeNeighborhoodSizeMultiplier = 1.05;
  /**
   * Minimum number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodMinSize = 1000;
  /**
   * Maximum number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodMaxSize = 10000;
  /**
   * Base number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodBaseSize = 1000;
  /**
   * Number of stale incumbent iterations before the restart.
   */
  public final int baseRestartThreshold = 50;

  /**
   * Restart threshold multiplier.
   */
  public final double restartThresholdMultiplier = 2;

  public SLSParams() {
  }
}
