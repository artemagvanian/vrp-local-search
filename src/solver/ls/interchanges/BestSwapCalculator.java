package solver.ls.interchanges;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import solver.ls.data.Insertion;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.Route;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;

public class BestSwapCalculator implements Callable<InterchangeResult> {

  private final int routeIdx1;
  private final RouteList routeList;
  private final RouteList incumbent;
  private final double excessCapacityPenaltyCoefficient;
  private final List<TabuItem> shortTermMemory;
  private final boolean firstBestFirst;
  private Interchange bestInterchange;
  private double bestObjective = Double.POSITIVE_INFINITY;

  public BestSwapCalculator(int routeIdx1, RouteList routeList, RouteList incumbent,
      double excessCapacityPenaltyCoefficient, List<TabuItem> shortTermMemory,
      boolean firstBestFirst) {
    this.routeIdx1 = routeIdx1;
    this.routeList = routeList;
    this.incumbent = incumbent;
    this.excessCapacityPenaltyCoefficient = excessCapacityPenaltyCoefficient;
    this.shortTermMemory = shortTermMemory;
    this.firstBestFirst = firstBestFirst;
  }

  private boolean isCustomerTabu(int routeIdx, int customerIdx) {
    int customer = routeList.routes.get(routeIdx).customers.get(customerIdx);
    for (TabuItem item : shortTermMemory) {
      if (item.customer == customer) {
        return true;
      }
    }
    return false;
  }

  public InterchangeResult call() {
    // Dummy interchange, to be edited later.
    Interchange interchange = new Interchange(
        routeIdx1, new ArrayList<>(List.of(new Insertion(0, 0))),
        0, new ArrayList<>(List.of(new Insertion(0, 0))));

    // Check every route with which we can swap.
    for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routeList.routes.size(); routeIdx2++) {
      interchange.routeIdx2 = routeIdx2;

      Route route1 = routeList.routes.get(routeIdx1);
      Route route2 = routeList.routes.get(routeIdx2);
      // Account for depots here.
      for (int customer1IdxFrom = 1; customer1IdxFrom < route1.customers.size() - 1;
          customer1IdxFrom++) {
        interchange.insertionList1.get(0).fromCustomerIdx = customer1IdxFrom;

        for (int customer2IdxFrom = 1; customer2IdxFrom < route2.customers.size() - 1;
            customer2IdxFrom++) {
          interchange.insertionList2.get(0).fromCustomerIdx = customer2IdxFrom;

          for (int customer1IdxTo = 1; customer1IdxTo < route2.customers.size() - 1;
              customer1IdxTo++) {
            interchange.insertionList1.get(0).toCustomerIdx = customer1IdxTo;

            for (int customer2IdxTo = 1; customer2IdxTo < route1.customers.size() - 1;
                customer2IdxTo++) {
              interchange.insertionList2.get(0).toCustomerIdx = customer2IdxTo;

              double newTotalLength =
                  routeList.length + routeList.calculateEdgeDelta(interchange);
              double excessCapacity = routeList.calculateExcessCapacity(interchange);

              double newObjective =
                  newTotalLength + excessCapacity * excessCapacityPenaltyCoefficient;

              // If we are better than what we have now.
              if (newObjective < bestObjective) {
                // Check whether the current customers are in the tabu list, account for aspiration.
                if ((!isCustomerTabu(routeIdx1, customer1IdxFrom) && !isCustomerTabu(routeIdx2,
                    customer2IdxFrom)) || (newObjective < incumbent.length
                    && excessCapacity == 0)) {
                  // Update the best values so far.
                  bestInterchange = interchange.clone();
                  bestObjective = newObjective;
                }
              }

              if (firstBestFirst && newObjective < incumbent.length && excessCapacity == 0) {
                return new InterchangeResult(bestInterchange, bestObjective);
              }
            }
          }
        }
      }
    }
    return new InterchangeResult(bestInterchange, bestObjective);
  }
}
