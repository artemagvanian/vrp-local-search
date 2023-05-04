package solver.ls.data;

public class Insertion implements Cloneable {

  public int fromCustomerIdx;
  public int toCustomerIdx;

  public Insertion(int fromCustomerIdx, int toCustomerIdx) {
    this.fromCustomerIdx = fromCustomerIdx;
    this.toCustomerIdx = toCustomerIdx;
  }

  @Override
  public String toString() {
    return "{" + "\"fromCustomerIdx\": " + fromCustomerIdx + ", \"toCustomerIdx\": " + toCustomerIdx
        + '}';
  }

  @Override
  public Insertion clone() {
    try {
      return (Insertion) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
