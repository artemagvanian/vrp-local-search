package solver.ls;

public record OneInterchange(InterchangeType type, double newObjective,
                             int fromRouteIdx, int fromCustomerIdx,
                             int toRouteIdx, int toCustomerIdx) {

}
