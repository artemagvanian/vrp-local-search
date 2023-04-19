package solver.ls;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

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

    // average demand maxK attempt
    int avgDemand = IntStream.of(instance.demandOfCustomer).sum() / instance.demandOfCustomer.length;
    int avgLoopLen = instance.vehicleCapacity / avgDemand;

    System.out.println("avgDemand: " + avgDemand + " avgLoopLen: " + avgLoopLen);

    double objective = instance.solve(
        true,
        false,
        false,
            (int) (avgLoopLen * 1.5),
        avgLoopLen / 2);
    watch.stop();

    System.out.println("{\"Instance\": \"" + filename +
        "\", \"Time\": " + String.format("%.2f", watch.getTime()) +
        ", \"Result\": " + String.format("%.2f", objective) +
        ", \"Solution\": \"" + instance.solutionString + "\"}");
  }
}
