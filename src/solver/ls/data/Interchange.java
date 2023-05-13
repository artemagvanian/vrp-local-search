package solver.ls.data;

import java.util.Arrays;

public class Interchange implements Cloneable {

  public int routeIdx1;
  public Insertion[] insertionList1;
  public int routeIdx2;
  public Insertion[] insertionList2;

  public Interchange(int routeIdx1, Insertion[] insertionList1, int routeIdx2,
      Insertion[] insertionList2) {
    this.routeIdx1 = routeIdx1;
    this.insertionList1 = insertionList1;
    this.routeIdx2 = routeIdx2;
    this.insertionList2 = insertionList2;
  }

  @Override
  public String toString() {
    return "{" + "\"routeIdx1\": " + routeIdx1 + ", \"insertionList1\": " + Arrays.toString(
        insertionList1) + ", \"routeIdx2\": " + routeIdx2 + ", \"insertionList2\": "
        + Arrays.toString(insertionList2) + '}';
  }

  @Override
  public Interchange clone() {
    try {
      Interchange clone = (Interchange) super.clone();
      clone.insertionList1 = new Insertion[insertionList1.length];
      clone.insertionList2 = new Insertion[insertionList2.length];
      for (int i = 0; i < insertionList1.length; i++) {
        clone.insertionList1[i] = insertionList1[i].clone();
      }
      for (int i = 0; i < insertionList2.length; i++) {
        clone.insertionList2[i] = insertionList2[i].clone();
      }
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
