package solver.ls.data;

import static solver.ls.incremental.EdgeDeltaCalculators.edgeDelta;
import static solver.ls.incremental.EdgeDeltaCalculators.performRawInterchange;

import java.util.Arrays;

public class RouteList implements Cloneable {

  private final double[][] distances;
  private final int[] demandOfCustomer;
  private final int vehicleCapacity;
  private final int[] longTermMemory;
  private final int numCustomers;
  public Route[] routes;
  public double length;
  private int excessCapacity;

  public RouteList(Route[] routes, double length, double[][] distances, int[] demandOfCustomer,
      int vehicleCapacity, int[] longTermMemory, int numCustomers,
      int excessCapacity) {
    this.routes = routes;
    this.length = length;
    this.distances = distances;
    this.demandOfCustomer = demandOfCustomer;
    this.vehicleCapacity = vehicleCapacity;
    this.longTermMemory = longTermMemory;
    this.numCustomers = numCustomers;
    this.excessCapacity = excessCapacity;
  }

  @Override
  public String toString() {
    return "{" + "\"routes\": " + Arrays.toString(routes) + ", \"length\": " + length + '}';
  }

  public RouteList clone() {
    RouteList clonedRoutes;
    try {
      clonedRoutes = (RouteList) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    clonedRoutes.routes = new Route[routes.length];

    for (int i = 0; i < routes.length; i++) {
      clonedRoutes.routes[i] = routes[i].clone();
    }

    return clonedRoutes;
  }

  public double objective(Interchange interchange, double excessCapacityPenaltyCoefficient,
      double customerUsePenaltyCoefficient, int currentIteration, boolean print) {
    double customerUsePenalty = 0;

    Route route1 = routes[interchange.routeIdx1];
    Route route2 = routes[interchange.routeIdx2];

    for (Insertion insertion : interchange.insertionList1) {
      customerUsePenalty += longTermMemory[route1.customers[insertion.fromCustomerIdx]];
    }
    for (Insertion insertion : interchange.insertionList2) {
      customerUsePenalty += longTermMemory[route2.customers[insertion.fromCustomerIdx]];
    }

    double newLength = length + edgeDelta(interchange, this, distances);
    double ecPenalty =
        excessCapacityPenaltyCoefficient * excessCapacity(interchange, route1, route2)
            / vehicleCapacity;
    double cuPenalty =
        customerUsePenaltyCoefficient * Math.sqrt(numCustomers) *
            customerUsePenalty / currentIteration;

    if (print) {
      System.out.println(
          "\tObjective = " + newLength + " (length) + "
              + ecPenalty + " (EC penalty) + " + cuPenalty + " (CU penalty)");
    }

    return newLength + ecPenalty + cuPenalty;
  }

  public int excessCapacity(Interchange interchange, Route route1, Route route2) {
    int newCustomerDemandRoute1 = route1.demand;
    int newCustomerDemandRoute2 = route2.demand;

    for (Insertion insertion : interchange.insertionList1) {
      int customer = route1.customers[insertion.fromCustomerIdx];
      newCustomerDemandRoute1 -= demandOfCustomer[customer];
      newCustomerDemandRoute2 += demandOfCustomer[customer];
    }

    for (Insertion insertion : interchange.insertionList2) {
      int customer = route2.customers[insertion.fromCustomerIdx];
      newCustomerDemandRoute2 -= demandOfCustomer[customer];
      newCustomerDemandRoute1 += demandOfCustomer[customer];
    }

    int excessCapacityDelta =
        Math.max(0, newCustomerDemandRoute1 - vehicleCapacity)
            + Math.max(0, newCustomerDemandRoute2 - vehicleCapacity)
            - Math.max(0, route1.demand - vehicleCapacity)
            - Math.max(0, route2.demand - vehicleCapacity);

    return excessCapacity + excessCapacityDelta;
  }

  public void perform(Interchange interchange) {
    length += edgeDelta(interchange, this, distances);
    Route route1 = routes[interchange.routeIdx1];
    Route route2 = routes[interchange.routeIdx2];

    excessCapacity = excessCapacity(interchange, route1, route2);

    // Update customer demands for the routes.
    for (Insertion insertion : interchange.insertionList1) {
      int customer = route1.customers[insertion.fromCustomerIdx];
      route1.demand -= demandOfCustomer[customer];
      route2.demand += demandOfCustomer[customer];
    }

    for (Insertion insertion : interchange.insertionList2) {
      int customer = route2.customers[insertion.fromCustomerIdx];
      route2.demand -= demandOfCustomer[customer];
      route1.demand += demandOfCustomer[customer];
    }

    performRawInterchange(route1, route2, interchange.insertionList1, interchange.insertionList2);
  }
}
