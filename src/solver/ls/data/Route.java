package solver.ls.data;

import java.util.Arrays;
import java.util.List;

public class Route implements Cloneable {

  public int[] customers;
  public int demand;
  public int length;

  public Route(List<Integer> customers, int numCustomers, int demand) {
    this.customers = new int[numCustomers + 1];
    for (int i = 0; i < customers.size(); i++) {
      this.customers[i] = customers.get(i);
    }
    this.length = customers.size();
    this.demand = demand;
  }

  @Override
  public String toString() {
    return "{" + "\"customers\": " + Arrays.toString(customers) + ", \"demand\": " + demand + '}';
  }

  public Route clone() {
    Route newRoute;
    try {
      newRoute = (Route) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    newRoute.customers = Arrays.copyOf(customers, customers.length);
    return newRoute;
  }

  public double calculateRouteLength(double[][] distances) {
    double routeLength = 0;
    for (int i = 0; i < length - 1; i++) {
      routeLength += distances[customers[i]][customers[i + 1]];
    }
    return routeLength;
  }
}
