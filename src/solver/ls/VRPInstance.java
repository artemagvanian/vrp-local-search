package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class VRPInstance {

  protected IloCplex cplex;         // IBM Ilog Cplex Solver
  // VRP Input Parameters
  int numCustomers;                 // the number of customers
  int numVehicles;                  // the number of vehicles
  int vehicleCapacity;              // the capacity of the vehicles
  int[] demandOfCustomer;           // the demand of each customer
  double[] xCoordOfCustomer;        // the x coordinate of each customer
  double[] yCoordOfCustomer;        // the y coordinate of each customer
  double[][] distances;             // distances between all customers

  String solutionString;

  double xCoordOfDepot;
  double yCoordOfDepot;


  public VRPInstance(String fileName) {
    Scanner read = null;
    try {
      read = new Scanner(new File(fileName));
    } catch (FileNotFoundException e) {
      System.out.println("Error: in VRPInstance() " + fileName + "\n" + e.getMessage());
      System.exit(-1);
    }

    numCustomers = read.nextInt() - 1; // normalizing for our dumb model
    numVehicles = read.nextInt();
    vehicleCapacity = read.nextInt();

    System.out.println("Number of customers: " + numCustomers);
    System.out.println("Number of vehicles: " + numVehicles);
    System.out.println("Vehicle capacity: " + vehicleCapacity);

    demandOfCustomer = new int[numCustomers];
    xCoordOfCustomer = new double[numCustomers];
    yCoordOfCustomer = new double[numCustomers];

    double demandOfDepot = read.nextInt();
    double xCoordOfDepot = read.nextDouble();
    double yCoordOfDepot = read.nextDouble();

    // depot
    for (int i = 0; i < numCustomers; i++) {
      demandOfCustomer[i] = read.nextInt();
      xCoordOfCustomer[i] = read.nextDouble();
      yCoordOfCustomer[i] = read.nextDouble();
    }

    for (int i = 0; i < numCustomers; i++) {
      System.out.println(
              demandOfCustomer[i] + " " + xCoordOfCustomer[i] + " " + yCoordOfCustomer[i]);
    }

    distances = precalculateDistances();
  }

  private static List<List<Integer>> getSubsets(List<Integer> customers, int size) {
    int n = customers.size();
    int[] subset = new int[size];

    List<List<Integer>> subsets = new ArrayList<>();
    getSubsetsHelper(customers, subsets, subset, 0, n - 1, 0, size);

    return subsets;
  }

  private static void getSubsetsHelper(List<Integer> customers, List<List<Integer>> subsets,
                                       int[] subset, int start, int end, int index, int k) {
    if (index == k) {
      List<Integer> subsetList = new ArrayList<>();
      for (int i = 0; i < k; i++) {
        subsetList.add(subset[i]);
      }
      subsets.add(subsetList);
      return;
    }

    for (int i = start; i <= end && end - i + 1 >= k - index; i++) {
      subset[index] = customers.get(i);
      getSubsetsHelper(customers, subsets, subset, i + 1, end, index + 1, k);
    }
  }

  private static double distance(double x1, double x2, double y1, double y2) {
    return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
  }

  public double solve(
          boolean useBppApproximation,
          boolean relaxCapacityConstraints,
          boolean relaxToContinuous) {
    try {
      cplex = new IloCplex();

      // 0. Setup of decision variables.
      // We are indexing until numCustomers + 1 since we have to account for the depot.
      // Decision variables represent the number of times an edge was traversed.
      // NB! this is a triangular matrix (j > i), since the distances are symmetrical.
      IloNumVar[][] nTraversals = new IloNumVar[numCustomers + 1][numCustomers + 1];
      for (int i = 0; i < numCustomers + 1; i++) {
        for (int j = i + 1; j < numCustomers + 1; j++) {
          if (i == 0) {
            // Edges adjacent to the depot could be traversed no more than twice.
            nTraversals[i][j] = cplex.numVar(0, 2,
                    relaxToContinuous ? IloNumVarType.Float : IloNumVarType.Int);
          } else {
            // Edges not adjacent to the depot could be traversed no more than once.
            nTraversals[i][j] = cplex.numVar(0, 1,
                    relaxToContinuous ? IloNumVarType.Float : IloNumVarType.Int);
          }
        }
      }

      // 1. Minimize distance
      IloLinearNumExpr totalCost = cplex.linearNumExpr();
      for (int i = 0; i < numCustomers + 1; i++) {
        for (int j = i + 1; j < numCustomers + 1; j++) {
          totalCost.addTerm(distances[i][j], nTraversals[i][j]);
        }
      }
      cplex.addMinimize(totalCost);

      // 2. Each customer is visited exactly once
      for (int i = 1; i < numCustomers + 1; i++) {
        IloLinearNumExpr visitOnce = cplex.linearNumExpr();
        // Add all non-self edges to the calculation
        for (int j = 0; j < numCustomers + 1; j++) {
          if (i < j) {
            // travelling = a vehicle goes on that path
            visitOnce.addTerm(1, nTraversals[i][j]);
          } else if (i > j) {
            visitOnce.addTerm(1, nTraversals[j][i]);
          }
        }
        cplex.addEq(visitOnce, 2);
      }

      // 3. There are no more than `numVehicles` outgoing edges from the depot
      IloLinearNumExpr returnToDepot = cplex.linearNumExpr();
      // sum of all outgoing edges from the depot
      for (int j = 1; j < numCustomers + 1; j++) {
        returnToDepot.addTerm(1, nTraversals[0][j]);
      }
      // adding the constraint
      cplex.addLe(returnToDepot, 2 * numVehicles);

      // 4. Rounded Capacity (RC) constraints (could be strengthened via solving the associated BPP).
      if (!relaxCapacityConstraints) {
        // Create an array of all customers.
        List<Integer> customers = new ArrayList<>();
        for (int i = 1; i < numCustomers + 1; i++) {
          customers.add(i);
        }

        // Check all subsets of size 1 to numCustomers + 1.
        for (int k = 1; k < numCustomers + 1; k++) {
          // Get all subsets of customers of size k.
          List<List<Integer>> subsets = getSubsets(customers, k);

          // For each subset of size k.
          for (List<Integer> subset : subsets) {
            // Calculate the lower bound on the number of vehicles needed.
            int minVehiclesNeeded;
            if (useBppApproximation) {
              double q = 0;
              for (int customer : subset) {
                q += demandOfCustomer[customer - 1];
              }
              // Calculate the simplified BPP.
              minVehiclesNeeded = (int) Math.ceil(q / vehicleCapacity);
            } else {
              minVehiclesNeeded = minVehicles(subset);
            }
            // Add the RC constraint.
            IloLinearNumExpr rcExpr = cplex.linearNumExpr();
            for (int i : subset) { // For each customer selected in the subset.
              for (int j : customers) { // For each customer *not* in the subset.
                if (!subset.contains(j)) {
                  if (i < j) {
                    rcExpr.addTerm(1, nTraversals[i][j]); // Add this edge to the edge sum.
                  } else if (i > j) {
                    rcExpr.addTerm(1, nTraversals[j][i]); // Add this edge to the edge sum.
                  }
                }
              }
              rcExpr.addTerm(1, nTraversals[0][i]); // Add the depot edge to the edge sum.
            }
            cplex.addGe(rcExpr, 2 * minVehiclesNeeded);
          }
        }
      }

      // Solution.
      if (cplex.solve()) {
        System.out.println("Num Vehicles: " + numVehicles);
        System.out.println("Num Customers: " + numCustomers);
        System.out.println("Objective Value: " + cplex.getObjValue());
        int[][] solvedAdjMat = new int[numCustomers + 1][numCustomers + 1];

        double totalSum = 0;
        for (int i = 0; i < numCustomers + 1; i++) {
          for (int j = 0; j < numCustomers + 1; j++) {
            if (i < j) {
              int indicator = (int) Math.round(cplex.getValue(nTraversals[i][j]));
              solvedAdjMat[i][j] = indicator;
              System.out.print(indicator + ", ");
              totalSum += indicator * distances[i][j];
            } else if (i > j) {
              int indicator = (int) Math.round(cplex.getValue(nTraversals[j][i]));
              solvedAdjMat[i][j] = indicator;
              System.out.print(indicator + ", ");
              totalSum += indicator * distances[j][i];
            } else {
              System.out.print(0 + ", ");
            }
          }
          System.out.println();
        }
        System.out.println("Objective Check: " + totalSum / 2);

        //  for (int i = 0; i < numCustomers + 1; i++) {
        //    for (int j = 0; j < numCustomers + 1; j++) {
        //      System.out.print((int) distances[i][j] + " ");
        //    }
        //    System.out.println();
        //  }

        // Turn the adjacency matrix into a CS2951O solution
        List<List<Integer>> walks = getWalks(solvedAdjMat);
        // add the vehicles that didn't go
        int excessVehicles = numVehicles - walks.size();
        for (int i = 0; i < excessVehicles; i++){
          ArrayList<Integer> excess = new ArrayList<>();
          excess.add(0);
          excess.add(0);
          walks.add(excess);
        }

//        System.out.println("Paths: " + walks.size());
//        for (List<Integer> walk : walks) {
//          for (int j : walk) {
//            System.out.print(j + " ");
//          }
//          System.out.println();
//        }

        // convert to a string
        List<Integer> flattenedList = new ArrayList<>();
        flattenedList.add(1); // NOTE: 1 HERE IF PROVED OPTIMAL, ELSE 0
        for (List<Integer> innerList : walks) {
          flattenedList.addAll(innerList);
        }
        StringBuilder sb = new StringBuilder();
        for (Integer number : flattenedList) {
          sb.append(number).append(" ");
        }
        solutionString = sb.toString().trim();
        return cplex.getObjValue();
      } else {
        throw new IllegalArgumentException("Infeasible VRP model.");
      }
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  private double[][] precalculateDistances() {
    double[][] distances = new double[numCustomers + 1][numCustomers + 1];

    // Calculate distances for depot.
    for (int j = 1; j < numCustomers + 1; j++) {
      double d = distance(xCoordOfCustomer[j - 1], xCoordOfDepot, yCoordOfCustomer[j - 1], yCoordOfDepot);
      distances[0][j] = d;
      distances[j][0] = d;
    }

    // Calculate distances for everything else (inefficiently).
    for (int i = 1; i < numCustomers + 1; i++) {
      for (int j = 1; j < numCustomers + 1; j++) {
        distances[i][j] = distance(xCoordOfCustomer[i - 1], xCoordOfCustomer[j - 1],
                yCoordOfCustomer[i - 1], yCoordOfCustomer[j - 1]);
      }
    }

    // Sanity check
//      System.out.println("****");
//      System.out.println(xCoordOfDepot);
//      System.out.println(yCoordOfDepot);
//      for (int i = 0; i < numCustomers + 1; i++) {
//        for (int j = 0; j < numCustomers + 1; j++) {
//          System.out.print((int)distances[i][j] + " ");
//        }
//        System.out.println();
//      }
//      System.out.println("****");

    return distances;
  }

  private int minVehicles(List<Integer> customers) {
    try (IloCplex bppModel = new IloCplex()) {
      bppModel.setOut(null);
      bppModel.setWarning(null);

      IloNumVar[] useVehicles = bppModel.boolVarArray(numVehicles);
      IloNumVar[][] customerVehicleAssignment = new IloNumVar[customers.size()][numVehicles];

      for (int i = 0; i < customers.size(); i++) {
        for (int j = 0; j < numVehicles; j++) {
          customerVehicleAssignment[i][j] = bppModel.boolVar();
        }
      }

      // Minimize number of vehicles (bins) used
      IloLinearNumExpr totalVehicles = bppModel.linearNumExpr();
      for (int i = 0; i < numVehicles; i++) {
        totalVehicles.addTerm(1, useVehicles[i]);
      }
      bppModel.addMinimize(totalVehicles);

      // Enforce each customer (item) being assigned to only one vehicle (bin)
      for (int i = 0; i < customers.size(); i++) {
        IloLinearNumExpr totalAssignments = bppModel.linearNumExpr();
        for (int j = 0; j < numVehicles; j++) {
          totalAssignments.addTerm(1, customerVehicleAssignment[i][j]);
        }
        bppModel.addEq(totalAssignments, 1);
      }

      // Enforce capacity constraints
      for (int i = 0; i < numVehicles; i++) {
        IloLinearNumExpr totalLoad = bppModel.linearNumExpr();
        for (int j = 0; j < customers.size(); j++) {
          // Note: This doesn't enforce customer capacity:
          // totalLoad.addTerm(customerVehicleAssignment[j][i], customers.get(j));
          totalLoad.addTerm(customerVehicleAssignment[j][i], demandOfCustomer[j]);

        }
        IloLinearNumExpr maxLoad = bppModel.linearNumExpr();
        maxLoad.addTerm(vehicleCapacity, useVehicles[i]);
        bppModel.addLe(totalLoad, maxLoad);
      }

      if (bppModel.solve()) {
        return (int) Math.round(bppModel.getObjValue());
      } else {
        throw new IllegalArgumentException("Infeasible BPP model.");
      }
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  // helper to turn the adjacency matrix into a list of paths
  public List<List<Integer>> getWalks(int[][] adjMat) {
    List<List<Integer>> allCircuits = new ArrayList<>();
    boolean[] visited = new boolean[adjMat.length];
    List<Integer> currentCircuit = new ArrayList<>();

    // recursively find all paths connected to the start node
    dfs(adjMat, 0, 0, visited, currentCircuit, allCircuits, new HashSet<>());
    for (List<Integer> l : allCircuits) {
      l.add(0);
    }
    return allCircuits;
  }

  // helper to calculate list of paths for adjacency matrix
  private void dfs(int[][] adjacencyMatrix, int startNode, int currentNode,
                    boolean[] visited, List<Integer> currentCircuit,
                    List<List<Integer>> allCircuits, Set<Integer> seen) {
    visited[currentNode] = true;
    currentCircuit.add(currentNode);

    for (int neighbor = 0; neighbor < adjacencyMatrix[currentNode].length; neighbor++) {
      if (adjacencyMatrix[currentNode][neighbor] == 1 && !seen.contains(neighbor)) {
        if (neighbor == startNode && currentCircuit.size() > 2) {
          allCircuits.add(new ArrayList<>(currentCircuit));
        } else if (!visited[neighbor]) {
          seen.add(neighbor);
          dfs(adjacencyMatrix, startNode, neighbor, visited, currentCircuit, allCircuits, seen);
        }
      }
    }

    visited[currentNode] = false;
    currentCircuit.remove(currentCircuit.size() - 1);
  }
}