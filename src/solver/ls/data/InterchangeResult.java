package solver.ls.data;

public class InterchangeResult {
  public final Interchange interchange;
  public final double objective;

  public InterchangeResult(Interchange interchange, double objective) {
    this.interchange = interchange;
    this.objective = objective;
  }
}
