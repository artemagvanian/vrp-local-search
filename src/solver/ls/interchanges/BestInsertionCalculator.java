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

public class BestInsertionCalculator implements Callable<InterchangeResult> {

  private final int routeIdx1;
  private final RouteList routeList;
  private final RouteList incumbent;
  private final double excessCapacityPenaltyCoefficient;
  private final List<TabuItem> shortTermMemory;
  private final boolean firstBestFirst;
  private Interchange bestInterchange;
  private double bestObjective = Double.POSITIVE_INFINITY;

  public BestInsertionCalculator(int routeIdx1, RouteList routeList, RouteList incumbent,
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
        0, new ArrayList<>(List.of()));

    // Check every route into which we can insert.
    for (int routeIdx2 = 0; routeIdx2 < routeList.routes.size(); routeIdx2++) {
      if (routeIdx2 == routeIdx1) {
        continue;
      }
      interchange.routeIdx2 = routeIdx2;

      Route route1 = routeList.routes.get(routeIdx1);
      Route route2 = routeList.routes.get(routeIdx2);

      for (int customerIdxFrom = 1; customerIdxFrom < route1.customers.size() - 1;
          customerIdxFrom++) {
        interchange.insertionList1.get(0).fromCustomerIdx = customerIdxFrom;
        for (int customerIdxTo = 1; customerIdxTo < route2.customers.size(); customerIdxTo++) {
          interchange.insertionList1.get(0).toCustomerIdx = customerIdxTo;

          double newTotalLength =
              routeList.length + routeList.calculateEdgeDelta(interchange);
          double excessCapacity = routeList.calculateExcessCapacity(interchange);

          // Calculate objective function and check whether it is better than the current.
          double newObjective = newTotalLength + excessCapacity * excessCapacityPenaltyCoefficient;
          if (newObjective < bestObjective) {
            // Check whether the current customer is in the tabu list, account for aspiration.
            if (!isCustomerTabu(routeIdx1, customerIdxFrom) ||
                (newObjective < incumbent.length && excessCapacity == 0)) {
              // Save the best place to insert this customer so far.
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
    return new InterchangeResult(bestInterchange, bestObjective);
  }
}
