package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;


public class VRPInstanceIncomplete extends VRPInstance {

  private double objective;

  VRPInstanceIncomplete(String fileName) {
    super(fileName);
    routes = generateInitialSolution();
    objective = calculateObjective();
    System.out.println("Initial objective: " + objective);

    optimizeCustomerInterchange();
  }

  // Interchanges 0 or more customers between routes to improve the current solution.
  // \lambda = 1:
  // Swaps and shifts: (1, 1), (1, 0)
  private void optimizeCustomerInterchange() {
    OneInterchange bestInsertion;
    OneInterchange bestSwap;

    // keep going while we are looking for a solution
    do {
      // 2. see if insertions do anything
      bestInsertion = findBestInsertion();
      if (bestInsertion != null) {
        System.out.println("Performing action: " + bestInsertion);
        objective = bestInsertion.newObjective();
        // perform the insertion of the original coordinate into the new spot
        routes.get(bestInsertion.toRouteIdx())
            .add(bestInsertion.toCustomerIdx(),
                routes.get(bestInsertion.fromRouteIdx()).get(bestInsertion.fromCustomerIdx()));
        // remove the original value
        routes.get(bestInsertion.fromRouteIdx()).remove(bestInsertion.fromCustomerIdx());
      }

      // 3. see if swaps do anything
      // currently lambda = 1
      bestSwap = findBestSwap();
      // there exists some swap that will improve the solution
      if (bestSwap != null) {
        System.out.println("Performing action: " + bestSwap);
        objective = bestSwap.newObjective();
        // perform the actual swap
        performSwap(bestSwap.fromRouteIdx(), bestSwap.fromCustomerIdx(),
            bestSwap.toRouteIdx(), bestSwap.toCustomerIdx());
      }
      System.out.println(objective);

    } while (bestInsertion != null || bestSwap != null);
  }

  // calculate insertions
  private OneInterchange findBestInsertion() {
    double newObjective = objective;
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

  // find all places where the given customer at route index can be
  private OneInterchange findBestInsertionForCustomer(int currentRouteIdx, int currentCustomerIdx) {
    double bestObjective = objective;
    int bestRouteIdx = currentRouteIdx;
    int bestCustomerIdx = currentCustomerIdx;
    int customer = routes.get(currentRouteIdx).get(currentCustomerIdx);

    // remove the customer from where it currently is
    routes.get(currentRouteIdx).remove(currentCustomerIdx);

    // iterate over every place in the route
    for (int routeIdx = 0; routeIdx < routes.size(); routeIdx++) {
      if (routeIdx == currentRouteIdx) {
        continue;
      }
      List<Integer> r = routes.get(routeIdx);
      for (int insertAtIdx = 1; insertAtIdx < r.size(); insertAtIdx++) {
        // add this current insertion into this route
        r.add(insertAtIdx, customer);

        // calculate objective function and check it, accounting for infeasibility
        double newObjective = calculateObjective();
        double amountOverCapacity = calculateAmountOverCapacity();
        if (newObjective < bestObjective && amountOverCapacity == 0) {
          // save the best places to insert this customer
          bestRouteIdx = routeIdx;
          bestCustomerIdx = insertAtIdx;
          bestObjective = newObjective;
        }

        // reset back to the original for next iteration
        r.remove(insertAtIdx);
      }
    }

    // reset back to the original route
    routes.get(currentRouteIdx).add(currentCustomerIdx, customer);

    return new OneInterchange(
        InterchangeType.Insertion, bestObjective,
        currentRouteIdx, currentCustomerIdx, bestRouteIdx, bestCustomerIdx);
  }

  // calculate the effect of swapping on every pair of customers
  private OneInterchange findBestSwap() {
    // THIS WORK CAN maybe? BE OPTIMIZED TO DONE BEFOREHAND
    // calculate all pairs of customers that aren't on the same route
    List<OneInterchange> customerPairs = getAllSwaps();

    OneInterchange bestSwap = null;
    double bestObjective = objective;

    // for each pair
    for (OneInterchange swap : customerPairs) {
      // swap
      performSwap(swap.fromRouteIdx(), swap.fromCustomerIdx(), swap.toRouteIdx(),
          swap.toCustomerIdx());
      // calculate new objective function
      double newObjective = calculateObjective();
      double amountOverCapacity = calculateAmountOverCapacity();
      // swap back
      performSwap(swap.fromRouteIdx(), swap.fromCustomerIdx(), swap.toRouteIdx(),
          swap.toCustomerIdx());

      // if it was better than the current strategy
      if (newObjective < bestObjective && amountOverCapacity == 0) {
        // save these values
        bestObjective = newObjective;
        bestSwap = new OneInterchange(InterchangeType.Swap, newObjective,
            swap.fromRouteIdx(), swap.fromCustomerIdx(), swap.toRouteIdx(), swap.toCustomerIdx());
      }
    }

    return bestSwap;
  }

  // calculates all pairs of customer indices, excluding those on the same route
  // It's essentially a list of tuples of coordinate pairs
  private List<OneInterchange> getAllSwaps() {
    List<OneInterchange> swaps = new ArrayList<>();

    // iterate through all routes
    for (int routeIdx1 = 0; routeIdx1 < routes.size(); routeIdx1++) {
      for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routes.size(); routeIdx2++) {
        List<Integer> route1 = routes.get(routeIdx1);
        List<Integer> route2 = routes.get(routeIdx2);
        // account for depots here
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
    // objective function: c'(x) = c(x) + Q(x) + D(x)
    //                    route cost + tot excess capacity   tot excess distance
    return getTourLength();
  }


  // for each vehicle's route, check how much over capacity it is
  public double calculateAmountOverCapacity() {
    double overCapacity = 0;
    double capacity = 0;
    // for each vehicle
    for (List<Integer> route : routes) {
      capacity = 0;
      // calculate capacity
      for (int i : route) {
        capacity += demandOfCustomer[i];
      }
      // only if its over what it should be, add amount over
      if (vehicleCapacity < capacity) {
        overCapacity += capacity - vehicleCapacity;
      }
    }
    return overCapacity;
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

      // Enforce each customer (item) being assigned to only one vehicle (bin)
      for (int i = 0; i < numCustomers - 1; i++) {
        IloLinearNumExpr totalAssignments = bppModel.linearNumExpr();
        for (int j = 0; j < numVehicles; j++) {
          totalAssignments.addTerm(1, customerVehicleAssignment[i][j]);
        }
        bppModel.addEq(totalAssignments, 1);
      }

      // Enforce capacity constraints
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
