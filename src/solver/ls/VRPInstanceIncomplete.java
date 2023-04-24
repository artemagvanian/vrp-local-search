package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;

import java.util.Random;

public class VRPInstanceIncomplete extends VRPInstance {

  VRPInstanceIncomplete(String fileName) {
    super(fileName);
    routes = generateInitialSolution();
  }

  // Interchanges 0 or more customers between routes to improve the current solution.
  // \lambda = 2:
  // Swaps and shifts: (2, 2), (2, 1), (2, 0), (1, 1), (1, 0)
  private void optimizeCustomerInterchange(List<List<Integer>> routes) {
    // 1. randomly pick two routes (i.e. pick two indices into routes)
//    Random rand = new Random();
//
//    int i1 = rand.nextInt(routes.size()); //
//    int i2 = rand.nextInt(routes.size()); // this is different from first bc of nextInt()
//    List<Integer> route1 = routes.get(i1);
//    List<Integer> route2 = routes.get(i2);

    // 1. Generate all pairs of
    List<List<Integer>> customerPairs = new ArrayList<>();
//
//    for (int i = 0; i < routes.size(); i++) {
//      for (int j = 0; j < routes.size(); j++) {
//        List<Integer> pair = new ArrayList<>();
//        pair.add(i);
//        pair.add(j);
//        indexPairs.add(pair);
//      }
//    }

    // 1. for each customer check all place of insertion into other routes
    // as we are doing this, generate a list of pairs

//    for (int i = 0; i <= list1.size(); i++){
//      list1.add(i, customer);
//      list1.remove(i);
//    }


    // 2. Calculate the capacity of the routes we have in a list of size lambda (2)
//    List<Double> curCapacities = new ArrayList<>();
//    curCapacities.add(getCapacity(route1));
//    curCapacities.add(getCapacity(route2));

    // 3. check all combinations of swaps between all the variables in the routes.

    // iterate over all possible combinations of swaps and shifts
//    for (int i = 0; i < route1.size(); i++) {
//      for (int j = 0; j < route2.size(); j++) {
//        // swap two elements in list1
//        List<Integer> newList1 = new ArrayList<>(route1);
//        List<Integer> newList2 = new ArrayList<>(route2);
//        int temp = newList1.get(i);
//        newList1.set(i, newList2.get(j));
//        newList2.set(j, temp);
//      }
//    }

//        // shift an element from list1 to list2
//        List<Integer> newList2 = new ArrayList<>(list2);
//        newList2.add(newList1.get(i));
//        newList1.remove(i);
//
//        // shift an element from list2 to list1
//        List<Integer> newList3 = new ArrayList<>(list1);
//        newList3.add(newList2.get(j));
//        newList2.remove(j);



        //  3.1. to check a combination, that means check capacity on each of the routes and see how much each thing is over
    //  3.2  additionally check the total tour length (optimize this!)

    // 4. save the total tour length + excess capacity

    // 5. select the route that has the lowest cost

    // loop

  }

  // Optimize structure of each route via switching the edges between customers.
  private void optimizeRouteStructure() {

  }

  // find all places where the given customer at route index can be
  private List<Double> checkInsertions(List<List<Integer>> routes, int customer, int curI, int curJ){
    // to avoid any issues
    // mutability stuff here is realllly sketch
    int bestI = curI;
    int bestJ = curJ;
    double bestObj = getTourLength(routes) + getAmtOverCapacity(routes);
    // remove
    routes.get(curI).remove(curJ);

    // iterate over every place in the route
    for (int i = 0; i < routes.size(); i++) {
          List<Integer> r = routes.get(i);
          for (int j = 1; j < r.size(); j++){
            // add this current insertion into this route
            r.add(i, customer);

            // calculate objective function and check it, accounting for infeasibility
            double obj = calculateObjFunction(routes);
            if (obj < bestObj){
              // save the best places to insert this customer
              bestI = i;
              bestJ = j;
              bestObj = obj;
            }

            r.remove(i);
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

  private List<Double> checkSwaps(List<List<Integer>> routes, int customer, int curI, int curJ) {
      // iterate over every pair of customers (save their indices somehow for ease of access?)
      // swap them

      // find the best swap and return it
      return null;
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
