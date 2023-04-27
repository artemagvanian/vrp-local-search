package solver.ls;

public class RemovedCustomer {

  public final Integer customer;
  public final Insertion insertion;

  public RemovedCustomer(Integer customer, Insertion insertion) {
    this.customer = customer;
    this.insertion = insertion;
  }

  @Override
  public String toString() {
    return "RemovedCustomer{" + "customer=" + customer + ", insertion=" + insertion + '}';
  }
}
