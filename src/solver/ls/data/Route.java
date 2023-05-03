package solver.ls.data;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Param;
import java.util.ArrayList;
import java.util.List;

public class Route implements Cloneable {

  public List<Integer> customers;
  public int demand;

  public Route(List<Integer> customers, int demand) {
    this.customers = customers;
    this.demand = demand;
  }

  @Override
  public String toString() {
    return "Route{" + "customers=" + customers + ", demand=" + demand + '}';
  }

  public Route clone() {
    Route newRoute;
    try {
      newRoute = (Route) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    newRoute.customers = new ArrayList<>(customers);
    return newRoute;
  }

  public double calculateRouteLength(double[][] distances) {
    double routeLength = 0;
    for (int i = 0; i < customers.size() - 1; i++) {
      routeLength += distances[customers.get(i)][customers.get(i + 1)];
    }
    return routeLength;
  }

  public double optimize(double[][] distances) {
    try (IloCplex tspModel = new IloCplex()) {
      /*
      tspModel.setOut(null);
      tspModel.setWarning(null);
       */
      tspModel.setParam(Param.TimeLimit, 5);

      int numCustomers = customers.size() - 1;

      IloNumVar[][] customerConnections = new IloNumVar[numCustomers][numCustomers];
      IloNumVar[] order = new IloNumVar[numCustomers - 1];

      for (int i = 0; i < numCustomers; i++) {
        for (int j = 0; j < numCustomers; j++) {
          customerConnections[i][j] = tspModel.intVar(0, 1);
        }
      }

      for (int i = 0; i < numCustomers - 1; i++) {
        order[i] = tspModel.intVar(1, numCustomers - 1);
      }

      for (int i = 0; i < numCustomers - 1; i++) {
        for (int j = 0; j < numCustomers - 1; j++) {
          if (i != j) {
            IloLinearNumExpr orderConstraint = tspModel.linearNumExpr();
            orderConstraint.addTerm(1, order[i]);
            orderConstraint.addTerm(-1, order[j]);
            orderConstraint.addTerm(numCustomers - 1, customerConnections[i + 1][j + 1]);
            tspModel.addLe(orderConstraint, numCustomers - 2);
          }
        }
      }

      for (int i = 0; i < numCustomers; i++) {
        IloLinearNumExpr visitOnce = tspModel.linearNumExpr();
        for (int j = 0; j < numCustomers; j++) {
          if (i != j) {
            visitOnce.addTerm(1, customerConnections[i][j]);
          }
        }
        tspModel.addEq(visitOnce, 1);
      }

      for (int i = 0; i < numCustomers; i++) {
        IloLinearNumExpr visitOnce = tspModel.linearNumExpr();
        for (int j = 0; j < numCustomers; j++) {
          if (i != j) {
            visitOnce.addTerm(1, customerConnections[j][i]);
          }
        }
        tspModel.addEq(visitOnce, 1);
      }

      IloLinearNumExpr totalCost = tspModel.linearNumExpr();
      for (int i = 0; i < numCustomers; i++) {
        for (int j = 0; j < numCustomers; j++) {
          if (i != j) {
            totalCost.addTerm(distances[customers.get(i)][customers.get(j)],
                customerConnections[i][j]);
          }
        }
      }
      tspModel.addMinimize(totalCost);

      if (tspModel.solve()) {
        List<Integer> newRoute = new ArrayList<>();
        double newRouteLength = 0;
        int nextCustomer = 0;

        do {
          for (int i = 0; i < numCustomers; i++) {
            int isNextCustomer;
            if (i != nextCustomer) {
              isNextCustomer = (int) Math.round(
                  tspModel.getValue(customerConnections[i][nextCustomer]));
            } else {
              continue;
            }

            if (isNextCustomer == 1) {
              newRoute.add(customers.get(nextCustomer));
              newRouteLength += distances[customers.get(nextCustomer)][customers.get(i)];
              nextCustomer = i;
              break;
            }
          }

        } while (nextCustomer != 0);
        newRoute.add(0);
        newRouteLength += distances[0][newRoute.get(newRoute.size() - 1)];

        double oldRouteLength = calculateRouteLength(distances);
        if (newRouteLength < oldRouteLength) {
          customers = newRoute;
          return newRouteLength - oldRouteLength;
        } else {
          return 0;
        }
      } else {
        throw new IllegalArgumentException("Infeasible TSP model.");
      }

    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }
}
