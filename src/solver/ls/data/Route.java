package solver.ls.data;

import java.util.ArrayList;
import java.util.List;

public class Route implements Cloneable {

  public List<Integer> customers;
  public int demand;

  public Route(List<Integer> customers, int demand) {
    this.customers = customers;
    this.demand = demand;
  }

  @Override
  public String toString() {
    return "Route{" + "customers=" + customers + ", demand=" + demand + '}';
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
