package solver.ls.incremental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import solver.ls.data.Insertion;
import solver.ls.data.Interchange;
import solver.ls.data.RemovedCustomer;
import solver.ls.data.Route;
import solver.ls.data.RouteList;

public class EdgeDeltaCalculators {

  private static double naiveDelta(Route route1, Route route2, Interchange interchange,
      double[][] distances) {
    Route clonedRoute1 = route1.clone();
    Route clonedRoute2 = route2.clone();

    performRawInterchange(clonedRoute1, clonedRoute2, interchange.insertionList1,
        interchange.insertionList2);

    double newRoute1Length = clonedRoute1.calculateRouteLength(distances);
    double newRoute2Length = clonedRoute2.calculateRouteLength(distances);

    double oldRoute1Length = route1.calculateRouteLength(distances);
    double oldRoute2Length = route2.calculateRouteLength(distances);

    return newRoute1Length - oldRoute1Length + newRoute2Length - oldRoute2Length;
  }

  private static double delta0I(Route route1, Route route2, Interchange interchange,
      double[][] distances) {
    Insertion insertion = interchange.insertionList1[0];

    int customer = route1.customers[insertion.fromCustomerIdx];
    int currentLeftNeighbor = route1.customers[insertion.fromCustomerIdx - 1];
    int currentRightNeighbor = route1.customers[insertion.fromCustomerIdx + 1];

    int futureLeftNeighbor = route2.customers[insertion.toCustomerIdx - 1];
    int futureRightNeighbor = route2.customers[insertion.toCustomerIdx];

    double edgePositiveDelta =
        distances[customer][futureLeftNeighbor] + distances[customer][futureRightNeighbor]
            + distances[currentLeftNeighbor][currentRightNeighbor];

    double edgeNegativeDelta =
        distances[customer][currentLeftNeighbor] + distances[customer][currentRightNeighbor]
            + distances[futureLeftNeighbor][futureRightNeighbor];

    // Sanity check.
    /*
    assert Math.abs(
        naiveDelta(route1, route2, interchange, distances) - edgePositiveDelta + edgeNegativeDelta)
        < Math.pow(10, -6);
     */

    return edgePositiveDelta - edgeNegativeDelta;
  }

  private static double delta1I(Route route1, Route route2, Interchange interchange,
      double[][] distances) {
    Insertion insertion1 = interchange.insertionList1[0];
    Insertion insertion2 = interchange.insertionList2[0];

    int customer1 = route1.customers[insertion1.fromCustomerIdx];
    int customer1LeftNeighbor = route1.customers[insertion1.fromCustomerIdx - 1];
    int customer1RightNeighbor = route1.customers[insertion1.fromCustomerIdx + 1];

    int customer2 = route2.customers[insertion2.fromCustomerIdx];
    int customer2LeftNeighbor = route2.customers[insertion2.fromCustomerIdx - 1];
    int customer2RightNeighbor = route2.customers[insertion2.fromCustomerIdx + 1];

    double extractionDelta =
        distances[customer1LeftNeighbor][customer1RightNeighbor]
            - distances[customer1LeftNeighbor][customer1]
            - distances[customer1RightNeighbor][customer1]
            + distances[customer2LeftNeighbor][customer2RightNeighbor]
            - distances[customer2LeftNeighbor][customer2]
            - distances[customer2RightNeighbor][customer2];

    int customer1FutureLeftNeighbor = route2.customers[
        insertion1.toCustomerIdx - 1 < insertion2.fromCustomerIdx ? insertion1.toCustomerIdx - 1
            : insertion1.toCustomerIdx];

    int customer1FutureRightNeighbor = route2.customers[
        insertion1.toCustomerIdx < insertion2.fromCustomerIdx ? insertion1.toCustomerIdx
            : insertion1.toCustomerIdx + 1];

    int customer2FutureLeftNeighbor = route1.customers[
        insertion2.toCustomerIdx - 1 < insertion1.fromCustomerIdx ? insertion2.toCustomerIdx - 1
            : insertion2.toCustomerIdx];

    int customer2FutureRightNeighbor = route1.customers[
        insertion2.toCustomerIdx < insertion1.fromCustomerIdx ? insertion2.toCustomerIdx
            : insertion2.toCustomerIdx + 1];

    double insertionDelta =
        -distances[customer1FutureLeftNeighbor][customer1FutureRightNeighbor]
            + distances[customer1FutureLeftNeighbor][customer1]
            + distances[customer1FutureRightNeighbor][customer1]
            - distances[customer2FutureLeftNeighbor][customer2FutureRightNeighbor]
            + distances[customer2FutureLeftNeighbor][customer2]
            + distances[customer2FutureRightNeighbor][customer2];

    // Sanity check.
    /*
    assert Math.abs(
        naiveDelta(route1, route2, interchange, distances) - extractionDelta - insertionDelta)
        < Math.pow(10, -6);
     */

    return extractionDelta + insertionDelta;
  }

  private static double extractionCost2I(Route route, Insertion insertion1,
      Insertion insertion2, double[][] distances) {
    int customer1 = route.customers[insertion1.fromCustomerIdx];
    int customer1LeftNeighbor = route.customers[insertion1.fromCustomerIdx - 1];
    int customer1RightNeighbor = route.customers[insertion1.fromCustomerIdx + 1];

    int customer2 = route.customers[insertion2.fromCustomerIdx];
    int customer2LeftNeighbor = route.customers[insertion2.fromCustomerIdx - 1];
    int customer2RightNeighbor = route.customers[insertion2.fromCustomerIdx + 1];

    double extractionDelta = 0;

    if (Math.abs(insertion1.fromCustomerIdx - insertion2.fromCustomerIdx) > 1) {
      extractionDelta += distances[customer1LeftNeighbor][customer1RightNeighbor]
          - distances[customer1LeftNeighbor][customer1]
          - distances[customer1][customer1RightNeighbor]
          + distances[customer2LeftNeighbor][customer2RightNeighbor]
          - distances[customer2LeftNeighbor][customer2]
          - distances[customer2][customer2RightNeighbor];
    } else {
      if (insertion1.fromCustomerIdx < insertion2.fromCustomerIdx) {
        extractionDelta += distances[customer1LeftNeighbor][customer2RightNeighbor]
            - distances[customer1LeftNeighbor][customer1]
            - distances[customer2][customer2RightNeighbor]
            - distances[customer1][customer2];
      } else {
        extractionDelta += distances[customer1RightNeighbor][customer2LeftNeighbor]
            - distances[customer1][customer1RightNeighbor]
            - distances[customer2LeftNeighbor][customer2]
            - distances[customer1][customer2];
      }
    }

    return extractionDelta;
  }

  private static double insertionCost2I(Route intoRoute, Insertion insertion1,
      Insertion insertion2, int extractedFromIdx1, int extractedFromIdx2, int customer1,
      int customer2, double[][] distances) {

    if (extractedFromIdx1 > extractedFromIdx2) {
      int temp = extractedFromIdx2;
      extractedFromIdx2 = extractedFromIdx1;
      extractedFromIdx1 = temp;
    }

    int customer1FutureLeftNeighborIdx = insertion1.toCustomerIdx - 1;
    int customer1FutureRightNeighborIdx = insertion1.toCustomerIdx;
    int customer2FutureLeftNeighborIdx = insertion2.toCustomerIdx - 1;
    int customer2FutureRightNeighborIdx = insertion2.toCustomerIdx;

    if (insertion1.toCustomerIdx < insertion2.toCustomerIdx) {
      customer2FutureLeftNeighborIdx--;
      customer2FutureRightNeighborIdx--;
    } else {
      customer1FutureLeftNeighborIdx--;
      customer1FutureRightNeighborIdx--;
    }

    if (customer1FutureLeftNeighborIdx >= extractedFromIdx1) {
      customer1FutureLeftNeighborIdx++;
    }
    if (customer1FutureLeftNeighborIdx >= extractedFromIdx2) {
      customer1FutureLeftNeighborIdx++;
    }

    if (customer2FutureLeftNeighborIdx >= extractedFromIdx1) {
      customer2FutureLeftNeighborIdx++;
    }
    if (customer2FutureLeftNeighborIdx >= extractedFromIdx2) {
      customer2FutureLeftNeighborIdx++;
    }

    if (customer1FutureRightNeighborIdx >= extractedFromIdx1) {
      customer1FutureRightNeighborIdx++;
    }
    if (customer1FutureRightNeighborIdx >= extractedFromIdx2) {
      customer1FutureRightNeighborIdx++;
    }

    if (customer2FutureRightNeighborIdx >= extractedFromIdx1) {
      customer2FutureRightNeighborIdx++;
    }
    if (customer2FutureRightNeighborIdx >= extractedFromIdx2) {
      customer2FutureRightNeighborIdx++;
    }

    if (Math.abs(insertion1.toCustomerIdx - insertion2.toCustomerIdx) > 1) {
      int customer1FutureLeftNeighbor = intoRoute.customers[customer1FutureLeftNeighborIdx];
      int customer1FutureRightNeighbor = intoRoute.customers[customer1FutureRightNeighborIdx];
      int customer2FutureLeftNeighbor = intoRoute.customers[customer2FutureLeftNeighborIdx];
      int customer2FutureRightNeighbor = intoRoute.customers[customer2FutureRightNeighborIdx];

      return -distances[customer1FutureLeftNeighbor][customer1FutureRightNeighbor]
          + distances[customer1FutureLeftNeighbor][customer1]
          + distances[customer1][customer1FutureRightNeighbor]
          - distances[customer2FutureLeftNeighbor][customer2FutureRightNeighbor]
          + distances[customer2FutureLeftNeighbor][customer2]
          + distances[customer2][customer2FutureRightNeighbor];
    } else {
      int leftNeighborIdx = Math.max(customer1FutureLeftNeighborIdx,
          customer2FutureLeftNeighborIdx);

      int rightNeighborIdx = Math.min(customer1FutureRightNeighborIdx,
          customer2FutureRightNeighborIdx);

      int leftNeighbor = intoRoute.customers[leftNeighborIdx];
      int rightNeighbor = intoRoute.customers[rightNeighborIdx];

      if (insertion1.toCustomerIdx < insertion2.toCustomerIdx) {
        return -distances[leftNeighbor][rightNeighbor]
            + distances[leftNeighbor][customer1]
            + distances[customer2][rightNeighbor]
            + distances[customer1][customer2];
      } else {
        return -distances[leftNeighbor][rightNeighbor]
            + distances[customer1][rightNeighbor]
            + distances[leftNeighbor][customer2]
            + distances[customer1][customer2];
      }
    }
  }

  private static double delta2I(Route route1, Route route2, Interchange interchange,
      double[][] distances) {
    Insertion insertion11 = interchange.insertionList1[0];
    Insertion insertion12 = interchange.insertionList1[1];

    Insertion insertion21 = interchange.insertionList2[0];
    Insertion insertion22 = interchange.insertionList2[1];

    double route1ExtractionCost = extractionCost2I(route1, insertion11, insertion12,
        distances);
    double route2ExtractionCost = extractionCost2I(route2, insertion21, insertion22,
        distances);

    double route1InsertionCost = insertionCost2I(route1, insertion21, insertion22,
        insertion11.fromCustomerIdx, insertion12.fromCustomerIdx,
        route2.customers[insertion21.fromCustomerIdx],
        route2.customers[insertion22.fromCustomerIdx], distances);

    double route2InsertionCost = insertionCost2I(route2, insertion11, insertion12,
        insertion21.fromCustomerIdx, insertion22.fromCustomerIdx,
        route1.customers[insertion11.fromCustomerIdx],
        route1.customers[insertion12.fromCustomerIdx], distances);

    // Sanity check.
    /*
    assert Math.abs(naiveDelta(route1, route2, interchange, distances) - (route1ExtractionCost
        + route2ExtractionCost + route1InsertionCost + route2InsertionCost)) < Math.pow(10, -6);
     */

    return route1ExtractionCost + route2ExtractionCost + route1InsertionCost + route2InsertionCost;
  }

  public static void performRawInterchange(Route route1, Route route2,
      Insertion[] insertionList1,
      Insertion[] insertionList2) {

    List<RemovedCustomer> movedCustomers1 = new ArrayList<>();
    List<RemovedCustomer> movedCustomers2 = new ArrayList<>();

    // Sort in the decreasing order by fromCustomerIdx.
    Arrays.sort(insertionList1, (ins1, ins2) -> ins2.fromCustomerIdx - ins1.fromCustomerIdx);
    for (Insertion insertion : insertionList1) {
      RemovedCustomer customer = new RemovedCustomer(route1.customers[insertion.fromCustomerIdx],
          insertion);
      for (int i = insertion.fromCustomerIdx; i < route1.length - 1; i++) {
        route1.customers[i] = route1.customers[i + 1];
      }
      route1.length--;
      movedCustomers1.add(customer);
    }

    // Sort in the decreasing order by fromCustomerIdx.
    Arrays.sort(insertionList2, (ins1, ins2) -> ins2.fromCustomerIdx - ins1.fromCustomerIdx);
    for (Insertion insertion : insertionList2) {
      RemovedCustomer customer = new RemovedCustomer(route2.customers[insertion.fromCustomerIdx],
          insertion);
      for (int i = insertion.fromCustomerIdx; i < route2.length - 1; i++) {
        route2.customers[i] = route2.customers[i + 1];
      }
      route2.length--;
      movedCustomers2.add(customer);
    }

    // Sort in the increasing order by toCustomerIdx.
    movedCustomers1.sort(Comparator.comparingInt(ins -> ins.insertion.toCustomerIdx));
    for (RemovedCustomer removedCustomer : movedCustomers1) {
      for (int i = route2.length - 1; i >= removedCustomer.insertion.toCustomerIdx; i--) {
        route2.customers[i + 1] = route2.customers[i];
      }
      route2.length++;
      route2.customers[removedCustomer.insertion.toCustomerIdx] = removedCustomer.customer;
    }

    // Sort in the increasing order by toCustomerIdx.
    movedCustomers2.sort(Comparator.comparingInt(ins -> ins.insertion.toCustomerIdx));
    for (RemovedCustomer removedCustomer : movedCustomers2) {
      for (int i = route1.length - 1; i >= removedCustomer.insertion.toCustomerIdx; i--) {
        route1.customers[i + 1] = route1.customers[i];
      }
      route1.length++;
      route1.customers[removedCustomer.insertion.toCustomerIdx] = removedCustomer.customer;
    }

  }

  public static double edgeDelta(Interchange interchange, RouteList routeList,
      double[][] distances) {
    Route route1 = routeList.routes[interchange.routeIdx1];
    Route route2 = routeList.routes[interchange.routeIdx2];

    if (interchange.insertionList1.length == 1 && interchange.insertionList2.length == 0) {
      return delta0I(route1, route2, interchange, distances);
    } else if (interchange.insertionList1.length == 1 && interchange.insertionList2.length == 1) {
      return delta1I(route1, route2, interchange, distances);
    } else if (interchange.insertionList1.length == 2 && interchange.insertionList2.length == 2) {
      return delta2I(route1, route2, interchange, distances);
    } else {
      throw new IllegalArgumentException("Can only process (1,0), (1,1), (2,2) -interchanges.");
    }
  }
}
