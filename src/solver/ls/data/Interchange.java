package solver.ls.data;

import java.util.ArrayList;
import java.util.List;

public class Interchange implements Cloneable {

  public int routeIdx1;
  public List<Insertion> insertionList1;
  public int routeIdx2;
  public List<Insertion> insertionList2;

  public Interchange(int routeIdx1, List<Insertion> insertionList1, int routeIdx2,
      List<Insertion> insertionList2) {
    this.routeIdx1 = routeIdx1;
    this.insertionList1 = insertionList1;
    this.routeIdx2 = routeIdx2;
    this.insertionList2 = insertionList2;
  }

  @Override
  public String toString() {
    return "{" + "\"routeIdx1\": " + routeIdx1 + ", \"insertionList1\": " + insertionList1
        + ", \"routeIdx2\": " + routeIdx2 + ", \"insertionList2\": " + insertionList2 + '}';
  }

  @Override
  public Interchange clone() {
    try {
      Interchange clone = (Interchange) super.clone();
      clone.insertionList1 = new ArrayList<>();
      clone.insertionList2 = new ArrayList<>();
      for (Insertion insertion : insertionList1) {
        clone.insertionList1.add(insertion.clone());
      }
      for (Insertion insertion : insertionList2) {
        clone.insertionList2.add(insertion.clone());
      }
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  public void invert() {
    int temp;

    temp = routeIdx1;
    routeIdx1 = routeIdx2;
    routeIdx2 = temp;

    for (Insertion insertion : insertionList1) {
      temp = insertion.fromCustomerIdx;
      insertion.fromCustomerIdx = insertion.toCustomerIdx;
      insertion.toCustomerIdx = temp;
    }
  }
}
