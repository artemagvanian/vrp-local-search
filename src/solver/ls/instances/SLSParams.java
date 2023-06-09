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
  public final double optimizationChance = 0.0;
  /**
   * Randomly optimizes the current route list with some chance.
   */
  public final double randomOptimizationChance = 0.0;
  /**
   * How many feasible/infeasible assignments we should have to start changing the EC penalty.
   */
  public final int excessCapacityPenaltyIncreaseThreshold = 10;
  /**
   * Multiplier for the EC penalty coefficient.
   */
  public final double excessCapacityPenaltyMultiplier = 2;
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
  public final int customerUsePenaltyIncreaseThreshold = 5;
  /**
   * Multiplier for the CU penalty coefficient.
   */
  public final double customerUsePenaltyMultiplier = 1.01;
  /**
   * Minimum CU penalty.
   */
  public final double customerUseMinPenalty = 0.1;
  /**
   * Maximum CU penalty.
   */
  public final double customerUseMaxPenalty = 1;
  /**
   * Base CU penalty.
   */
  public final double customerUseBasePenalty = 0.5;
  /**
   * How many iterations of steady incumbent has to pass before we start changing the neighborhood
   * size.
   */
  public final int largeNeighborhoodSizeIncreaseThreshold = 20;
  /**
   * By how much to increase the neighborhood size once the incumbent does not change.
   */
  public final double largeNeighborhoodSizeMultiplier = 1.01;
  /**
   * Minimum number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodMinSize = 500;
  /**
   * Maximum number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodMaxSize = 2500;
  /**
   * Base number of tries for 2-interchanges.
   */
  public final int largeNeighborhoodBaseSize = 500;
  /**
   * Number of stale incumbent iterations before the restart.
   */
  public final int baseRestartThreshold = 128;
  /**
   * Restart threshold multiplier.
   */
  public final double restartThresholdMultiplier = 1.25;
  /**
   * Random move minimum chance.
   */
  public final double randomMoveMin = 0.0001;
  /**
   * Random move maximum chance.
   */
  public final double randomMoveMax = 0.75;
  /**
   * Random move chance multiplier.
   */
  public final double randomMoveMultiplier = 1.01;

  public SLSParams() {
  }
}
