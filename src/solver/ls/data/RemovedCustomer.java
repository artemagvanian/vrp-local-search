package solver.ls.data;

public class RemovedCustomer {

  public final Integer customer;
  public final Insertion insertion;

  public RemovedCustomer(Integer customer, Insertion insertion) {
    this.customer = customer;
    this.insertion = insertion;
  }

  @Override
  public String toString() {
    return "{" + "\"customer\": " + customer + ", \"insertion\": " + insertion + '}';
  }
}
