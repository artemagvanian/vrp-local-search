package solver.ls.interchanges;

import java.util.List;
import solver.ls.data.Insertion;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.Route;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;

public class Best1ICalculator extends InterchangeCalculator {

  private final int routeIdx1;

  public Best1ICalculator(RouteList routeList, RouteList incumbent,
      double excessCapacityPenaltyCoefficient, double customerUsePenaltyCoefficient,
      int currentIteration, List<TabuItem> shortTermMemory, boolean firstBestFirst, int routeIdx1) {
    super(routeList, incumbent, excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient,
        shortTermMemory, firstBestFirst, currentIteration);
    this.routeIdx1 = routeIdx1;
  }

  public InterchangeResult call() {
    // Dummy interchange, to be edited later.
    Interchange interchange = new Interchange(
        routeIdx1, new Insertion[]{new Insertion(0, 0)},
        0, new Insertion[]{new Insertion(0, 0)});

    Route route1 = routeList.routes[routeIdx1];
    // Check every route with which we can swap.
    for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routeList.routes.length; routeIdx2++) {
      interchange.routeIdx2 = routeIdx2;
      Route route2 = routeList.routes[routeIdx2];
      // Account for depots here.
      for (int customer1IdxFrom = 1; customer1IdxFrom < route1.length - 1;
          customer1IdxFrom++) {
        interchange.insertionList1[0].fromCustomerIdx = customer1IdxFrom;

        for (int customer2IdxFrom = 1; customer2IdxFrom < route2.length - 1;
            customer2IdxFrom++) {
          interchange.insertionList2[0].fromCustomerIdx = customer2IdxFrom;

          for (int customer1IdxTo = 1; customer1IdxTo < route2.length - 1;
              customer1IdxTo++) {
            interchange.insertionList1[0].toCustomerIdx = customer1IdxTo;

            for (int customer2IdxTo = 1; customer2IdxTo < route1.length - 1;
                customer2IdxTo++) {
              interchange.insertionList2[0].toCustomerIdx = customer2IdxTo;

              double excessCapacity = routeList.excessCapacity(interchange, route1, route2);
              double newObjective = routeList.objective(interchange,
                  excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient,
                  currentIteration, false);

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
