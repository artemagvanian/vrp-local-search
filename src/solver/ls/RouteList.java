package solver.ls;

import java.util.ArrayList;
import java.util.List;

public class RouteList implements Cloneable {

  public List<Route> routes;
  public double totalLength;

  public RouteList(List<Route> routes, double totalLength) {
    this.routes = routes;
    this.totalLength = totalLength;
  }

  @Override
  public String toString() {
    return "RouteList{" + "routes=" + routes + ", totalLength=" + totalLength + '}';
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

  public double calculateEdgeDelta(OneInterchange interchange, double[][] distances) {
    if (interchange.type == InterchangeType.Swap) {
      int customer1 = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx);
      int customer1LeftNeighbor = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx - 1);
      int customer1RightNeighbor = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx + 1);

      int customer2 = routes.get(interchange.toRouteIdx).customers.get(interchange.toCustomerIdx);
      int customer2LeftNeighbor = routes.get(interchange.toRouteIdx).customers.get(
          interchange.toCustomerIdx - 1);
      int customer2RightNeighbor = routes.get(interchange.toRouteIdx).customers.get(
          interchange.toCustomerIdx + 1);

      double edgePositiveDelta =
          distances[customer1][customer2LeftNeighbor] + distances[customer1][customer2RightNeighbor]
              + distances[customer2][customer1LeftNeighbor]
              + distances[customer2][customer1RightNeighbor];

      double edgeNegativeDelta =
          distances[customer1][customer1LeftNeighbor] + distances[customer1][customer1RightNeighbor]
              + distances[customer2][customer2LeftNeighbor]
              + distances[customer2][customer2RightNeighbor];

      return edgePositiveDelta - edgeNegativeDelta;
    } else if (interchange.type == InterchangeType.Insertion) {
      int customer = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx);
      int currentLeftNeighbor = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx - 1);
      int currentRightNeighbor = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx + 1);

      int futureLeftNeighbor = routes.get(interchange.toRouteIdx).customers.get(
          interchange.toCustomerIdx - 1);
      int futureRightNeighbor = routes.get(interchange.toRouteIdx).customers.get(
          interchange.toCustomerIdx);

      double edgePositiveDelta =
          distances[customer][futureLeftNeighbor] + distances[customer][futureRightNeighbor]
              + distances[currentLeftNeighbor][currentRightNeighbor];

      double edgeNegativeDelta =
          distances[customer][currentLeftNeighbor] + distances[customer][currentRightNeighbor]
              + distances[futureLeftNeighbor][futureRightNeighbor];

      return edgePositiveDelta - edgeNegativeDelta;
    }
    throw new IllegalStateException("Should not have been here.");
  }

  public int calculateExcessCapacity(OneInterchange interchange, int[] demandOfCustomer,
      int vehicleCapacity) {
    int newCustomerDemandFrom, newCustomerDemandTo;

    if (interchange.type == InterchangeType.Swap) {
      int customer1 = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx);
      int customer2 = routes.get(interchange.toRouteIdx).customers.get(interchange.toCustomerIdx);

      // Swap demand between routes.
      newCustomerDemandFrom =
          routes.get(interchange.fromRouteIdx).totalCustomerDemand + demandOfCustomer[customer2]
              - demandOfCustomer[customer1];
      newCustomerDemandTo =
          routes.get(interchange.toRouteIdx).totalCustomerDemand + demandOfCustomer[customer1]
              - demandOfCustomer[customer2];
    } else if (interchange.type == InterchangeType.Insertion) {
      int customer = routes.get(interchange.fromRouteIdx).customers.get(
          interchange.fromCustomerIdx);
      // Remove demand from one route and add it to the other one.
      newCustomerDemandFrom =
          routes.get(interchange.fromRouteIdx).totalCustomerDemand - demandOfCustomer[customer];
      newCustomerDemandTo =
          routes.get(interchange.toRouteIdx).totalCustomerDemand + demandOfCustomer[customer];
    } else {
      throw new IllegalStateException("Should not have been here.");
    }

    int excessCapacity = 0;
    // for each vehicle
    for (int routeIdx = 0; routeIdx < routes.size(); routeIdx++) {
      int currentCustomerDemand;
      if (routeIdx == interchange.fromRouteIdx) {
        currentCustomerDemand = newCustomerDemandFrom;
      } else if (routeIdx == interchange.toRouteIdx) {
        currentCustomerDemand = newCustomerDemandTo;
      } else {
        currentCustomerDemand = routes.get(routeIdx).totalCustomerDemand;
      }
      // only if its over what it should be, add amount over
      if (vehicleCapacity < currentCustomerDemand) {
        excessCapacity += currentCustomerDemand - vehicleCapacity;
      }
    }

    return excessCapacity;
  }

  public void perform(OneInterchange interchange, double[][] distances, int[] demandOfCustomer) {
    totalLength += calculateEdgeDelta(interchange, distances);

    Route routeFrom = routes.get(interchange.fromRouteIdx);
    Route routeTo = routes.get(interchange.toRouteIdx);

    if (interchange.type == InterchangeType.Swap) {
      // Update customer demands for the routes.
      routeFrom.totalCustomerDemand +=
          demandOfCustomer[routeTo.customers.get(interchange.toCustomerIdx)]
              - demandOfCustomer[routeFrom.customers.get(interchange.fromCustomerIdx)];
      routeTo.totalCustomerDemand +=
          demandOfCustomer[routeFrom.customers.get(interchange.fromCustomerIdx)]
              - demandOfCustomer[routeTo.customers.get(interchange.toCustomerIdx)];
      // Perform the swap.
      int temp = routeFrom.customers.get(interchange.fromCustomerIdx);
      routeFrom.customers.set(interchange.fromCustomerIdx,
          routeTo.customers.get(interchange.toCustomerIdx));
      routeTo.customers.set(interchange.toCustomerIdx, temp);
    } else if (interchange.type == InterchangeType.Insertion) {
      // Update customer demands for the routes.
      routeFrom.totalCustomerDemand -= demandOfCustomer[routeFrom.customers.get(
          interchange.fromCustomerIdx)];
      routeTo.totalCustomerDemand += demandOfCustomer[routeFrom.customers.get(
          interchange.fromCustomerIdx)];
      // Perform the insertion of the original coordinate into the new spot.
      routes.get(interchange.toRouteIdx).customers.add(interchange.toCustomerIdx,
          routes.get(interchange.fromRouteIdx).customers.get(interchange.fromCustomerIdx));
      // Remove the original value.
      routes.get(interchange.fromRouteIdx).customers.remove(interchange.fromCustomerIdx);
    }
  }
}
