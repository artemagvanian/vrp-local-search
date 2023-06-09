package solver.ls.utils;

public class Timer {

  private long startTime;
  private long stopTime;
  private boolean running;

  public Timer() {
    super();
  }

  public void start() {
    this.startTime = System.nanoTime();
    this.running = true;
  }

  public void stop() {
    if (running) {
      this.stopTime = System.nanoTime();
      this.running = false;
    }
  }

  public double getTime() {
    double elapsed;
    double nano = 1000000000.0;
    if (running) {
      elapsed = ((System.nanoTime() - startTime) / nano);
    } else {
      elapsed = ((stopTime - startTime) / nano);
    }
    return elapsed;
  }
}
