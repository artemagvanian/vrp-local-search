package solver.ls.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RouteList implements Cloneable {

  public List<Route> routes;
  public double length;

  public RouteList(List<Route> routes, double length) {
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
    clonedRoutes.routes = new ArrayList<>();

    for (Route route : routes) {
      clonedRoutes.routes.add(route.clone());
    }

    return clonedRoutes;
  }

  public double calculateEdgeDelta(Interchange interchange, double[][] distances) {
    Route route1 = routes.get(interchange.routeIdx1);
    Route route2 = routes.get(interchange.routeIdx2);

    if (interchange.insertionList1.size() == 1 && interchange.insertionList2.size() == 0) {
      Insertion insertion = interchange.insertionList1.get(0);
      // Insertion delta.
      return getInsertionDelta(distances, route1, route2, insertion);
    } else if (interchange.insertionList1.size() == 1 && interchange.insertionList2.size() == 1) {
      Insertion insertion1 = interchange.insertionList1.get(0);
      Insertion insertion2 = interchange.insertionList2.get(0);
      if (insertion1.fromCustomerIdx == insertion2.toCustomerIdx
          && insertion1.toCustomerIdx == insertion2.fromCustomerIdx) {
        // Perfect swap.
        return getSwapDelta(distances, route1, route2, insertion1, insertion2);
      } else if (insertion1.fromCustomerIdx == insertion2.toCustomerIdx) {
        // Half-swap.
        return getHalfSwapDelta(distances, route1, route2, insertion1, insertion2);
      } else if (insertion1.toCustomerIdx == insertion2.fromCustomerIdx) {
        // Half-swap.
        return getHalfSwapDelta(distances, route2, route1, insertion2, insertion1);
      } else {
        // Two independent insertions.
        return calculateEdgeDelta(
            new Interchange(interchange.routeIdx1, new ArrayList<>(List.of(insertion1)),
                interchange.routeIdx2, new ArrayList<>(List.of())), distances) + calculateEdgeDelta(
            new Interchange(interchange.routeIdx1, new ArrayList<>(List.of(insertion2)),
                interchange.routeIdx2, new ArrayList<>(List.of())), distances);
      }
    } else {
      throw new IllegalArgumentException("Can only process 1-interchanges.");
    }
  }

  private double getInsertionDelta(double[][] distances, Route route1, Route route2,
      Insertion insertion) {
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

  private double getSwapDelta(double[][] distances, Route route1, Route route2,
      Insertion insertion1, Insertion insertion2) {
    int customer1 = route1.customers.get(insertion1.fromCustomerIdx);
    int customer1LeftNeighbor = route1.customers.get(insertion1.fromCustomerIdx - 1);
    int customer1RightNeighbor = route1.customers.get(insertion1.fromCustomerIdx + 1);

    int customer2 = route2.customers.get(insertion2.fromCustomerIdx);
    int customer2LeftNeighbor = route2.customers.get(insertion2.fromCustomerIdx - 1);
    int customer2RightNeighbor = route2.customers.get(insertion2.fromCustomerIdx + 1);

    double edgePositiveDelta =
        distances[customer1][customer2LeftNeighbor] + distances[customer1][customer2RightNeighbor]
            + distances[customer2][customer1LeftNeighbor]
            + distances[customer2][customer1RightNeighbor];

    double edgeNegativeDelta =
        distances[customer1][customer1LeftNeighbor] + distances[customer1][customer1RightNeighbor]
            + distances[customer2][customer2LeftNeighbor]
            + distances[customer2][customer2RightNeighbor];

    return edgePositiveDelta - edgeNegativeDelta;
  }

  private double getHalfSwapDelta(double[][] distances, Route route1, Route route2,
      Insertion insertion1, Insertion insertion2) {
    int customer1 = route1.customers.get(insertion1.fromCustomerIdx);
    int customer1LeftNeighbor = route1.customers.get(insertion1.fromCustomerIdx - 1);
    int customer1RightNeighbor = route1.customers.get(insertion1.fromCustomerIdx + 1);

    int customer2 = route2.customers.get(insertion2.fromCustomerIdx);
    int customer2LeftNeighbor = route2.customers.get(insertion2.fromCustomerIdx - 1);
    int customer2RightNeighbor = route2.customers.get(insertion2.fromCustomerIdx + 1);

    int futureLeftNeighbor = route2.customers.get(insertion1.toCustomerIdx - 1);
    int futureRightNeighbor = route2.customers.get(insertion1.toCustomerIdx);

    double edgePositiveDelta =
        distances[customer2][customer1LeftNeighbor] + distances[customer2][customer1RightNeighbor]
            + distances[customer1][futureLeftNeighbor] + distances[customer1][futureRightNeighbor]
            + distances[customer2LeftNeighbor][customer2RightNeighbor];

    double edgeNegativeDelta = distances[futureLeftNeighbor][futureRightNeighbor]
        + distances[customer1][customer1RightNeighbor]
        + distances[customer1][customer1RightNeighbor];

    return edgePositiveDelta - edgeNegativeDelta;
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
