package solver.ls;

public class TabuItem {

  public final int customer;
  public final int iteration;

  public TabuItem(int customer, int iteration) {
    this.customer = customer;
    this.iteration = iteration;
  }

  @Override
  public String toString() {
    return "TabuItem{" +
        "customer=" + customer +
        ", iteration=" + iteration +
        '}';
  }
}
