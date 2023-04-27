package solver.ls;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class RouteList implements Cloneable {

  public LinkedList<Route> routes;
  public double length;

  public RouteList(LinkedList<Route> routes, double length) {
    this.routes = routes;
    this.length = length;
  }

  @Override
  public String toString() {
    return "RouteList{" + "routes=" + routes + ", length=" + length + '}';
  }

  public RouteList clone() {
    RouteList clonedRoutes;
    try {
      clonedRoutes = (RouteList) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    clonedRoutes.routes = new LinkedList<>();

    for (Route route : routes) {
      clonedRoutes.routes.add(route.clone());
    }

    return clonedRoutes;
  }

  public double calculateEdgeDelta(Interchange interchange, double[][] distances) {
    Route clonedRoute1 = routes.get(interchange.routeIdx1).clone();
    Route clonedRoute2 = routes.get(interchange.routeIdx2).clone();

    performRawInterchange(clonedRoute1, clonedRoute2, interchange.insertionList1,
        interchange.insertionList2);
    double newRoute1Length = calculateRouteLength(clonedRoute1, distances);
    double newRoute2Length = calculateRouteLength(clonedRoute2, distances);

    return newRoute1Length - clonedRoute1.length + newRoute2Length - clonedRoute2.length;
  }

  private double calculateRouteLength(Route route, double[][] distances) {
    double routeLength = 0;
    for (int i = 0; i < route.customers.size() - 1; i++) {
      routeLength += distances[route.customers.get(i)][route.customers.get(i + 1)];
    }
    return routeLength;
  }

  public int calculateExcessCapacity(Interchange interchange, int[] demandOfCustomer,
      int vehicleCapacity) {
    Route route1 = routes.get(interchange.routeIdx1);
    Route route2 = routes.get(interchange.routeIdx2);

    int newCustomerDemandRoute1 = route1.demand;
    int newCustomerDemandRoute2 = route2.demand;

    for (Insertion insertion : interchange.insertionList1) {
      newCustomerDemandRoute1 -= demandOfCustomer[route1.customers.get(insertion.fromCustomerIdx)];
      newCustomerDemandRoute2 += demandOfCustomer[route1.customers.get(insertion.fromCustomerIdx)];
    }

    for (Insertion insertion : interchange.insertionList2) {
      newCustomerDemandRoute2 -= demandOfCustomer[route2.customers.get(insertion.fromCustomerIdx)];
      newCustomerDemandRoute1 += demandOfCustomer[route2.customers.get(insertion.fromCustomerIdx)];
    }

    int excessCapacity = 0;
    // for each vehicle
    for (int routeIdx = 0; routeIdx < routes.size(); routeIdx++) {
      int currentCustomerDemand;
      if (routeIdx == interchange.routeIdx1) {
        currentCustomerDemand = newCustomerDemandRoute1;
      } else if (routeIdx == interchange.routeIdx2) {
        currentCustomerDemand = newCustomerDemandRoute2;
      } else {
        currentCustomerDemand = routes.get(routeIdx).demand;
      }
      // only if its over what it should be, add amount over
      if (vehicleCapacity < currentCustomerDemand) {
        excessCapacity += currentCustomerDemand - vehicleCapacity;
      }
    }

    return excessCapacity;
  }

  public void perform(Interchange interchange, double[][] distances, int[] demandOfCustomer) {
    length += calculateEdgeDelta(interchange, distances);

    Route route1 = routes.get(interchange.routeIdx1);
    Route route2 = routes.get(interchange.routeIdx2);

    // Update customer demands for the routes.
    for (Insertion insertion : interchange.insertionList1) {
      route1.demand -= demandOfCustomer[route1.customers.get(insertion.fromCustomerIdx)];
      route2.demand += demandOfCustomer[route1.customers.get(insertion.fromCustomerIdx)];
    }

    for (Insertion insertion : interchange.insertionList2) {
      route2.demand -= demandOfCustomer[route2.customers.get(insertion.fromCustomerIdx)];
      route1.demand += demandOfCustomer[route2.customers.get(insertion.fromCustomerIdx)];
    }

    performRawInterchange(route1, route2, interchange.insertionList1, interchange.insertionList2);

    route1.length = calculateRouteLength(route1, distances);
    route2.length = calculateRouteLength(route2, distances);
  }

  private void performRawInterchange(Route route1, Route route2, List<Insertion> insertionList1,
      List<Insertion> insertionList2) {

    LinkedList<RemovedCustomer> movedCustomers1 = new LinkedList<>();
    LinkedList<RemovedCustomer> movedCustomers2 = new LinkedList<>();

    // Sort in the decreasing order by fromCustomerIdx.
    insertionList1.sort((ins1, ins2) -> ins2.fromCustomerIdx - ins1.fromCustomerIdx);
    for (Insertion insertion : insertionList1) {
      movedCustomers1.add(
          new RemovedCustomer(route1.customers.remove(insertion.fromCustomerIdx), insertion));
    }

    // Sort in the decreasing order by fromCustomerIdx.
    insertionList2.sort((ins1, ins2) -> ins2.fromCustomerIdx - ins1.fromCustomerIdx);
    for (Insertion insertion : insertionList2) {
      movedCustomers2.add(
          new RemovedCustomer(route2.customers.remove(insertion.fromCustomerIdx), insertion));
    }

    // Sort in the increasing order by toCustomerIdx.
    movedCustomers1.sort(Comparator.comparingInt(ins -> ins.insertion.toCustomerIdx));
    for (RemovedCustomer removedCustomer : movedCustomers1) {
      route2.customers.add(removedCustomer.insertion.toCustomerIdx, removedCustomer.customer);
    }

    // Sort in the increasing order by toCustomerIdx.
    movedCustomers2.sort(Comparator.comparingInt(ins -> ins.insertion.toCustomerIdx));
    for (RemovedCustomer removedCustomer : movedCustomers2) {
      route1.customers.add(removedCustomer.insertion.toCustomerIdx, removedCustomer.customer);
    }

  }
}
