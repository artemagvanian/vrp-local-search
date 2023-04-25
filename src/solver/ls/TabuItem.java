package solver.ls;

public class TabuItem {

  public final int customer;
  public final int experationIteration;

  public TabuItem(int customer, int experationIteration) {
    this.customer = customer;
    this.experationIteration = experationIteration;
  }

  @Override
  public String toString() {
    return "TabuItem{" +
        "customer=" + customer +
        ", iteration=" + experationIteration +
        '}';
  }
}
