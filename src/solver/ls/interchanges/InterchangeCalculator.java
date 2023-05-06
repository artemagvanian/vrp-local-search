package solver.ls.interchanges;

import java.util.List;
import java.util.concurrent.Callable;
import solver.ls.data.Interchange;
import solver.ls.data.InterchangeResult;
import solver.ls.data.RouteList;
import solver.ls.data.TabuItem;

public abstract class InterchangeCalculator implements Callable<InterchangeResult> {

  protected final RouteList routeList;
  protected final RouteList incumbent;
  protected final double excessCapacityPenaltyCoefficient;
  protected final List<TabuItem> shortTermMemory;
  protected final boolean firstBestFirst;
  protected Interchange bestInterchange;
  protected double bestObjective = Double.POSITIVE_INFINITY;

  public InterchangeCalculator(RouteList routeList, RouteList incumbent,
      double excessCapacityPenaltyCoefficient, List<TabuItem> shortTermMemory,
      boolean firstBestFirst) {
    this.routeList = routeList;
    this.incumbent = incumbent;
    this.excessCapacityPenaltyCoefficient = excessCapacityPenaltyCoefficient;
    this.shortTermMemory = shortTermMemory;
    this.firstBestFirst = firstBestFirst;
  }

  protected boolean isCustomerTabu(int routeIdx, int customerIdx) {
    int customer = routeList.routes.get(routeIdx).customers.get(customerIdx);
    for (TabuItem item : shortTermMemory) {
      if (item.customer == customer) {
        return true;
      }
    }
    return false;
  }
}
