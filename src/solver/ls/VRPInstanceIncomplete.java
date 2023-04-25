package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class VRPInstanceIncomplete extends VRPInstance {

  /**
   * Number of iterations to run the search for.
   */
  private final int maxIterations;
  /**
   * For how many iterations the customer should not be touched after a move.
   */
  private final int tabuTenure;
  /**
   * Memory list to keep the recently moved customers.
   */
  private final List<TabuItem> shortTermMemory;
  /**
   * Current best solution, with no excess capacity.
   */
  public List<List<Integer>> incumbent;
  /**
   * Current best objective, calculated as specified by the objective function.
   */
  private double objective;
  /**
   * Initial penalty coefficient for the objective function.
   */
  private double excessCapacityPenaltyCoefficient = 1;

  VRPInstanceIncomplete(String fileName, int maxIterations, int tabuTenure) {
    super(fileName);
    // Copy parameters.
    this.maxIterations = maxIterations;
    this.tabuTenure = tabuTenure;
    // Generate the initial solution, initialize variables.
    routes = generateInitialSolution();
    incumbent = cloneRoutes(routes);
    objective = calculateObjective();
    shortTermMemory = new ArrayList<>(tabuTenure);
    // Objective of the initial solution.
    System.out.println("Initial objective: " + objective);
    // Perform search for a given number of iterations.
    search();
  }

  /**
   * Interchanges 0 or 1 customers between routes to improve the current solution. In our case,
   * \lambda = 1, so we consider swaps and shifts: (1, 1), (1, 0).
   */
  private void search() {
    OneInterchange bestInsertion;
    OneInterchange bestSwap;

    int currentIteration = 0;

    // Keep going for a fixed number of iterations.
    while (currentIteration < maxIterations) {
      currentIteration++;
      // Calculate both best insertion and best swap.
      bestInsertion = findBestInsertion();
      bestSwap = findBestSwap();

      // Only perform an action if either of the interchanges is not null.
      if (bestInsertion != null || bestSwap != null) {
        // If current best insertion is better.
        if (bestSwap == null || (bestInsertion != null
            && bestInsertion.newObjective() <= bestSwap.newObjective())) {
          System.out.println("Performing action: " + bestInsertion);
          objective = bestInsertion.newObjective();
          // Add the customer to the short-term memory.
          shortTermMemory.add(new TabuItem(
              routes.get(bestInsertion.fromRouteIdx()).get(bestInsertion.fromCustomerIdx()),
              currentIteration));
          // Perform the insertion of the original coordinate into the new spot.
          routes.get(bestInsertion.toRouteIdx())
              .add(bestInsertion.toCustomerIdx(),
                  routes.get(bestInsertion.fromRouteIdx()).get(bestInsertion.fromCustomerIdx()));
          // Remove the original value.
          routes.get(bestInsertion.fromRouteIdx()).remove(bestInsertion.fromCustomerIdx());
        } else { // If current best swap is better.
          System.out.println("Performing action: " + bestSwap);
          objective = bestSwap.newObjective();
          // Perform the actual swap.
          performSwap(bestSwap.fromRouteIdx(), bestSwap.fromCustomerIdx(),
              bestSwap.toRouteIdx(), bestSwap.toCustomerIdx());
          // Add the customers to the short-term memory.
          shortTermMemory.add(
              new TabuItem(routes.get(bestSwap.fromRouteIdx()).get(bestSwap.fromCustomerIdx()),
                  currentIteration));
          shortTermMemory.add(
              new TabuItem(routes.get(bestSwap.toRouteIdx()).get(bestSwap.toCustomerIdx()),
                  currentIteration));
        }
      }

      // Remove all tabu items that are past the tenure.
      int finalCurrentIteration = currentIteration;
      shortTermMemory.removeIf(
          tabuItem -> (tabuItem.iteration() + tabuTenure < finalCurrentIteration));

      // Check whether we should update the incumbent.
      if (getTourLength(routes) < getTourLength(incumbent)
          && calculateExcessCapacity(routes) == 0) {
        incumbent = cloneRoutes(routes);
      }

      // Adjust excess capacity coefficient.
      if (calculateExcessCapacity(routes) == 0) {
        excessCapacityPenaltyCoefficient /= 1.25;
      } else {
        excessCapacityPenaltyCoefficient *= 1.25;
      }

      // Log the data to the console.
      System.out.println("Iteration #" + currentIteration);
      System.out.println("Current objective: " + objective);
      System.out.println("Penalty Coefficient: " + excessCapacityPenaltyCoefficient);
      System.out.println("Short-term memory: " + shortTermMemory);
    }
  }

  /**
   * Finds the best possible insertion.
   *
   * @return best possible insertion from the current routes.
   */
  private OneInterchange findBestInsertion() {
    // We should consider the best available objective to avoid local minima.
    double newObjective = Double.POSITIVE_INFINITY;
    OneInterchange bestInsertion = null;

    // Checks every customer index.
    for (int routeIdx = 0; routeIdx < routes.size(); routeIdx++) {
      for (int customerIdx = 1; customerIdx < routes.get(routeIdx).size() - 1; customerIdx++) {
        // Check whether the current customer is in the tabu list.
        boolean isCustomerTabu = false;
        for (TabuItem tabuCustomer : shortTermMemory) {
          if (Objects.equals(routes.get(routeIdx).get(customerIdx), tabuCustomer.customer())) {
            isCustomerTabu = true;
            break;
          }
        }
        if (isCustomerTabu) {
          continue;
        }

        // Find the best possible insertion for a given non-tabu customer.
        OneInterchange bestInsertionForCustomer =
            findBestInsertionForCustomer(routeIdx, customerIdx);

        // If we have a new winner.
        if (bestInsertionForCustomer.newObjective() < newObjective) {
          newObjective = bestInsertionForCustomer.newObjective();
          bestInsertion = bestInsertionForCustomer;
        }
      }
    }

    return bestInsertion;
  }

  /**
   * Finds the best possible insertion for a given customer.
   *
   * @param currentRouteIdx    route index.
   * @param currentCustomerIdx customer index.
   * @return best possible insertion for a given customer.
   */
  private OneInterchange findBestInsertionForCustomer(int currentRouteIdx, int currentCustomerIdx) {
    double bestObjective = Double.POSITIVE_INFINITY;
    int bestRouteIdx = currentRouteIdx;
    int bestCustomerIdx = currentCustomerIdx;
    int customer = routes.get(currentRouteIdx).get(currentCustomerIdx);

    // Remove the customer from where it currently is.
    routes.get(currentRouteIdx).remove(currentCustomerIdx);

    // Iterate over every place in the route.
    for (int routeIdx = 0; routeIdx < routes.size(); routeIdx++) {
      if (routeIdx == currentRouteIdx) {
        continue;
      }
      List<Integer> route = routes.get(routeIdx);
      for (int insertAtIdx = 1; insertAtIdx < route.size(); insertAtIdx++) {
        // Add current insertion into the route.
        route.add(insertAtIdx, customer);

        // Calculate objective function and check whether it is better than the current.
        double newObjective = calculateObjective();
        if (newObjective < bestObjective) {
          // Save the best place to insert this customer so far.
          bestRouteIdx = routeIdx;
          bestCustomerIdx = insertAtIdx;
          bestObjective = newObjective;
        }

        // Reset back to the original for next iteration.
        route.remove(insertAtIdx);
      }
    }

    // Reset back to the original route.
    routes.get(currentRouteIdx).add(currentCustomerIdx, customer);

    return new OneInterchange(
        InterchangeType.Insertion, bestObjective,
        currentRouteIdx, currentCustomerIdx, bestRouteIdx, bestCustomerIdx);
  }

  /**
   * Finds the best possible swap.
   *
   * @return best possible swap from the current routes
   */
  private OneInterchange findBestSwap() {
    // TODO: this takes a lot of time to calculate, we should consider alternatives.
    // Calculate all pairs of customers that aren't on the same route.
    List<OneInterchange> customerPairs = getAllSwaps();

    OneInterchange bestSwap = null;
    double bestObjective = Double.POSITIVE_INFINITY;

    // For each generated pair.
    for (OneInterchange swap : customerPairs) {
      // Check whether the current customer is in the tabu list.
      boolean isPairTabu = false;
      for (TabuItem tabuCustomer : shortTermMemory) {
        if (Objects.equals(routes.get(swap.fromRouteIdx()).get(swap.fromCustomerIdx()),
            tabuCustomer.customer())
            || Objects.equals(routes.get(swap.toRouteIdx()).get(swap.toCustomerIdx()),
            tabuCustomer.customer())) {
          isPairTabu = true;
          break;
        }
      }
      if (isPairTabu) {
        continue;
      }

      // Swap forward.
      performSwap(swap.fromRouteIdx(), swap.fromCustomerIdx(), swap.toRouteIdx(),
          swap.toCustomerIdx());
      double newObjective = calculateObjective();
      // Swap back.
      performSwap(swap.fromRouteIdx(), swap.fromCustomerIdx(), swap.toRouteIdx(),
          swap.toCustomerIdx());

      // If we are better than what we have now.
      if (newObjective < bestObjective) {
        // Update the best values so far.
        bestObjective = newObjective;
        bestSwap = new OneInterchange(InterchangeType.Swap, newObjective,
            swap.fromRouteIdx(), swap.fromCustomerIdx(), swap.toRouteIdx(), swap.toCustomerIdx());
      }
    }

    return bestSwap;
  }

  /**
   * Calculates all pairs of customer indices, excluding those on the same route.
   *
   * @return all different-route pairs of customer indices.
   */
  private List<OneInterchange> getAllSwaps() {
    List<OneInterchange> swaps = new ArrayList<>();
    // Iterate through all routes.
    for (int routeIdx1 = 0; routeIdx1 < routes.size(); routeIdx1++) {
      for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routes.size(); routeIdx2++) {
        List<Integer> route1 = routes.get(routeIdx1);
        List<Integer> route2 = routes.get(routeIdx2);
        // Account for depots here.
        for (int customerIdx1 = 1; customerIdx1 < route1.size() - 1; customerIdx1++) {
          for (int customerIdx2 = 1; customerIdx2 < route2.size() - 1; customerIdx2++) {
            swaps.add(new OneInterchange(InterchangeType.Swap, 0,
                routeIdx1, customerIdx1, routeIdx2, customerIdx2));
          }
        }
      }
    }

    return swaps;
  }

  /**
   * Swaps two customers.
   *
   * @param routeIdx1    route index of customer 1.
   * @param customerIdx1 customer index of customer 1.
   * @param routeIdx2    route index of customer 2.
   * @param customerIdx2 customer index of customer 2.
   */
  private void performSwap(int routeIdx1, int customerIdx1, int routeIdx2, int customerIdx2) {
    List<Integer> route1 = routes.get(routeIdx1);
    List<Integer> route2 = routes.get(routeIdx2);

    int temp = route1.get(customerIdx1);
    route1.set(customerIdx1, route2.get(customerIdx2));
    route2.set(customerIdx2, temp);
  }

  private double calculateObjective() {
    return getTourLength(routes)
        + calculateExcessCapacity(routes) * excessCapacityPenaltyCoefficient;
  }

  /**
   * For each vehicle's route, checks how much over capacity it is.
   *
   * @param routes routes to calculate the excess capacity for.
   * @return excess capacity.
   */
  public double calculateExcessCapacity(List<List<Integer>> routes) {
    double excessCapacity = 0;
    // for each vehicle
    for (List<Integer> route : routes) {
      double demand = 0;
      // calculate capacity
      for (int customer : route) {
        demand += demandOfCustomer[customer];
      }
      // only if its over what it should be, add amount over
      if (vehicleCapacity < demand) {
        excessCapacity += demand - vehicleCapacity;
      }
    }
    return excessCapacity;
  }

  /**
   * Perform a deep copy of the routes.
   *
   * @param routes routes to clone.
   * @return deep copy of the routes.
   */
  public List<List<Integer>> cloneRoutes(List<List<Integer>> routes) {
    List<List<Integer>> routesCopy = new ArrayList<>();

    for (List<Integer> route : routes) {
      routesCopy.add(new ArrayList<>(route));
    }

    return routesCopy;
  }

  /**
   * Generates initial feasible solution via solving a bin packing problem.
   *
   * @return routes of the initial feasible solution.
   */
  private List<List<Integer>> generateInitialSolution() {
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
          totalLoad.addTerm(customerVehicleAssignment[j][i],
              demandOfCustomer[j + 1]);
        }
        IloLinearNumExpr maxLoad = bppModel.linearNumExpr();
        maxLoad.addTerm(vehicleCapacity, useVehicles[i]);
        bppModel.addLe(totalLoad, maxLoad);
      }

      if (bppModel.solve()) {
        List<List<Integer>> initialRoutes = new ArrayList<>();
        for (int i = 0; i < numVehicles; i++) {
          List<Integer> currentRoute = new ArrayList<>();
          currentRoute.add(0);
          for (int j = 0; j < numCustomers - 1; j++) {
            if (bppModel.getValue(customerVehicleAssignment[j][i]) == 1) {
              currentRoute.add(j + 1);
            }
          }
          currentRoute.add(0);
          initialRoutes.add(currentRoute);
        }
        return initialRoutes;
      } else {
        throw new IllegalArgumentException("Infeasible BPP model.");
      }
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }
}
