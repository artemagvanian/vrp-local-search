package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;


public class VRPInstanceIncomplete extends VRPInstance {

  private final int maxIterations;
  public List<List<Integer>> incumbent;
  private double objective;
  private double excessCapacityPenaltyCoefficient = 5.0;

  VRPInstanceIncomplete(String fileName, int maxIterations) {
    super(fileName);
    routes = generateInitialSolution();
    incumbent = cloneRoutes(routes);
    objective = calculateObjective();
    this.maxIterations = maxIterations;
    System.out.println("Initial objective: " + objective);

    optimizeCustomerInterchange();
  }

  // Interchanges 0 or more customers between routes to improve the current solution.
  // \lambda = 1:
  // Swaps and shifts: (1, 1), (1, 0)
  private void optimizeCustomerInterchange() {
    OneInterchange bestInsertion;
    OneInterchange bestSwap;

    int numIterations = 0;

    // Keep going for a fixed number of iterations.
    do {
      numIterations++;
      // Calculate both best insertion and best swap.
      bestInsertion = findBestInsertion();
      bestSwap = findBestSwap();

      if (bestInsertion.newObjective() <= bestSwap.newObjective()) {
        System.out.println("Performing action: " + bestInsertion);
        objective = bestInsertion.newObjective();
        // Perform the insertion of the original coordinate into the new spot.
        routes.get(bestInsertion.toRouteIdx())
            .add(bestInsertion.toCustomerIdx(),
                routes.get(bestInsertion.fromRouteIdx()).get(bestInsertion.fromCustomerIdx()));
        // Remove the original value.
        routes.get(bestInsertion.fromRouteIdx()).remove(bestInsertion.fromCustomerIdx());
      }

      if (bestSwap.newObjective() < bestInsertion.newObjective()) {
        System.out.println("Performing action: " + bestSwap);
        objective = bestSwap.newObjective();
        // Perform the actual swap.
        performSwap(bestSwap.fromRouteIdx(), bestSwap.fromCustomerIdx(),
            bestSwap.toRouteIdx(), bestSwap.toCustomerIdx());
      }

      if (getTourLength(routes) < getTourLength(incumbent)
          && calculateExcessCapacity(routes) == 0) {
        incumbent = cloneRoutes(routes);
      }

      if (calculateExcessCapacity(routes) == 0) {
        excessCapacityPenaltyCoefficient /= 2;
      } else {
        excessCapacityPenaltyCoefficient *= 2;
      }

      System.out.println("Current objective: " + objective);
      System.out.println("Penalty Coefficient: " + excessCapacityPenaltyCoefficient);

    } while (numIterations < maxIterations);
  }

  // Find the best possible insertion.
  private OneInterchange findBestInsertion() {
    double newObjective = Double.POSITIVE_INFINITY;
    OneInterchange bestInsertion = null;

    // checks every customer index
    for (int routeIdx = 0; routeIdx < routes.size(); routeIdx++) {
      for (int customerIdx = 1; customerIdx < routes.get(routeIdx).size() - 1; customerIdx++) {
        OneInterchange bestInsertionForCustomer =
            findBestInsertionForCustomer(routeIdx, customerIdx);

        // if we have a new winner
        if (bestInsertionForCustomer.newObjective() < newObjective) {
          newObjective = bestInsertionForCustomer.newObjective();
          bestInsertion = bestInsertionForCustomer;
        }
      }
    }

    return bestInsertion;
  }

  // Find the best possible insertion for a given customer.
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

  // Find the best possible swap.
  private OneInterchange findBestSwap() {
    // TODO: this takes a lot of time to calculate, we should consider alternatives.
    // Calculate all pairs of customers that aren't on the same route.
    List<OneInterchange> customerPairs = getAllSwaps();

    OneInterchange bestSwap = null;
    double bestObjective = Double.POSITIVE_INFINITY;

    // For each generated pair.
    for (OneInterchange swap : customerPairs) {
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

  // Calculates all pairs of customer indices, excluding those on the same route.
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

  // For each vehicle's route, check how much over capacity it is.
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

  public List<List<Integer>> cloneRoutes(List<List<Integer>> routes) {
    List<List<Integer>> routesCopy = new ArrayList<>();

    for (List<Integer> route : routes) {
      routesCopy.add(new ArrayList<>(route));
    }

    return routesCopy;
  }

  // Generate initial feasible solution via solving a bin packing problem.
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
