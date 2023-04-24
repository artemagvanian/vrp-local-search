package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Random;


public class VRPInstanceIncomplete extends VRPInstance {

  VRPInstanceIncomplete(String fileName) {
    super(fileName);
    routes = generateInitialSolution();
    optimizeCustomerInterchange(routes);
  }

  // Interchanges 0 or more customers between routes to improve the current solution.
  // \lambda = 2:
  // Swaps and shifts: (2, 2), (2, 1), (2, 0), (1, 1), (1, 0)
  private void optimizeCustomerInterchange(List<List<Integer>> routes) {
    // 1. calculate current best objective
    double initialObjective = calculateObjFunction(routes);
    double currentObjective = initialObjective;

    System.out.println("initial: " + initialObjective);

    List<Double> insertions = new ArrayList<>();
    Tuple<Double, Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>>> swap = null;

    // keep going while we are looking for a solution
    while (!(insertions == null && swap == null )) {//&& getAmtOverCapacity(routes) == 0)) {
      // 2. see if insertions do anything
      insertions = calculateInsertions(routes, currentObjective);
      if (insertions != null) {
        // returns output (obs1, obs2, coord1, coord2, bestObj)
        currentObjective = insertions.get(4);
        // perform the insertion of the original coordinate into the new spot
        routes.get(insertions.get(2).intValue()).add(insertions.get(3).intValue(),
                routes.get(insertions.get(0).intValue()).get(insertions.get(1).intValue()));
        // remove the original value
        routes.get(insertions.get(0).intValue()).remove(insertions.get(1).intValue());
      }

//      System.out.println("*****");
//      System.out.println("ATTEMPT Insertion: ");
//      System.out.println(serializeRoutes());
//      System.out.println("*****");

      // 3. see if swaps do anything
      // currently lambda = 1
      swap = calculateSwaps(routes, currentObjective);
      // there exists some swap that will improve the solution
      if (swap != null) {
        currentObjective = swap.first();
        Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> s = swap.second();
        // perform the actual swap
        swap(routes, s.first().first(), s.second().first(), s.first().second(), s.second().second());
      }
      System.out.println();
    }

    System.out.println("AMT OVER CAPACITY: " + getAmtOverCapacity(routes));
  }

  // Optimize structure of each route via switching the edges between customers.
  private void optimizeRouteStructure() {

  }

  // calculate insertions
  private List<Double> calculateInsertions(List<List<Integer>> routes, double currentBest){
    double bestObj = currentBest;
    double insert1 = 0;
    double insert2 = 0;

    double obs1 = 0;
    double obs2 = 0;

    // checks every customer index
    for (int i = 0; i < routes.size() ; i++) {
      for(int j = 1; j < routes.get(i).size() - 1; j++) {
        // returns output (coord1, coord2, bestObj)
        List<Double> output = checkInsertions(routes, currentBest, i, j);

        // if we have a new winner
        if (output.get(2) < bestObj) {
          bestObj = output.get(2);
          insert1 = output.get(0);
          insert2 = output.get(1);
          obs1 = i;
          obs2 = j;
        }
      }
    }

    // NO IMPROVEMENT from any insertions
    if (bestObj == currentBest) {
      return null;
    }

    // generate output list
    List<Double> toRet = new ArrayList<>();
    toRet.add(obs1);
    toRet.add(obs2);
    toRet.add(insert1);
    toRet.add(insert2);
    toRet.add(bestObj);
    return toRet;
  }

  // find all places where the given customer at route index can be
  private List<Double> checkInsertions(List<List<Integer>> routes, double bestObj, int curI, int curJ){
    // to avoid any issues
    // mutability stuff here is realllly sketch
    int bestI = curI;
    int bestJ = curJ;
    int customer = routes.get(curI).get(curJ);

    // remove the customer from where it currently is
    routes.get(curI).remove(curJ);

    // iterate over every place in the route
    for (int i = 0; i < routes.size(); i++) {
          List<Integer> r = routes.get(i);
          for (int j = 1; j < r.size(); j++){
            // add this current insertion into this route
            r.add(j, customer);

            // calculate objective function and check it, accounting for infeasibility
            double obj = calculateObjFunction(routes);
            if (obj < bestObj){
              // save the best places to insert this customer
              bestI = i;
              bestJ = j;
              bestObj = obj;
            }

            // reset back to the original for next iteration
            r.remove(j);
          }
    }
    // reset back to the original route
    routes.get(curI).add(curJ, customer);

    // generate output list
    List<Double> toRet = new ArrayList<>();
    toRet.add((double)bestI);
    toRet.add((double)bestJ);
    toRet.add(bestObj);

    return toRet;
  }

  // calculate the effect of swapping on every pair of customers
  private Tuple<Double, Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>>> calculateSwaps(
          List<List<Integer>> routes, double currentBest){
    // THIS WORK CAN maybe? BE OPTIMIZED TO DONE BEFOREHAND
    // calculate all pairs of customers that aren't on the same route
    List<Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>>> customerPairs = getAllPairs(routes);

    Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> bestPair = null;
    double bestObj = currentBest;

    // for each pair
    for (Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> pair : customerPairs) {
      // calculate the value gained by swapping
      double obj = checkSwap(routes, bestObj, pair.first().first(), pair.first().second(),
              pair.second().first(), pair.second().second());

      // if it was better than the current strategy
      if (obj > 0) {
        //save these values
        bestObj = obj;
        bestPair = pair;
      }
    }

    if (bestObj == currentBest) {
      return null;
    }
    return new Tuple<>(bestObj, bestPair);
  }

  // calculates all pairs of customer indices, excluding those on the same route
  // It's essentially a list of tuples of coordinate pairs
  private List<Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>>> getAllPairs(List<List<Integer>> routes) {
    List<Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>>> toRet = new ArrayList<>();

    // iterate through all routes
    for (int i = 0; i < routes.size(); i++) {
      for (int j = i + 1; j < routes.size(); j++) {
        List<Integer> list1 = routes.get(i);
        List<Integer> list2 = routes.get(j);
        // account for depots here
        for (int k = 1; k < list1.size() - 1; k++) {
          for (int l = 1; l < list2.size() - 1; l++) {
            toRet.add(new Tuple<>(new Tuple<>(i, k), new Tuple<>(j, l)));
          }
        }
      }
    }

    return toRet;
  }

  private Double checkSwap(List<List<Integer>> routes, double bestObj, int c1i, int c1j, int c2i, int c2j) {
    // in this function, we are calculating the effect of a single swap
    // swap them
    List<Integer> r1 = routes.get(c1i);
    List<Integer> r2 = routes.get(c2i);

    // swap (works cuz mutability)
    swap(r1, r2, c1j, c2j);
    // calculate new objective function
    double obj = calculateObjFunction(routes);
    // swap back
    swap(r1, r2, c1j, c2j);

    // return the best objective
    if (obj < bestObj) {
      swap(r1, r2, c1j, c2j);
      return obj;
    }

    return -1.0;
  }
  private void swap(List<Integer> r1, List<Integer> r2, int j1, int j2){
    int temp = r1.get(j1);
    r1.set(j1, r2.get(j2));
    r2.set(j2, temp);
  }

  private void swap(List<List<Integer>> routes, int i1, int i2, int j1, int j2){
    List<Integer> r1 = routes.get(i1);
    List<Integer> r2 = routes.get(i2);

    int temp = r1.get(j1);
    r1.set(j1, r2.get(j2));
    r2.set(j2, temp);
  }

  private double calculateObjFunction(List<List<Integer>> routes){
    // objecting function: c'(x) = c(x) + Q(x) + D(x)
    //                    route cost + tot excess capacity   tot excess distance
    return getTourLength(routes) + getAmtOverCapacity(routes);
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
