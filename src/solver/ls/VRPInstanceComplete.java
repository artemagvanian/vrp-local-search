package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VRPInstanceComplete extends VRPInstance {

  public VRPInstanceComplete(String fileName) {
    super(fileName);
  }

  // Get subsets of the customers of a given size.
  private static List<List<Integer>> getSubsets(List<Integer> customers, int size) {
    int n = customers.size();
    int[] subset = new int[size];

    List<List<Integer>> subsets = new ArrayList<>();
    getSubsetsHelper(customers, subsets, subset, 0, n - 1, 0, size);

    return subsets;
  }

  // Recursive helper for getting subsets.
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

  // Solve the model.
  public void solve(
      boolean useBppApproximation,
      boolean relaxCapacityConstraints,
      boolean relaxToContinuous,
      int minK,
      int maxK) {
    try (IloCplex cplex = new IloCplex()) {
      // 0. Setup of decision variables.
      // We are indexing until numCustomers + 1 since we have to account for the depot.
      // Decision variables represent the number of times an edge was traversed.
      // NB! this is a triangular matrix (j > i), since the distances are symmetrical.
      IloNumVar[][] nTraversals = new IloNumVar[numCustomers][numCustomers];
      for (int i = 0; i < numCustomers; i++) {
        for (int j = i + 1; j < numCustomers; j++) {
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
      for (int i = 0; i < numCustomers; i++) {
        for (int j = i + 1; j < numCustomers; j++) {
          totalCost.addTerm(distances[i][j], nTraversals[i][j]);
        }
      }
      cplex.addMinimize(totalCost);

      // 2. Each customer is visited exactly once
      for (int i = 1; i < numCustomers; i++) {
        IloLinearNumExpr visitOnce = cplex.linearNumExpr();
        // Add all non-self edges to the calculation (including depot)
        for (int j = 0; j < numCustomers; j++) {
          if (i < j) {
            // travelling = a vehicle goes on that path
            visitOnce.addTerm(1, nTraversals[i][j]);
          } else if (i > j) {
            visitOnce.addTerm(1, nTraversals[j][i]);
          }
        }
        cplex.addEq(visitOnce, 2);
      }

      // 3. There are no more than 2 * `numVehicles` edges adjacent to the depot
      IloLinearNumExpr returnToDepot = cplex.linearNumExpr();
      // Sum of all edges adjacent to the depot
      for (int j = 1; j < numCustomers; j++) {
        returnToDepot.addTerm(1, nTraversals[0][j]);
      }
      // adding the constraint
      cplex.addLe(returnToDepot, 2 * numVehicles);

      // 4. Capacity constraints (could be strengthened via solving the associated BPP).
      if (!relaxCapacityConstraints) {
        // Create an array of all customers.
        List<Integer> customers = new ArrayList<>();
        for (int i = 1; i < numCustomers; i++) {
          customers.add(i);
        }

        // Check all subsets of size minK to maxK
        for (int k = minK; k < maxK + 1; k++) {
          // Get all subsets of customers of size k.
          List<List<Integer>> subsets = getSubsets(customers, k);

          // For each subset of size k.
          for (List<Integer> subset : subsets) {
            // Calculate the lower bound on the number of vehicles needed.
            int minVehiclesNeeded;
            if (useBppApproximation) {
              double q = 0;
              for (int customer : subset) {
                q += demandOfCustomer[customer];
              }
              // Calculate the simplified BPP.
              minVehiclesNeeded = (int) Math.ceil(q / vehicleCapacity);
            } else {
              minVehiclesNeeded = minVehicles(subset);
            }
            // Add the RC constraint.
            IloLinearNumExpr capacityExpr = cplex.linearNumExpr();
            for (int i : subset) { // For each customer selected in the subset.
              for (int j : customers) { // For each customer *not* in the subset.
                if (!subset.contains(j)) {
                  if (i < j) {
                    capacityExpr.addTerm(1, nTraversals[i][j]); // Add this edge to the edge sum.
                  } else if (i > j) {
                    capacityExpr.addTerm(1, nTraversals[j][i]); // Add this edge to the edge sum.
                  }
                }
              }
              capacityExpr.addTerm(1, nTraversals[0][i]); // Add the depot edge to the edge sum.
            }
            cplex.addGe(capacityExpr, 2 * minVehiclesNeeded);
          }
        }
      }

      // Solution.
      if (cplex.solve()) {
        System.out.println("Number of vehicles: " + numVehicles);
        System.out.println("Number of customers: " + numCustomers);
        System.out.println("Objective value: " + cplex.getObjValue());
        int[][] solvedAdjMat = new int[numCustomers][numCustomers];

        System.out.println("Solution adjacency matrix: ");
        double totalSum = 0;
        for (int i = 0; i < numCustomers; i++) {
          for (int j = 0; j < numCustomers; j++) {
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

        System.out.println("Objective check: " + totalSum / 2);

        // Turn the adjacency matrix into routes.
        routes = getWalks(solvedAdjMat);
      } else {
        throw new IllegalArgumentException("Infeasible VRP model.");
      }
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  // Determine how many vehicles are strictly needed to serve a given list of customers.
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
          totalLoad.addTerm(customerVehicleAssignment[j][i],
              demandOfCustomer[customers.get(j)]);
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

  // Helper to turn the adjacency matrix into a list of paths.
  private List<List<Integer>> getWalks(int[][] adjMat) {
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

  // Helper to calculate list of paths for adjacency matrix.
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