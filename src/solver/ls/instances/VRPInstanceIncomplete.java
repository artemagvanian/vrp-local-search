package solver.ls.instances;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.Route;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;
import solver.ls.interchanges.BestInsertionCalculator;
import solver.ls.interchanges.BestSwapCalculator;
import solver.ls.utils.Timer;

public class VRPInstanceIncomplete extends VRPInstance {

  /**
   * Number of iterations to run the search for.
   */
  private final int maxIterations = 10000;
  /**
   * The minimum value tabu tenure can take.
   */
  private final int minimumTabuTenure = 5;
  /**
   * The maximum value tabu tenure can take.
   */
  private final int maximumTabuTenure = 10;
  /**
   * Timeout to optimize the solution (seconds).
   */
  private final double optimizationTimeout = 5;
  /**
   * Allowed solution time (seconds).
   */
  private final double timeout = 300;
  /**
   * Increase greediness by grabbing the first solution that is better than the incumbent.
   */
  private final boolean firstBestFirst = false;
  /**
   * Randomly optimizes the incumbent with kOptimize chance.
   */
  private final double kOptimize = 0.25;
  /**
   * Randomly optimizes the incumbent with 1/kOptimize chance.
   */
  private final double penaltyMultiplier = 1.5;
  /**
   * How many feasible/infeasible assignments we should have to start changing the penalty.
   */
  private final int feasibilityBound = 10;
  /**
   * Memory list to keep the recently moved customers.
   */
  private final List<TabuItem> shortTermMemory;
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
   * Current best solution, with no excess capacity.
   */
  public RouteList incumbent;
  /**
   * Current solution, not necessarily optimal.
   */
  public RouteList routeList;
  /**
   * Current iteration of SLS.
   */
  public int currentIteration = 0;
  /**
   * Current best objective, calculated as specified by the objective function.
   */
  private double objective;
  /**
   * Initial penalty coefficient for the objective function.
   */
  private double penaltyCoefficient = 1;
  /**
   * How many feasible/infeasible assignments we had.
   */
  private int numLastFeasible = 1;

  public VRPInstanceIncomplete(String fileName, Timer watch) {
    super(fileName);
    // Copy parameters.
    this.watch = watch;
    // Generate the initial solution, initialize variables.
    routeList = generateInitialSolution();
    // Optimize routes.
    for (Route route : routeList.routes) {
      routeList.length += route.optimize(distances, optimizationTimeout);
    }
    incumbent = routeList.clone();
    objective = calculateObjective(incumbent.length, 0);
    // Initialize helpers.
    shortTermMemory = new ArrayList<>();
    rand = new Random();
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

    currentIteration = 0;

    // Keep going for a fixed number of iterations.
    while (currentIteration < maxIterations && watch.getTime() < timeout - 5) {
      currentIteration++;
      // Calculate both best insertion and best swap.
      bestInsertion = findBestInsertion();
      bestSwap = findBestSwap();

      // Get insertion objectives, if possible.
      double insertionObjective = bestInsertion == null ? Double.POSITIVE_INFINITY
          : calculateObjective(
              routeList.length + routeList.calculateEdgeDelta(bestInsertion),
              routeList.calculateExcessCapacity(bestInsertion));
      double swapObjective = bestSwap == null ? Double.POSITIVE_INFINITY
          : calculateObjective(routeList.length + routeList.calculateEdgeDelta(bestSwap),
              routeList.calculateExcessCapacity(bestSwap));

      // Route indices of the interchange, so we could optimize those later.
      int routeIdx1 = 0;
      int routeIdx2 = 0;

      // Only perform an action if either of the interchanges is not null.
      if (bestInsertion != null || bestSwap != null) {
        // If current best insertion is better.
        if (insertionObjective <= swapObjective) {
          System.out.println("Performing action: " + bestInsertion);
          objective = insertionObjective;
          // Add the customer to the short-term memory.
          assert bestInsertion != null;
          shortTermMemory.add(new TabuItem(
              routeList.routes.get(bestInsertion.routeIdx1).customers.get(
                  bestInsertion.insertionList1.get(0).fromCustomerIdx),
              currentIteration + getRandomTabuTenure()));
          routeIdx1 = bestInsertion.routeIdx1;
          routeIdx2 = bestInsertion.routeIdx2;
          routeList.perform(bestInsertion);
        } else { // If current best swap is better.
          System.out.println("Performing action: " + bestSwap);
          objective = swapObjective;
          assert bestSwap != null;
          // Add the customers to the short-term memory.
          shortTermMemory.add(new TabuItem(routeList.routes.get(bestSwap.routeIdx1).customers.get(
              bestSwap.insertionList1.get(0).fromCustomerIdx),
              currentIteration + getRandomTabuTenure()));
          shortTermMemory.add(new TabuItem(routeList.routes.get(bestSwap.routeIdx2).customers.get(
              bestSwap.insertionList2.get(0).fromCustomerIdx),
              currentIteration + getRandomTabuTenure()));
          routeIdx1 = bestSwap.routeIdx1;
          routeIdx2 = bestSwap.routeIdx2;
          // Perform the actual swap.
          routeList.perform(bestSwap);
        }
      }

      // Remove all tabu items that are past the tenure.
      int finalCurrentIteration = currentIteration;
      shortTermMemory.removeIf(tabuItem -> (tabuItem.expirationIteration < finalCurrentIteration));

      // Check whether we should update the incumbent.
      if (routeList.length < incumbent.length && calculateExcessCapacity(routeList) == 0) {
        if (rand.nextDouble() < kOptimize) { // kOptimize chance of optimization.
          routeList.length += routeList.routes.get(routeIdx1)
              .optimize(distances, optimizationTimeout);
          routeList.length += routeList.routes.get(routeIdx2)
              .optimize(distances, optimizationTimeout);
        }
        incumbent = routeList.clone();
      }

      // Adjust the number of last feasible solutions.
      if (calculateExcessCapacity(routeList) == 0) {
        if (numLastFeasible > 0) {
          numLastFeasible++;
        } else {
          numLastFeasible = 1;
        }
      } else {
        if (numLastFeasible < 0) {
          numLastFeasible--;
        } else {
          numLastFeasible = -1;
        }
      }

      // Update penalty coefficient.
      if (numLastFeasible > feasibilityBound) {
        penaltyCoefficient /= penaltyMultiplier;
      } else if (numLastFeasible < -feasibilityBound) {
        penaltyCoefficient *= penaltyMultiplier;
      }

      // Log the data to the console.
      System.out.println("Iteration #" + currentIteration);
      System.out.println("Current objective: " + objective);
      System.out.println("Penalty Coefficient: " + penaltyCoefficient);
      System.out.println("Short-term memory: " + shortTermMemory);
      System.out.println("Elapsed time: " + watch.getTime());
    }
  }

  /**
   * Finds the best possible insertion.
   *
   * @return best possible insertion from the current routes.
   */
  private Interchange findBestInsertion() {
    // We should consider the best available objective to avoid local minima.
    Interchange bestInterchange = null;
    double bestObjective = Double.POSITIVE_INFINITY;

    List<Callable<InterchangeResult>> tasks = new ArrayList<>();

    // Checks every customer index.
    for (int routeIdx1 = 0; routeIdx1 < routeList.routes.size(); routeIdx1++) {
      BestInsertionCalculator task = new BestInsertionCalculator(routeIdx1, routeList, incumbent,
          penaltyCoefficient, shortTermMemory, firstBestFirst);
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
    return rand.nextInt((maximumTabuTenure - minimumTabuTenure) + 1) + minimumTabuTenure;
  }

  /**
   * Finds the best possible swap.
   *
   * @return best possible swap from the current routes
   */
  private Interchange findBestSwap() {
    // We should consider the best available objective to avoid local minima.
    Interchange bestInterchange = null;
    double bestObjective = Double.POSITIVE_INFINITY;

    List<Callable<InterchangeResult>> tasks = new ArrayList<>();

    // Checks every customer index.
    for (int routeIdx1 = 0; routeIdx1 < routeList.routes.size(); routeIdx1++) {
      BestSwapCalculator task = new BestSwapCalculator(routeIdx1, routeList, incumbent,
          penaltyCoefficient, shortTermMemory, firstBestFirst);
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

  private double calculateObjective(double tourLength, double excessCapacity) {
    return tourLength + excessCapacity * penaltyCoefficient;
  }

  /**
   * For each vehicle's route, checks how much over capacity it is.
   *
   * @param routeList routes to calculate the excess capacity for.
   * @return excess capacity.
   */
  public double calculateExcessCapacity(RouteList routeList) {
    double excessCapacity = 0;
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
            vehicleCapacity);
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
