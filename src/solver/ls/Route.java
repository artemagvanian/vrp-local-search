package solver.ls;

import java.util.LinkedList;

public class Route implements Cloneable {

  public LinkedList<Integer> customers;
  public int demand;
  public double length;

  public Route(LinkedList<Integer> customers, int demand, double length) {
    this.customers = customers;
    this.demand = demand;
    this.length = length;
  }

  @Override
  public String toString() {
    return "Route{" + "customers=" + customers + ", demand=" + demand + ", length=" + length + '}';
  }

  public Route clone() {
    Route newRoute;
    try {
      newRoute = (Route) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    newRoute.customers = new LinkedList<>(customers);
    return newRoute;
  }
}
