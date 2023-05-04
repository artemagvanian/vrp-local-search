package solver.ls.data;

import java.util.List;

public class Interchange {

  public final int routeIdx1;
  public final List<Insertion> insertionList1;
  public final int routeIdx2;
  public final List<Insertion> insertionList2;

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
}
