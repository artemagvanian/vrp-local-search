package solver.ls;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.Scanner;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cp.*;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Algorithm;
import ilog.cplex.IloCplex.Param;

public class VRPInstance {
  // VRP Input Parameters
    int numCustomers;        		// the number of customers
    int numVehicles;           	// the number of vehicles
    int vehicleCapacity;			// the capacity of the vehicles
    int[] demandOfCustomer;		// the demand of each customer
    double[] xCoordOfCustomer;	// the x coordinate of each customer
    double[] yCoordOfCustomer;	// the y coordinate of each customer
    protected IloCplex cplex;       // IBM Ilog Cplex Solver

    IloCP cp;       // IBM Ilo CPOptimizer

    double[][] distances;       // distances between all customers


    public VRPInstance(String fileName) {
        Scanner read = null;
        try
        {
          read = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
          System.out.println("Error: in VRPInstance() " + fileName + "\n" + e.getMessage());
          System.exit(-1);
    }

        numCustomers = read.nextInt();
        numVehicles = read.nextInt();
        vehicleCapacity = read.nextInt();

        System.out.println("Number of customers: " + numCustomers);
        System.out.println("Number of vehicles: " + numVehicles);
        System.out.println("Vehicle capacity: " + vehicleCapacity);

        demandOfCustomer = new int[numCustomers];
        xCoordOfCustomer = new double[numCustomers];
        yCoordOfCustomer = new double[numCustomers];

        for (int i = 0; i < numCustomers; i++)
        {
            demandOfCustomer[i] = read.nextInt();
            xCoordOfCustomer[i] = read.nextDouble();
            yCoordOfCustomer[i] = read.nextDouble();
        }

        for (int i = 0; i < numCustomers; i++) {
            System.out.println(demandOfCustomer[i] + " " + xCoordOfCustomer[i] + " " + yCoordOfCustomer[i]);
        }
        distances = precalculateGraph();
    }

    public void completeModel(){
      try {
          cplex = new IloCplex();
          cp = new IloCP();

          // 0. Setup of decision variables
          // numCustomers + 1 (depot/home base)
          // number of times an edge was traversed
          IloNumVar[][] numTraversals = new IloNumVar[numCustomers+1][];
          for(int i = 0; i < numCustomers+1; i++) {
              numTraversals[i] = cplex.numVarArray(numCustomers+1, 0, 2, IloNumVarType.Int);
          }

          // incident edges:
          // Create 2D array of variables for edges between nodes
//          IloNumVar[][] incidentEdges = new IloNumVar[numCustomers+1][];
//          for (int i = 0; i < numCustomers+1; i++) {
//              incidentEdges[i] = cplex.numVarArray(numCustomers+1, 0, 1, IloNumVarType.Int);
//          }

//          for (int i = 1; i < numCustomers+1; i++) {
//              for (int j = 1; j < numCustomers+1; j++) {
//                  cplex.add(cplex.ifThen()
//              }
//          }

          // 1. Minimize distance
          // TODO: why is objective value 0 here?
          IloLinearNumExpr totalCost = cplex.linearNumExpr();
          for (int i = 0; i < numCustomers+1; i++) {
              for(int j = 0; j < numCustomers+1; j++){
                  totalCost.addTerm(distances[i][j], numTraversals[i][j]);
              }
          }
          cplex.addMinimize(totalCost);

          // 2. Each customer is visited exactly once
          for (int i = 1; i < numCustomers+1; i++) {
              IloLinearNumExpr visitOnce = cplex.linearNumExpr();
              // ingoing edges (add them)
              for (int j = 1; j < numCustomers+1; j++) {
                  if (i != j) {
                      // travelling = a vehicle goes on that path
                      // for a customer i, we add all the times any customer j travels to i
                      visitOnce.addTerm(1.0, numTraversals[i][j]);
                      // for the same customer i, we add all the times they travel somewhere else
                      visitOnce.addTerm(1.0, numTraversals[j][i]);
                  }
              }
              // outgoing edges
//              for (int k = 0; k <= numCustomers; k++) {
//                  if (i != k) {
//                      visitOnce.addTerm(1.0, numTraversals[k][i]);
//                  }
//              }
              cplex.addEq(visitOnce, 2.0);
          }
//          for (int i = 1; i < numCustomers + 1; i++) {
//              IloLinearNumExpr edgeSum = cplex.linearNumExpr();
//              // if an edge is travelled, its incident
//              for (int j = 1; j < numCustomers + 1; j++) {
//                  // this is 1 or 0
//                  if (i != j) {
//                      edgeSum.addTerm(1, numTraversals[i][j]);
//                  }
//              }
//              // sum of incident edges for this customer must be 2
//              cplex.addEq(edgeSum, 2);
//          }

          // 3. Each customer is visited exactly once
//          IloLinearNumExpr sumEdges = cplex.linearNumExpr();
//          for (int i = 1; i < numCustomers + 1; i++) {
//              sumEdges.addTerm(numTraversals[0][i], 1);
//              sumEdges.addTerm(numTraversals[i][0], 1);
//              // sum of incident edges for this customer must be 2
//              cplex.addEq(sumEdges, 2 * numVehicles);
//          }


//          for (int i = 1; i < numCustomers+1; i++) {
//              IloLinearNumExpr customerVisit = cplex.linearNumExpr();
//              // across each column, a customer must only have 1 value filled in -> sum across column = 1
//              for (int j = 0; j < numCustomers+1; j++) {
//                  if (i != j) {
//                      customerVisit.addTerm(1, numTraversals[i][j]);
//                  }
//              }
//              cplex.addEq(customerVisit, 1);
//          }

          // 3. Each route starts and ends at the depot
          IloLinearNumExpr sumEdges = cplex.linearNumExpr();
          for (int i = 1; i <= numCustomers; i++) {
              sumEdges.addTerm(numTraversals[0][i], 1);
              sumEdges.addTerm(numTraversals[i][0], 1);
          }
          cplex.addEq(sumEdges, 2 * numVehicles);

          // 4.
          // 4. Capacity constraints
          // Define capacity constraints (4)
//          for (int s = 1; s <= numCustomers; s++) {
//              IloLinearNumExpr demand = cplex.linearNumExpr();
//              for (int t = 0; t <= numCustomers; t++) {
//                  if (s-1 != t) {
//                      demand.addTerm(demandOfCustomer[s-1], numTraversals[s-1][t]);
//                  }
//              }
//              cplex.addLe(demand, vehicleCapacity);
//          }

          // 4. Capacity constraints
//          for (int subset = 1; subset < numCustomers; subset++) {
//              for (int i = 1; i <= numCustomers; i++) {
//                  if (i == subset) {
//                      continue;
//                  }
//                  IloLinearNumExpr edgesEnteringSubset = cplex.linearNumExpr();
//                  for (int j = 0; j <= numCustomers; j++) {
//                      if (j != i && j != subset) {
//                          edgesEnteringSubset.addTerm(1.0, numTraversals[j][i]);
//                      }
//                  }
//
//                  int binCapacity = (int) Math.ceil(cp.pack(demandOfCustomer, vehicleCapacity, ));
//                  cplex.addLe(edgesEnteringSubset, 2 * binCapacity);
//              }
//          }

          //  4. Capacity constraints
//          int Q = vehicleCapacity;
//          for (int S = 1; S < numCustomers+1; S++) {
//              IloLinearNumExpr capacityExpr = cplex.linearNumExpr();
//              for (int i = 0; i < numCustomers+1; i++) {
//                  if (i != S) {
//                      for (int j = 0; j < numCustomers+1; j++) {
//                          if (j != S && j != i) {
//                              capacityExpr.addTerm(1, numTraversals[i][j]);
//                          }
//                      }
//                  }
//              }
//              int[] items = new int[numCustomers];
//              for (int i = 0; i < numCustomers; i++) {
//                  items[i] = i+1;
//              }
//              IloNumExpr packExpr = cp.pack(cp.intVarArray(numCustomers, 0, Q), cp.intVarArray(Q), items, numTraversals[S]);
//              cplex.addGe(capacityExpr, Math.ceil(cplex.getValue(packExpr) / Q));
//          }

          // 5. Each edge between two customers is traversed at most once
          for (int i = 1; i <= numCustomers; i++) {
              for (int j = 1; j <= numCustomers; j++) {
                  if (i != j) {
                      IloLinearNumExpr atMostOnce = cplex.linearNumExpr();
                      atMostOnce.addTerm(1, numTraversals[i][j]);
                      atMostOnce.addTerm(1, numTraversals[j][i]);
                      cplex.addLe(atMostOnce, 1);
                  }
              }
          }

          // 6. Each edge incident to the depot is traversed at most twice
          for (int i = 1; i <= numCustomers; i++) {
              IloLinearNumExpr atMostTwice = cplex.linearNumExpr();
              atMostTwice.addTerm(1, numTraversals[0][i]);
              atMostTwice.addTerm(1, numTraversals[i][0]);
              cplex.addLe(atMostTwice, 2);
          }

          // solution
          if (cplex.solve()) {
              System.out.println("Num Vehicles: " + numVehicles);
              System.out.println("Objective Value: " + cplex.getObjValue());
              for (int i = 0; i < numCustomers+1; i++) {
                  for(int j = 0; j < numCustomers+1; j++){
                      System.out.print(cplex.getValue(numTraversals[i][j]) + " ");
                  }
                  System.out.println(" next ");
              }
          }
      } catch (IloException e) {
          throw new RuntimeException(e);
      }
    }

    public double[][] precalculateGraph(){
        double[][] graph = new double[numCustomers+1][numCustomers+1];

        // calculate for depot
        for (int j = 0; j < numCustomers; j++) {
            graph[0][j] = calcDist(xCoordOfCustomer[j], 0, yCoordOfCustomer[j],0);
        }

        // calculate for everything else
        for (int i = 1; i < numCustomers+1; i++){
            for (int j = 1; j < numCustomers+1; j++){
                graph[i][j] = calcDist(xCoordOfCustomer[i-1], xCoordOfCustomer[j-1],
                        yCoordOfCustomer[i-1], yCoordOfCustomer[j-1]);
            }
        }

        // sanity check
        for (int i = 1; i < numCustomers+1; i++){
            for (int j = 1; j < numCustomers+1; j++){
                System.out.print(graph[i][j] + " ");
            }
            System.out.println(" next ");
        }

        return graph;
    }

    public double calcDist(double x1, double x2, double y1, double y2){
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }


}
