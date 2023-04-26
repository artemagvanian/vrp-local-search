package solver.ls;

public class TabuItem {

  public final int customer;
  public final int expirationIteration;

  public TabuItem(int customer, int expirationIteration) {
    this.customer = customer;
    this.expirationIteration = expirationIteration;
  }

  @Override
  public String toString() {
    return "TabuItem{" + "customer=" + customer + ", expirationIteration=" + expirationIteration
        + '}';
  }
}
