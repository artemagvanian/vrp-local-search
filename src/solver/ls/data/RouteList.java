package solver.ls.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RouteList implements Cloneable {

  private final double[][] distances;
  private final int[] demandOfCustomer;
  private final int vehicleCapacity;
  public List<Route> routes;
  public double length;

  public RouteList(List<Route> routes, double length, double[][] distances, int[] demandOfCustomer,
      int vehicleCapacity) {
    this.routes = routes;
    this.length = length;
    this.distances = distances;
    this.demandOfCustomer = demandOfCustomer;
    this.vehicleCapacity = vehicleCapacity;
  }

  @Override
  public String toString() {
    return "{" + "\"routes\": " + routes + ", \"length\": " + length + '}';
  }

  public RouteList clone() {
    RouteList clonedRoutes;
    try {
      clonedRoutes = (RouteList) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    clonedRoutes.routes = new ArrayList<>();

    for (Route route : routes) {
      clonedRoutes.routes.add(route.clone());
    }

    return clonedRoutes;
  }

  public double calculateEdgeDelta(Interchange interchange) {
    Route route1 = routes.get(interchange.routeIdx1);
    Route route2 = routes.get(interchange.routeIdx2);

    if (interchange.insertionList1.size() == 1 && interchange.insertionList2.size() == 0) {
      Insertion insertion = interchange.insertionList1.get(0);
      // Insertion delta.
      return calculateInsertionDelta(route1, route2, insertion);
    } else if (interchange.insertionList1.size() == 1 && interchange.insertionList2.size() == 1) {
      return calculateSwapDelta(route1, route2, interchange);
    } else {
      throw new IllegalArgumentException("Can only process 1-interchanges.");
    }
  }

  private double calculateSwapDelta(Route route1, Route route2, Interchange interchange) {
    Insertion insertion1 = interchange.insertionList1.get(0);
    Insertion insertion2 = interchange.insertionList2.get(0);

    int customer1 = route1.customers.get(insertion1.fromCustomerIdx);
    int customer1LeftNeighbor = route1.customers.get(insertion1.fromCustomerIdx - 1);
    int customer1RightNeighbor = route1.customers.get(insertion1.fromCustomerIdx + 1);

    int customer2 = route2.customers.get(insertion2.fromCustomerIdx);
    int customer2LeftNeighbor = route2.customers.get(insertion2.fromCustomerIdx - 1);
    int customer2RightNeighbor = route2.customers.get(insertion2.fromCustomerIdx + 1);

    double extractionDelta =
        distances[customer1LeftNeighbor][customer1RightNeighbor]
            - distances[customer1LeftNeighbor][customer1]
            - distances[customer1RightNeighbor][customer1]
            + distances[customer2LeftNeighbor][customer2RightNeighbor]
            - distances[customer2LeftNeighbor][customer2]
            - distances[customer2RightNeighbor][customer2];

    int customer1FutureLeftNeighbor = route2.customers.get(
        insertion1.toCustomerIdx <= insertion2.fromCustomerIdx ? insertion1.toCustomerIdx - 1
            : insertion1.toCustomerIdx);

    int customer1FutureRightNeighbor = route2.customers.get(
        insertion1.toCustomerIdx < insertion2.fromCustomerIdx ? insertion1.toCustomerIdx
            : insertion1.toCustomerIdx + 1);

    int customer2FutureLeftNeighbor = route1.customers.get(
        insertion2.toCustomerIdx <= insertion1.fromCustomerIdx ? insertion2.toCustomerIdx - 1
            : insertion2.toCustomerIdx);

    int customer2FutureRightNeighbor = route1.customers.get(
        insertion2.toCustomerIdx < insertion1.fromCustomerIdx ? insertion2.toCustomerIdx
            : insertion2.toCustomerIdx + 1);

    double insertionDelta =
        -distances[customer1FutureLeftNeighbor][customer1FutureRightNeighbor]
            + distances[customer1FutureLeftNeighbor][customer1]
            + distances[customer1FutureRightNeighbor][customer1]
            - distances[customer2FutureLeftNeighbor][customer2FutureRightNeighbor]
            + distances[customer2FutureLeftNeighbor][customer2]
            + distances[customer2FutureRightNeighbor][customer2];

    return extractionDelta + insertionDelta;
  }

  private double calculateInsertionDelta(Route route1, Route route2, Insertion insertion) {
    int customer = route1.customers.get(insertion.fromCustomerIdx);
    int currentLeftNeighbor = route1.customers.get(insertion.fromCustomerIdx - 1);
    int currentRightNeighbor = route1.customers.get(insertion.fromCustomerIdx + 1);

    int futureLeftNeighbor = route2.customers.get(insertion.toCustomerIdx - 1);
    int futureRightNeighbor = route2.customers.get(insertion.toCustomerIdx);

    double edgePositiveDelta =
        distances[customer][futureLeftNeighbor] + distances[customer][futureRightNeighbor]
            + distances[currentLeftNeighbor][currentRightNeighbor];

    double edgeNegativeDelta =
        distances[customer][currentLeftNeighbor] + distances[customer][currentRightNeighbor]
            + distances[futureLeftNeighbor][futureRightNeighbor];

    return edgePositiveDelta - edgeNegativeDelta;
  }

  public int calculateExcessCapacity(Interchange interchange) {
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

  public void perform(Interchange interchange) {
    length += calculateEdgeDelta(interchange);

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
  }

  private void performRawInterchange(Route route1, Route route2, List<Insertion> insertionList1,
      List<Insertion> insertionList2) {

    List<RemovedCustomer> movedCustomers1 = new ArrayList<>();
    List<RemovedCustomer> movedCustomers2 = new ArrayList<>();

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
