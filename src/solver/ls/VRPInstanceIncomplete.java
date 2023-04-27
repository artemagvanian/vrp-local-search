package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class VRPInstanceIncomplete extends VRPInstance {

  /**
   * Number of iterations to run the search for.
   */
  private final int maxIterations;
  /**
   * The minimum value tabu tenure can take.
   */
  private final int minimumTabuTenure;
  /**
   * The maximum value tabu tenure can take.
   */
  private final int maximumTabuTenure;
  /**
   * Memory list to keep the recently moved customers.
   */
  private final Set<TabuItem> shortTermMemory;
  /**
   * Random number generator for the instance.
   */
  private final Random rand;
  /**
   * Current best solution, with no excess capacity.
   */
  public RouteList incumbent;
  /**
   * Current solution, not necessarily optimal.
   */
  public RouteList routeList;
  /**
   * Current best objective, calculated as specified by the objective function.
   */
  private double objective;
  /**
   * Initial penalty coefficient for the objective function.
   */
  private double excessCapacityPenaltyCoefficient = 1;

  VRPInstanceIncomplete(String fileName, int maxIterations) {
    super(fileName);
    // Copy parameters.
    this.maxIterations = maxIterations;
    // Set tabu tenure limits
    this.minimumTabuTenure = 5;
    this.maximumTabuTenure = 5;
    // Generate the initial solution, initialize variables.
    routeList = generateInitialSolution();
    incumbent = routeList.clone();
    objective = calculateObjective(incumbent.length, 0);
    shortTermMemory = new HashSet<>();
    rand = new Random();
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
    Interchange bestInsertion;
    Interchange bestSwap;

    int currentIteration = 0;

    // Keep going for a fixed number of iterations.
    while (currentIteration < maxIterations) {
      currentIteration++;
      // Calculate both best insertion and best swap.
      bestInsertion = findBestInsertion();
      bestSwap = findBestSwap();

      double insertionObjective = bestInsertion == null ? Double.POSITIVE_INFINITY
          : calculateObjective(
              routeList.length + routeList.calculateEdgeDelta(bestInsertion, distances),
              routeList.calculateExcessCapacity(bestInsertion, demandOfCustomer, vehicleCapacity));
      double swapObjective = bestSwap == null ? Double.POSITIVE_INFINITY
          : calculateObjective(routeList.length + routeList.calculateEdgeDelta(bestSwap, distances),
              routeList.calculateExcessCapacity(bestSwap, demandOfCustomer, vehicleCapacity));

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
          routeList.perform(bestInsertion, distances, demandOfCustomer);
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
          // Perform the actual swap.
          routeList.perform(bestSwap, distances, demandOfCustomer);
        }
      }

      // Remove all tabu items that are past the tenure.
      int finalCurrentIteration = currentIteration;
      shortTermMemory.removeIf(tabuItem -> (tabuItem.expirationIteration < finalCurrentIteration));

      // Check whether we should update the incumbent.
      if (routeList.length < incumbent.length && calculateExcessCapacity(routeList) == 0) {
        incumbent = routeList.clone();
      }

      // Adjust excess capacity coefficient.
      if (calculateExcessCapacity(routeList) == 0) {
        excessCapacityPenaltyCoefficient /= 1.25;
      } else {
        excessCapacityPenaltyCoefficient *= 1.25;
        excessCapacityPenaltyCoefficient = Math.min(excessCapacityPenaltyCoefficient, 1000000);
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
  private Interchange findBestInsertion() {
    // We should consider the best available objective to avoid local minima.
    Interchange bestInterchange = null;
    double bestObjective = Double.POSITIVE_INFINITY;

    // Checks every customer index.
    for (int routeIdx1 = 0; routeIdx1 < routeList.routes.size(); routeIdx1++) {
      for (int routeIdx2 = 0; routeIdx2 < routeList.routes.size(); routeIdx2++) {
        if (routeIdx2 == routeIdx1) {
          continue;
        }
        Route route1 = routeList.routes.get(routeIdx1);
        Route route2 = routeList.routes.get(routeIdx2);

        for (int customerIdxFrom = 1; customerIdxFrom < route1.customers.size() - 1;
            customerIdxFrom++) {
          // Check whether the current customer is in the tabu list.
          if (isCustomerTabu(routeIdx1, customerIdxFrom)) {
            continue;
          }

          for (int customerIdxTo = 1; customerIdxTo < route2.customers.size(); customerIdxTo++) {
            Insertion insertion = new Insertion(customerIdxFrom, customerIdxTo);
            Interchange interchange = new Interchange(routeIdx1,
                new ArrayList<>(List.of(insertion)), routeIdx2, new ArrayList<>(List.of()));

            double newTotalLength =
                routeList.length + routeList.calculateEdgeDelta(interchange, distances);
            double excessCapacity = routeList.calculateExcessCapacity(interchange, demandOfCustomer,
                vehicleCapacity);

            // Calculate objective function and check whether it is better than the current.
            double newObjective =
                newTotalLength + excessCapacity * excessCapacityPenaltyCoefficient;
            if (newObjective < bestObjective) {
              // Save the best place to insert this customer so far.
              bestInterchange = interchange;
              bestObjective = newObjective;
            }
          }
        }
      }
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

    // For each generated pair.
    for (int routeIdx1 = 0; routeIdx1 < routeList.routes.size(); routeIdx1++) {
      for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routeList.routes.size(); routeIdx2++) {
        Route route1 = routeList.routes.get(routeIdx1);
        Route route2 = routeList.routes.get(routeIdx2);
        // Account for depots here.
        for (int customer1IdxFrom = 1; customer1IdxFrom < route1.customers.size() - 1;
            customer1IdxFrom++) {
          if (isCustomerTabu(routeIdx1, customer1IdxFrom)) {
            continue;
          }
          for (int customer2IdxFrom = 1; customer2IdxFrom < route2.customers.size() - 1;
              customer2IdxFrom++) {
            if (isCustomerTabu(routeIdx2, customer2IdxFrom)) {
              continue;
            }

            for (int customer1IdxTo = 1; customer1IdxTo < route2.customers.size() - 1;
                customer1IdxTo++) {
              for (int customer2IdxTo = 1; customer2IdxTo < route1.customers.size() - 1;
                  customer2IdxTo++) {
                Insertion insertion1 = new Insertion(customer1IdxFrom, customer2IdxFrom);
                Insertion insertion2 = new Insertion(customer2IdxFrom, customer1IdxFrom);
                Interchange interchange = new Interchange(routeIdx1,
                    new ArrayList<>(List.of(insertion1)), routeIdx2,
                    new ArrayList<>(List.of(insertion2)));

                double newTotalLength =
                    routeList.length + routeList.calculateEdgeDelta(interchange, distances);
                double excessCapacity = routeList.calculateExcessCapacity(interchange,
                    demandOfCustomer, vehicleCapacity);

                double newObjective =
                    newTotalLength + excessCapacity * excessCapacityPenaltyCoefficient;

                // If we are better than what we have now.
                if (newObjective < bestObjective) {
                  // Update the best values so far.
                  bestObjective = newObjective;
                  bestInterchange = interchange;
                }
              }
            }
          }
        }
      }
    }

    return bestInterchange;
  }

  private boolean isCustomerTabu(int routeIdx, int customerIdx) {
    TabuItem customer = new TabuItem(routeList.routes.get(routeIdx).customers.get(customerIdx), 0);
    return shortTermMemory.contains(customer);
  }

  private double calculateObjective(double tourLength, double excessCapacity) {
    return tourLength + excessCapacity * excessCapacityPenaltyCoefficient;
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
        LinkedList<Route> initialRoutes = new LinkedList<>();

        for (int i = 0; i < numVehicles; i++) {
          LinkedList<Integer> currentRoute = new LinkedList<>();
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

          initialRoutes.add(new Route(currentRoute, currentRouteDemand, currentRouteLength));
        }

        double initialRoutesLength = 0;
        for (Route route : initialRoutes) {
          initialRoutesLength += route.length;
        }

        return new RouteList(initialRoutes, initialRoutesLength);
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
      LinkedList<Integer> excess = new LinkedList<>();
      excess.add(0);
      excess.add(0);
      routeList.routes.add(new Route(excess, 0, 0));
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
