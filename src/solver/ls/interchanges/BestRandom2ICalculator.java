package solver.ls.interchanges;

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
  private final Random rand;

  public BestRandom2ICalculator(RouteList routeList, RouteList incumbent,
      double excessCapacityPenaltyCoefficient, double customerUsePenaltyCoefficient,
      int currentIteration, List<TabuItem> shortTermMemory, boolean firstBestFirst, int routeIdx1,
      int numAttempts, Random rand) {
    super(routeList, incumbent, excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient,
        shortTermMemory, firstBestFirst, currentIteration);
    this.routeIdx1 = routeIdx1;
    this.numAttempts = numAttempts;
    this.rand = rand;
  }

  public static int randIntBetween(Random rand, int origin, int bound) {
    return rand.nextInt(bound - origin) + origin;
  }

  public static void populateRandom2I(Interchange interchange, Route route1, Route route2,
      Random rand) {
    while (true) {
      interchange.insertionList1[0].fromCustomerIdx = randIntBetween(rand, 1,
          route1.length - 1);
      interchange.insertionList1[0].toCustomerIdx = randIntBetween(rand, 1,
          route2.length - 1);

      interchange.insertionList1[1].fromCustomerIdx = randIntBetween(rand, 1,
          route1.length - 1);
      interchange.insertionList1[1].toCustomerIdx = randIntBetween(rand, 1,
          route2.length - 1);

      interchange.insertionList2[0].fromCustomerIdx = randIntBetween(rand, 1,
          route2.length - 1);
      interchange.insertionList2[0].toCustomerIdx = randIntBetween(rand, 1,
          route1.length - 1);

      interchange.insertionList2[1].fromCustomerIdx = randIntBetween(rand, 1,
          route2.length - 1);
      interchange.insertionList2[1].toCustomerIdx = randIntBetween(rand, 1,
          route1.length - 1);

      if (interchange.insertionList1[0].fromCustomerIdx
          == interchange.insertionList1[1].fromCustomerIdx ||
          interchange.insertionList1[0].toCustomerIdx == interchange.insertionList1[1].toCustomerIdx
          ||
          interchange.insertionList2[0].fromCustomerIdx
              == interchange.insertionList2[1].fromCustomerIdx ||
          interchange.insertionList2[0].toCustomerIdx
              == interchange.insertionList2[1].toCustomerIdx) {
        continue;
      }
      return;
    }
  }

  public InterchangeResult call() {
    // Dummy interchange, to be edited later.
    Interchange interchange = new Interchange(
        routeIdx1, new Insertion[]{new Insertion(0, 0), new Insertion(0, 0)},
        0, new Insertion[]{new Insertion(0, 0), new Insertion(0, 0)});

    Route route1 = routeList.routes[routeIdx1];

    // Check every route with which we can swap.
    for (int routeIdx2 = routeIdx1 + 1; routeIdx2 < routeList.routes.length; routeIdx2++) {

      interchange.routeIdx2 = routeIdx2;
      Route route2 = routeList.routes[routeIdx2];

      if (route1.length < 4 || route2.length < 4) {
        continue;
      }

      for (int i = 0; i < numAttempts; i++) {
        populateRandom2I(interchange, route1, route2, rand);

        double excessCapacity = routeList.excessCapacity(interchange, route1, route2);
        double newObjective = routeList.objective(interchange,
            excessCapacityPenaltyCoefficient, customerUsePenaltyCoefficient, currentIteration,
            false);

        // If we are better than what we have now.
        if (newObjective < bestObjective) {
          // Check whether the current customers are in the tabu list, account for aspiration.
          if ((!isCustomerTabu(routeIdx1, interchange.insertionList1[0].fromCustomerIdx) &&
              !isCustomerTabu(routeIdx1, interchange.insertionList1[1].fromCustomerIdx) &&
              !isCustomerTabu(routeIdx2, interchange.insertionList2[0].fromCustomerIdx) &&
              !isCustomerTabu(routeIdx2, interchange.insertionList2[1].fromCustomerIdx)) || (
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
