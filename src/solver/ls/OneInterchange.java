package solver.ls;

public class OneInterchange {

  public final InterchangeType type;
  public final double newObjective;
  public final int fromRouteIdx;
  public final int fromCustomerIdx;
  public final int toRouteIdx;
  public final int toCustomerIdx;

  public OneInterchange(InterchangeType type, double newObjective, int fromRouteIdx,
      int fromCustomerIdx, int toRouteIdx, int toCustomerIdx) {
    this.type = type;
    this.newObjective = newObjective;
    this.fromRouteIdx = fromRouteIdx;
    this.fromCustomerIdx = fromCustomerIdx;
    this.toRouteIdx = toRouteIdx;
    this.toCustomerIdx = toCustomerIdx;
  }

  @Override
  public String toString() {
    return "OneInterchange{" +
        "type=" + type +
        ", newObjective=" + newObjective +
        ", fromRouteIdx=" + fromRouteIdx +
        ", fromCustomerIdx=" + fromCustomerIdx +
        ", toRouteIdx=" + toRouteIdx +
        ", toCustomerIdx=" + toCustomerIdx +
        '}';
  }
}
