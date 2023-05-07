package solver.ls.instances;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Param;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import solver.ls.data.Insertion;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.Route;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;
import solver.ls.interchanges.BestInsertionCalculator;
import solver.ls.interchanges.BestRandom2ICalculator;
import solver.ls.interchanges.BestSwapCalculator;
import solver.ls.interchanges.InterchangeCalculator;
import solver.ls.interchanges.InterchangePerRoute;
import solver.ls.utils.Timer;

public class VRPInstanceSLS extends VRPInstance {

  /**
   * Solver parameters.
   */
  private final SLSParams params;
  /**
   * Memory list to keep the recently moved customers.
   */
  private final List<TabuItem> shortTermMemory;
  /**
   * Map between the customer and the number of interchanges it participated in.
   */
  private final Map<Integer, Integer> longTermMemory;
  /**
   * Random number generator for the instance.
   */
  private final Random rand;
  /**
   * Current timer, to stop the iteration once allowed time elapses.
   */
  private final Timer watch;
  /**
   * Thread pool to perform neighborhood calculations.
   */
  private final ExecutorService executor = Executors.newFixedThreadPool(16);
  /**
   * Current best solution for the current restart, with no excess capacity.
   */
  public RouteList incumbent;
  /**
   * Current best solution, with no excess capacity.
   */
  public RouteList bestIncumbent;
  /**
   * Current solution, not necessarily optimal.
   */
  public RouteList routeList;
  /**
   * Current iteration of SLS.
   */
  public int currentIteration = 0;
  /**
   * Customer use penalty coefficient for long-term memory.
   */
  private double customerUsePenaltyCoefficient;
  /**
   * Current best objective, calculated as specified by the objective function.
   */
  private double objective;
  /**
   * Initial penalty coefficient for the objective function.
   */
  private double excessCapacityPenaltyCoefficient;
  /**
   * How many feasible/infeasible assignments we had.
   */
  private int lastFeasibleIterations = 1;
  /**
   * How many iterations has passed since the last incumbent update.
   */
  private int iterationsSinceLastIncumbent = 0;
  /**
   * Number of tries for 2-interchanges.
   */
  private int largeNeighborhoodSize;
  /**
   * Restart threshold.
   */
  private int restartThreshold;

  public VRPInstanceSLS(String fileName, Timer watch, SLSParams params) {
    super(fileName);
    // Copy parameters.
    this.params = params;
    this.watch = watch;
    // Initialize helpers.
    shortTermMemory = new ArrayList<>();
    longTermMemory = new HashMap<>();
    for (int i = 0; i < numCustomers; i++) {
      longTermMemory.put(i, 0);
    }
    rand = new Random();
    // Instantiate coefficients.
    largeNeighborhoodSize = params.largeNeighborhoodBaseSize;
    excessCapacityPenaltyCoefficient = params.excessCapacityBasePenalty;
    customerUsePenaltyCoefficient = params.customerUseBasePenalty;
    restartThreshold = params.baseRestartThreshold;
    // Generate the initial solution, initialize variables.
    routeList = generateInitialSolution();
    // Optimize routes.
    for (Route route : routeList.routes) {
      routeList.length += route.optimize(distances, params.optimizationTimeout);
    }
    incumbent = routeList.clone();
    bestIncumbent = routeList.clone();
    objective = incumbent.length;
    // Objective of the initial solution.
    System.out.println("Initial objective: " + objective);
    // Perform search for a given number of iterations.
    search();
    // Shut down executor.
    executor.shutdownNow();
  }

  /**
   * Interchanges 0 or 1 customers between routes to improve the current solution. In our case,
   * \lambda = 1, so we consider swaps and shifts: (1, 1), (1, 0).
   */
  private void search() {
    Interchange bestInsertion;
    Interchange bestSwap;
    Interchange best2Interchange;

    currentIteration = 0;

    // Keep going for a fixed number of iterations.
    while (watch.getTime() < params.instanceTimeout - 2 * params.optimizationTimeout) {
      currentIteration++;
      System.out.println("============ ITERATION #" + currentIteration + " ============");

      // Calculate both best insertion and best swap.
      bestInsertion = searchNeighborhood(
          (routeIdx1) -> new BestInsertionCalculator(routeList, incumbent,
              excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient, currentIteration,
              shortTermMemory, params.firstBestFirst, routeIdx1));
      bestSwap = searchNeighborhood(
          (routeIdx1) -> new BestSwapCalculator(routeList, incumbent,
              excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient, currentIteration,
              shortTermMemory, params.firstBestFirst, routeIdx1));
      best2Interchange = searchNeighborhood(
          (routeIdx1) -> new BestRandom2ICalculator(routeList, incumbent,
              excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient, shortTermMemory,
              params.firstBestFirst, routeIdx1, largeNeighborhoodSize, currentIteration));

      // Get insertion objectives, if possible.
      double insertionObjective =
          bestInsertion == null ? Double.POSITIVE_INFINITY
              : routeList.calculateObjective(bestInsertion, excessCapacityPenaltyCoefficient,
                  customerUsePenaltyCoefficient, currentIteration, true);
      double swapObjective =
          bestSwap == null ? Double.POSITIVE_INFINITY
              : routeList.calculateObjective(bestSwap, excessCapacityPenaltyCoefficient,
                  customerUsePenaltyCoefficient, currentIteration, true);
      double twoInterchangeObjective =
          best2Interchange == null ? Double.POSITIVE_INFINITY
              : routeList.calculateObjective(best2Interchange, excessCapacityPenaltyCoefficient,
                  customerUsePenaltyCoefficient, currentIteration, true);

      // Route indices of the interchange, so we could optimize those later.
      int routeIdx1 = 0;
      int routeIdx2 = 0;

      if (bestInsertion != null || bestSwap != null || best2Interchange != null) {
        // If current best insertion is better.
        if (insertionObjective <= swapObjective && insertionObjective <= twoInterchangeObjective) {
          assert bestInsertion != null;
          updateInterchange(bestInsertion, insertionObjective);
          routeIdx1 = bestInsertion.routeIdx1;
          routeIdx2 = bestInsertion.routeIdx2;
        } else if (swapObjective <= insertionObjective
            && swapObjective <= twoInterchangeObjective) {
          assert bestSwap != null;
          updateInterchange(bestSwap, insertionObjective);
          routeIdx1 = bestSwap.routeIdx1;
          routeIdx2 = bestSwap.routeIdx2;
        } else {
          assert best2Interchange != null;
          updateInterchange(best2Interchange, insertionObjective);
          routeIdx1 = best2Interchange.routeIdx1;
          routeIdx2 = best2Interchange.routeIdx2;
        }
      }

      // Remove all tabu items that are past the tenure.
      int finalCurrentIteration = currentIteration;
      shortTermMemory.removeIf(tabuItem -> (tabuItem.expirationIteration < finalCurrentIteration));

      // Check whether we should update the incumbent.
      if (routeList.length < incumbent.length && calculateExcessCapacity(routeList) == 0) {
        if (rand.nextDouble() < params.optimizationChance) { // kOptimize chance of optimization.
          routeList.length += routeList.routes.get(routeIdx1)
              .optimize(distances, params.optimizationTimeout);
          routeList.length += routeList.routes.get(routeIdx2)
              .optimize(distances, params.optimizationTimeout);
        }
        incumbent = routeList.clone();
        iterationsSinceLastIncumbent = 0;
        if (incumbent.length < bestIncumbent.length) {
          bestIncumbent = incumbent.clone();
        }
      } else {
        iterationsSinceLastIncumbent++;
      }

      // Increase the neighborhood size, if necessary.
      if (iterationsSinceLastIncumbent > params.largeNeighborhoodSizeIncreaseThreshold) {
        largeNeighborhoodSize = Math.min(
            (int) (largeNeighborhoodSize * params.largeNeighborhoodSizeMultiplier),
            params.largeNeighborhoodMaxSize);
      } else {
        largeNeighborhoodSize = Math.max(
            (int) (largeNeighborhoodSize / params.largeNeighborhoodSizeMultiplier),
            params.largeNeighborhoodMinSize);
      }

      // Increase the customer use penalty, if necessary.
      if (iterationsSinceLastIncumbent > params.customerUsePenaltyIncreaseThreshold) {
        customerUsePenaltyCoefficient = Math.min(
            customerUsePenaltyCoefficient * params.customerUsePenaltyMultiplier,
            params.customerUseMaxPenalty);
      } else {
        customerUsePenaltyCoefficient = Math.max(
            customerUsePenaltyCoefficient / params.customerUsePenaltyMultiplier,
            params.customerUseMinPenalty);
      }

      // Adjust the number of last feasible solutions.
      if (calculateExcessCapacity(routeList) == 0) {
        if (lastFeasibleIterations > 0) {
          lastFeasibleIterations++;
        } else {
          lastFeasibleIterations = 1;
        }
      } else {
        if (lastFeasibleIterations < 0) {
          lastFeasibleIterations--;
        } else {
          lastFeasibleIterations = -1;
        }
      }

      // Update excess capacity penalty, if necessary.
      if (lastFeasibleIterations > params.excessCapacityPenaltyIncreaseThreshold) {
        excessCapacityPenaltyCoefficient = Math.max(
            excessCapacityPenaltyCoefficient / params.excessCapacityPenaltyMultiplier,
            params.excessCapacityMinPenalty);
      } else if (lastFeasibleIterations < -params.excessCapacityPenaltyIncreaseThreshold) {
        excessCapacityPenaltyCoefficient = Math.min(
            excessCapacityPenaltyCoefficient * params.excessCapacityPenaltyMultiplier,
            params.excessCapacityMaxPenalty);
      }

      // Randomly optimize the solution.
      if (rand.nextDouble()
          < params.randomOptimizationChance) { // kOptimize chance of optimization.
        routeList.length += routeList.routes.get(rand.nextInt(routeList.routes.size()))
            .optimize(distances, params.optimizationTimeout);
      }

      // Random restarts.
      if (iterationsSinceLastIncumbent > restartThreshold) {
        // Clear short-term memory
        shortTermMemory.clear();
        // Instantiate coefficients.
        largeNeighborhoodSize = params.largeNeighborhoodBaseSize;
        excessCapacityPenaltyCoefficient = params.excessCapacityBasePenalty;
        customerUsePenaltyCoefficient = params.customerUseBasePenalty;
        restartThreshold *= params.restartThresholdMultiplier;
        // Generate the initial solution, initialize variables.
        routeList = generateInitialSolution();
        // Optimize routes.
        for (Route route : routeList.routes) {
          routeList.length += route.optimize(distances, params.optimizationTimeout);
        }
        incumbent = routeList.clone();
        objective = routeList.length;
        iterationsSinceLastIncumbent = 0;
        lastFeasibleIterations = 1;
      }

      // Log the data to the console.
      System.out.println("\tCurrent objective: " + objective);
      System.out.println("\tCurrent incumbent: " + incumbent.length);
      System.out.println("\tBest incumbent: " + bestIncumbent.length);
      System.out.println("--> PENALTIES");
      System.out.println("\tEC Penalty Coefficient: " + excessCapacityPenaltyCoefficient);
      System.out.println("\tCU Penalty Coefficient: " + customerUsePenaltyCoefficient);
      System.out.println("-->  MEMORY");
      System.out.println("\tShort-term memory: " + shortTermMemory);
      System.out.println("\tLong-term memory: " + longTermMemory);
      System.out.println("-->  NBHD & RESTARTS");
      System.out.println("\tCurrent 2-interchange trials #: " + largeNeighborhoodSize);
      System.out.println("\tCurrent # of last (in)feasible iterations: " + lastFeasibleIterations);
      System.out.println(
          "\tCurrent # of iterations since last incumbent: " + iterationsSinceLastIncumbent);
      System.out.println("\tRestart threshold: " + restartThreshold);
      System.out.println("-->  TIME");
      System.out.println("\tElapsed time: " + watch.getTime());
    }
  }

  private void updateInterchange(Interchange interchange, double interchangeObjective) {
    System.out.println("-->  PERFORMING ACTION");
    System.out.println("\t" + interchange);
    objective = interchangeObjective;

    // Add the customers to the short-term memory.
    for (Insertion insertion : interchange.insertionList1) {
      shortTermMemory.add(
          new TabuItem(
              routeList.routes.get(interchange.routeIdx1).customers.get(insertion.fromCustomerIdx),
              currentIteration + getRandomTabuTenure()));
      longTermMemory.merge(
          routeList.routes.get(interchange.routeIdx1).customers.get(insertion.fromCustomerIdx), 1,
          Integer::sum);
    }
    for (Insertion insertion : interchange.insertionList2) {
      shortTermMemory.add(
          new TabuItem(
              routeList.routes.get(interchange.routeIdx2).customers.get(insertion.fromCustomerIdx),
              currentIteration + getRandomTabuTenure()));
      longTermMemory.merge(
          routeList.routes.get(interchange.routeIdx2).customers.get(insertion.fromCustomerIdx), 1,
          Integer::sum);
    }

    // Perform the actual interchange.
    routeList.perform(interchange);
  }

  private Interchange searchNeighborhood(InterchangePerRoute lambda) {
    // We should consider the best available objective to avoid local minima.
    Interchange bestInterchange = null;
    double bestObjective = Double.POSITIVE_INFINITY;

    List<Callable<InterchangeResult>> tasks = new ArrayList<>();

    // Checks every customer index.
    for (int routeIdx1 = 0; routeIdx1 < routeList.routes.size(); routeIdx1++) {
      InterchangeCalculator task = lambda.op(routeIdx1);
      tasks.add(task);
    }

    try {
      List<Future<InterchangeResult>> futures = executor.invokeAll(tasks);
      for (Future<InterchangeResult> future : futures) {
        InterchangeResult result = future.get();
        if (result.objective < bestObjective) {
          // Save the best place to insert this customer so far.
          bestInterchange = result.interchange;
          bestObjective = result.objective;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return bestInterchange;
  }

  /**
   * Calculates a random tabu tenure, on a uniform distribution, according to [tabuMin, tabuMax].
   *
   * @return random tabu tenure.
   */
  private int getRandomTabuTenure() {
    return rand.nextInt((params.maximumTabuTenure - params.minimumTabuTenure) + 1)
        + params.minimumTabuTenure;
  }

  /**
   * For each vehicle's route, checks how much over capacity it is.
   *
   * @param routeList routes to calculate the excess capacity for.
   * @return excess capacity.
   */
  public int calculateExcessCapacity(RouteList routeList) {
    int excessCapacity = 0;
    // for each vehicle
    for (Route route : routeList.routes) {
      // only if its over what it should be, add amount over
      if (vehicleCapacity < route.demand) {
        excessCapacity += route.demand - vehicleCapacity;
      }
    }
    return excessCapacity;
  }

  /**
   * Generates initial feasible solution via solving a bin packing problem.
   *
   * @return routes of the initial feasible solution.
   */
  private RouteList generateInitialSolution() {
    try (IloCplex bppModel = new IloCplex()) {
      bppModel.setOut(null);
      bppModel.setWarning(null);

      bppModel.setParam(Param.RandomSeed, rand.nextInt(10000));

      IloNumVar[] useVehicles = bppModel.boolVarArray(numVehicles);
      IloNumVar[][] customerVehicleAssignment = new IloNumVar[numCustomers - 1][numVehicles];

      for (int i = 0; i < numCustomers - 1; i++) {
        for (int j = 0; j < numVehicles; j++) {
          customerVehicleAssignment[i][j] = bppModel.boolVar();
        }
      }

      // Enforce each customer (item) being assigned to only one vehicle (bin).
      for (int i = 0; i < numCustomers - 1; i++) {
        IloLinearNumExpr totalAssignments = bppModel.linearNumExpr();
        for (int j = 0; j < numVehicles; j++) {
          totalAssignments.addTerm(1, customerVehicleAssignment[i][j]);
        }
        bppModel.addEq(totalAssignments, 1);
      }

      // Enforce capacity constraints.
      for (int i = 0; i < numVehicles; i++) {
        IloLinearNumExpr totalLoad = bppModel.linearNumExpr();
        for (int j = 0; j < numCustomers - 1; j++) {
          totalLoad.addTerm(customerVehicleAssignment[j][i], demandOfCustomer[j + 1]);
        }
        IloLinearNumExpr maxLoad = bppModel.linearNumExpr();
        maxLoad.addTerm(vehicleCapacity, useVehicles[i]);
        bppModel.addLe(totalLoad, maxLoad);
      }

      if (bppModel.solve()) {
        List<Route> initialRoutes = new ArrayList<>();
        double initialRoutesLength = 0;

        for (int i = 0; i < numVehicles; i++) {
          List<Integer> currentRoute = new ArrayList<>();
          int currentRouteDemand = 0;
          double currentRouteLength = 0;

          currentRoute.add(0);
          for (int j = 0; j < numCustomers - 1; j++) {
            if (bppModel.getValue(customerVehicleAssignment[j][i]) == 1) {
              currentRoute.add(j + 1);
              currentRouteDemand += demandOfCustomer[j + 1];
            }
          }
          currentRoute.add(0);

          for (int customerIdx = 0; customerIdx < currentRoute.size() - 1; customerIdx++) {
            currentRouteLength += distances[currentRoute.get(customerIdx)][currentRoute.get(
                customerIdx + 1)];
          }

          initialRoutes.add(new Route(currentRoute, currentRouteDemand));
          initialRoutesLength += currentRouteLength;
        }

        return new RouteList(initialRoutes, initialRoutesLength, distances, demandOfCustomer,
            vehicleCapacity, longTermMemory, numCustomers, 0);
      } else {
        throw new IllegalArgumentException("Infeasible BPP model.");
      }
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  // Serialize all routes into the required format.
  public String serializeRoutes(RouteList routeList) {
    // Add the vehicles that didn't go
    int excessVehicles = numVehicles - routeList.routes.size();
    for (int i = 0; i < excessVehicles; i++) {
      List<Integer> excess = new ArrayList<>();
      excess.add(0);
      excess.add(0);
      routeList.routes.add(new Route(excess, 0));
    }

    System.out.println("Routes: " + routeList.routes.size());
    for (Route route : routeList.routes) {
      for (int customer : route.customers) {
        System.out.print(customer + " ");
      }
      System.out.println();
    }

    // convert to a string
    List<Integer> flattenedList = new ArrayList<>();
    flattenedList.add(0); // NOTE: 1 HERE IF PROVED OPTIMAL, ELSE 0

    for (Route route : routeList.routes) {
      flattenedList.addAll(route.customers);
    }

    StringBuilder sb = new StringBuilder();
    for (Integer number : flattenedList) {
      sb.append(number).append(" ");
    }

    return sb.toString().trim();
  }

}
