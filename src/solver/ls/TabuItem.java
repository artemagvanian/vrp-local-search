package solver.ls;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TabuItem tabuItem = (TabuItem) o;
    return customer == tabuItem.customer;
  }

  @Override
  public int hashCode() {
    return Objects.hash(customer);
  }
}
