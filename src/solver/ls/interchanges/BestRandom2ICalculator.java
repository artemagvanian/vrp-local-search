package solver.ls.interchanges;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import solver.ls.data.Insertion;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.Route;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;

public class BestRandom2ICalculator extends InterchangeCalculator {

  private final int routeIdx1;
  private final int numAttempts;

  public BestRandom2ICalculator(RouteList routeList, RouteList incumbent,
      double excessCapacityPenaltyCoefficient, double customerUsePenaltyCoefficient,
      List<TabuItem> shortTermMemory, boolean firstBestFirst, int routeIdx1, int numAttempts,
      int currentIteration) {
    super(routeList, incumbent, excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient,
        shortTermMemory, firstBestFirst, currentIteration);
    this.routeIdx1 = routeIdx1;
    this.numAttempts = numAttempts;
  }

  public InterchangeResult call() {
    // Dummy interchange, to be edited later.
    Interchange interchange = new Interchange(
        routeIdx1, new ArrayList<>(List.of(new Insertion(0, 0), new Insertion(0, 0))),
        0, new ArrayList<>(List.of(new Insertion(0, 0), new Insertion(0, 0))));

    Random rand = new Random();

    // Check every route with which we can swap.
    for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routeList.routes.size(); routeIdx2++) {

      interchange.routeIdx2 = routeIdx2;

      Route route1 = routeList.routes.get(routeIdx1);
      Route route2 = routeList.routes.get(routeIdx2);

      if (route1.customers.size() < 4 || route2.customers.size() < 4) {
        continue;
      }

      for (int i = 0; i < numAttempts; i++) {

        interchange.insertionList1.get(0).fromCustomerIdx = rand.nextInt(1,
            route1.customers.size() - 1);
        interchange.insertionList1.get(0).toCustomerIdx = rand.nextInt(1,
            route2.customers.size() - 1);

        interchange.insertionList1.get(1).fromCustomerIdx = rand.nextInt(1,
            route1.customers.size() - 1);
        interchange.insertionList1.get(1).toCustomerIdx = rand.nextInt(1,
            route2.customers.size() - 1);

        interchange.insertionList2.get(0).fromCustomerIdx = rand.nextInt(1,
            route2.customers.size() - 1);
        interchange.insertionList2.get(0).toCustomerIdx = rand.nextInt(1,
            route1.customers.size() - 1);

        interchange.insertionList2.get(1).fromCustomerIdx = rand.nextInt(1,
            route2.customers.size() - 1);
        interchange.insertionList2.get(1).toCustomerIdx = rand.nextInt(1,
            route1.customers.size() - 1);

        if (interchange.insertionList1.get(0).fromCustomerIdx == interchange.insertionList1.get(
            1).fromCustomerIdx ||
            interchange.insertionList1.get(0).toCustomerIdx == interchange.insertionList1.get(
                1).toCustomerIdx ||
            interchange.insertionList2.get(0).fromCustomerIdx == interchange.insertionList2.get(
                1).fromCustomerIdx ||
            interchange.insertionList2.get(0).toCustomerIdx == interchange.insertionList2.get(
                1).toCustomerIdx) {
          continue;
        }

        double excessCapacity = routeList.calculateExcessCapacity(interchange);
        double newObjective = routeList.calculateObjective(interchange,
            excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient, currentIteration);

        // If we are better than what we have now.
        if (newObjective < bestObjective) {
          // Check whether the current customers are in the tabu list, account for aspiration.
          if ((!isCustomerTabu(routeIdx1, interchange.insertionList1.get(0).fromCustomerIdx) &&
              !isCustomerTabu(routeIdx1, interchange.insertionList1.get(1).fromCustomerIdx) &&
              !isCustomerTabu(routeIdx2, interchange.insertionList2.get(0).fromCustomerIdx) &&
              !isCustomerTabu(routeIdx2, interchange.insertionList2.get(1).fromCustomerIdx)) || (
              newObjective < incumbent.length && excessCapacity == 0)) {
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
    return new InterchangeResult(bestInterchange, bestObjective);
  }
}