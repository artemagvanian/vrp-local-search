package solver.ls.interchanges;

import java.util.ArrayList;
import java.util.List;
import solver.ls.data.Insertion;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.Route;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;

public class Best0ICalculator extends InterchangeCalculator {

  private final int routeIdx1;

  public Best0ICalculator(RouteList routeList, RouteList incumbent,
      double excessCapacityPenaltyCoefficient, double customerUsePenaltyCoefficient,
      int currentIteration, List<TabuItem> shortTermMemory, boolean firstBestFirst, int routeIdx1) {
    super(routeList, incumbent, excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient,
        shortTermMemory, firstBestFirst, currentIteration);
    this.routeIdx1 = routeIdx1;
  }

  public InterchangeResult call() {
    // Dummy interchange, to be edited later.
    Interchange interchange = new Interchange(
        routeIdx1, new ArrayList<>(List.of(new Insertion(0, 0))),
        0, new ArrayList<>(List.of()));

    Route route1 = routeList.routes.get(routeIdx1);
    // Check every route into which we can insert.
    for (int routeIdx2 = 0; routeIdx2 < routeList.routes.size(); routeIdx2++) {
      if (routeIdx2 == routeIdx1) {
        continue;
      }
      interchange.routeIdx2 = routeIdx2;
      Route route2 = routeList.routes.get(routeIdx2);

      for (int customerIdxFrom = 1; customerIdxFrom < route1.customers.size() - 1;
          customerIdxFrom++) {
        interchange.insertionList1.get(0).fromCustomerIdx = customerIdxFrom;
        for (int customerIdxTo = 1; customerIdxTo < route2.customers.size(); customerIdxTo++) {
          interchange.insertionList1.get(0).toCustomerIdx = customerIdxTo;

          double excessCapacity = routeList.excessCapacity(interchange, route1, route2);
          // Calculate objective function and check whether it is better than the current.
          double newObjective = routeList.objective(interchange,
              excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient, currentIteration,
              false);
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
