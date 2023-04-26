package solver.ls;

import java.util.ArrayList;
import java.util.List;

public class Route implements Cloneable {

  public List<Integer> customers;
  public int totalCustomerDemand;

  public Route(List<Integer> customers, int totalCustomerDemand) {
    this.customers = customers;
    this.totalCustomerDemand = totalCustomerDemand;
  }

  @Override
  public String toString() {
    return "Route{" + "customers=" + customers + ", totalCustomerDemand=" + totalCustomerDemand
        + '}';
  }

  public Route clone() {
    Route newRoute;
    try {
      newRoute = (Route) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    newRoute.customers = new ArrayList<>(customers);
    return newRoute;
  }
}
