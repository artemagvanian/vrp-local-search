package solver.ls.data;

public class Insertion {

  public final int fromCustomerIdx;
  public final int toCustomerIdx;

  public Insertion(int fromCustomerIdx, int toCustomerIdx) {
    this.fromCustomerIdx = fromCustomerIdx;
    this.toCustomerIdx = toCustomerIdx;
  }

  @Override
  public String toString() {
    return "OneInterchange{" + ", fromCustomerIdx=" + fromCustomerIdx + ", toCustomerIdx="
        + toCustomerIdx + '}';
  }
}
