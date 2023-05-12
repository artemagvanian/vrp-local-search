package solver.ls.interchanges;

import java.util.ArrayList;
import java.util.List;
import solver.ls.data.Insertion;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.Route;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;

public class Best2ICalculator extends InterchangeCalculator {

  private final int routeIdx1;

  public Best2ICalculator(RouteList routeList, RouteList incumbent,
      double excessCapacityPenaltyCoefficient, double customerUsePenaltyCoefficient,
      int currentIteration, List<TabuItem> shortTermMemory, boolean firstBestFirst, int routeIdx1) {
    super(routeList, incumbent, excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient,
        shortTermMemory, firstBestFirst, currentIteration);
    this.routeIdx1 = routeIdx1;
  }

  public InterchangeResult call() {
    // Dummy interchange, to be edited later.
    Interchange interchange = new Interchange(
        routeIdx1, new ArrayList<>(List.of(new Insertion(0, 0), new Insertion(0, 0))),
        0, new ArrayList<>(List.of(new Insertion(0, 0), new Insertion(0, 0))));

    Route route1 = routeList.routes.get(routeIdx1);

    // Check every route with which we can swap.
    for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routeList.routes.size(); routeIdx2++) {

      interchange.routeIdx2 = routeIdx2;
      Route route2 = routeList.routes.get(routeIdx2);

      if (route1.customers.size() < 4 || route2.customers.size() < 4) {
        continue;
      }

      for (int customerIdx11From = 1; customerIdx11From < route1.customers.size() - 1;
          customerIdx11From++) {
        for (int customerIdx12From = 1; customerIdx12From < route1.customers.size() - 1;
            customerIdx12From++) {
          for (int customerIdx21From = 1; customerIdx21From < route2.customers.size() - 1;
              customerIdx21From++) {
            for (int customerIdx22From = 1; customerIdx22From < route2.customers.size() - 1;
                customerIdx22From++) {
              for (int customerIdx11To = 1; customerIdx11To < route2.customers.size() - 1;
                  customerIdx11To++) {
                for (int customerIdx12To = 1; customerIdx12To < route2.customers.size() - 1;
                    customerIdx12To++) {
                  for (int customerIdx21To = 1; customerIdx21To < route1.customers.size() - 1;
                      customerIdx21To++) {
                    for (int customerIdx22To = 1; customerIdx22To < route1.customers.size() - 1;
                        customerIdx22To++) {

                      interchange.insertionList1.get(0).fromCustomerIdx = customerIdx11From;
                      interchange.insertionList1.get(0).toCustomerIdx = customerIdx11To;

                      interchange.insertionList1.get(1).fromCustomerIdx = customerIdx12From;
                      interchange.insertionList1.get(1).toCustomerIdx = customerIdx12To;

                      interchange.insertionList2.get(0).fromCustomerIdx = customerIdx21From;
                      interchange.insertionList2.get(0).toCustomerIdx = customerIdx21To;

                      interchange.insertionList2.get(1).fromCustomerIdx = customerIdx22From;
                      interchange.insertionList2.get(1).toCustomerIdx = customerIdx22To;

                      if (interchange.insertionList1.get(0).fromCustomerIdx
                          == interchange.insertionList1.get(
                          1).fromCustomerIdx ||
                          interchange.insertionList1.get(0).toCustomerIdx
                              == interchange.insertionList1.get(
                              1).toCustomerIdx ||
                          interchange.insertionList2.get(0).fromCustomerIdx
                              == interchange.insertionList2.get(
                              1).fromCustomerIdx ||
                          interchange.insertionList2.get(0).toCustomerIdx
                              == interchange.insertionList2.get(
                              1).toCustomerIdx) {
                        continue;
                      }

                      double excessCapacity = routeList.excessCapacity(interchange, route1, route2);
                      double newObjective = routeList.objective(interchange,
                          excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient,
                          currentIteration,
                          false);

                      // If we are better than what we have now.
                      if (newObjective < bestObjective) {
                        // Check whether the current customers are in the tabu list, account for aspiration.
                        if ((!isCustomerTabu(routeIdx1,
                            interchange.insertionList1.get(0).fromCustomerIdx) &&
                            !isCustomerTabu(routeIdx1,
                                interchange.insertionList1.get(1).fromCustomerIdx) &&
                            !isCustomerTabu(routeIdx2,
                                interchange.insertionList2.get(0).fromCustomerIdx) &&
                            !isCustomerTabu(routeIdx2,
                                interchange.insertionList2.get(1).fromCustomerIdx)) || (
                            newObjective < incumbent.length && excessCapacity == 0)) {
                          // Update the best values so far.
                          bestInterchange = interchange.clone();
                          bestObjective = newObjective;
                        }
                      }

                      if (firstBestFirst && newObjective < incumbent.length
                          && excessCapacity == 0) {
                        return new InterchangeResult(bestInterchange, bestObjective);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return new InterchangeResult(bestInterchange, bestObjective);
  }
}
