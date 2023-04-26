package solver.ls;

public class OneInterchange {

  public final InterchangeType type;
  public final int fromRouteIdx;
  public final int fromCustomerIdx;
  public final int toRouteIdx;
  public final int toCustomerIdx;

  public OneInterchange(InterchangeType type, int fromRouteIdx, int fromCustomerIdx, int toRouteIdx,
      int toCustomerIdx) {
    this.type = type;
    this.fromRouteIdx = fromRouteIdx;
    this.fromCustomerIdx = fromCustomerIdx;
    this.toRouteIdx = toRouteIdx;
    this.toCustomerIdx = toCustomerIdx;
  }

  @Override
  public String toString() {
    return "OneInterchange{" + "type=" + type + ", fromRouteIdx=" + fromRouteIdx
        + ", fromCustomerIdx=" + fromCustomerIdx + ", toRouteIdx=" + toRouteIdx + ", toCustomerIdx="
        + toCustomerIdx + '}';
  }
}
