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
    VRPInstanceIncomplete instance = new VRPInstanceIncomplete(input);
    watch.stop();
    Timer watch2 = new Timer();
    watch2.start();
//    VRPInstanceComplete instance2 = new VRPInstanceComplete(input);

    // Calculate average demand and average loop length for minK and maxK
    int avgDemand =
        IntStream.of(instance.demandOfCustomer).sum() / instance.demandOfCustomer.length;
    int avgLoopLength = instance.vehicleCapacity / avgDemand;

    System.out.println("Average demand: " + avgDemand + "; Average loop length: " + avgLoopLength);

    // Calling solve for the complete model.
//     instance2.solve(
//        true,
//        false,
//        false,
//        1, instance.numCustomers);

    watch2.stop();
//
    System.out.println("*************");
//    System.out.println("{\"Instance2 (COMPLETE) \": \"" + filename +
//            "\", \"Time\": " + String.format("%.2f", watch2.getTime()) +
//            ", \"Result\": " + String.format("%.2f", instance2.getTourLength(instance2.routes)) +
//            ", \"Solution\": \"" + instance2.serializeRoutes() + "\"}");
    System.out.println("*************");
    System.out.println("Amount over capacity: " + instance.getAmtOverCapacity(instance.routes));
    System.out.println("{\"Instance\": \"" + filename +
        "\", \"Time\": " + String.format("%.2f", watch.getTime()) +
        ", \"Result\": " + String.format("%.2f", instance.getTourLength(instance.routes)) +
        ", \"Solution\": \"" + instance.serializeRoutes() + "\"}");
  }
}
