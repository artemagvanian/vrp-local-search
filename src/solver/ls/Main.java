package solver.ls;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage: java Main <file>");
      return;
    }

    String input = args[0];
    Path path = Paths.get(input);
    String filename = path.getFileName().toString();
    System.out.println("Instance: " + input);

    Timer watch = new Timer();
    watch.start();
    VRPInstance instance = new VRPInstance(input);
    double objective = instance.solve(
        true,
        false,
        false,
        10);
    watch.stop();

    System.out.println("{\"Instance\": \"" + filename +
        "\", \"Time\": " + String.format("%.2f", watch.getTime()) +
        ", \"Result\": " + String.format("%.2f", objective) +
        ", \"Solution\": \"" + instance.solutionString + "\"}");
  }
}
