package solver.ls.data;

public class TabuItem {

  public final int customer;
  public final int expirationIteration;

  public TabuItem(int customer, int expirationIteration) {
    this.customer = customer;
    this.expirationIteration = expirationIteration;
  }

  @Override
  public String toString() {
    return "{" + "\"customer\":" + customer + ", \"expirationIteration\":" + expirationIteration
        + '}';
  }
}
