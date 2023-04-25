package solver.ls;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
// import java.util.stream.IntStream;

public class Main {

  public static void main(String[] args) throws IOException {
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
    VRPInstanceIncomplete incompleteInstance = new VRPInstanceIncomplete(input, 10000);
    watch.stop();

    /*
    Timer watch2 = new Timer();
    watch2.start();
    VRPInstanceComplete completeInstance = new VRPInstanceComplete(input);

    // Calculate average demand and average loop length for minK and maxK
    int avgDemand =
        IntStream.of(incompleteInstance.demandOfCustomer).sum()
            / incompleteInstance.demandOfCustomer.length;
    int avgLoopLength = incompleteInstance.vehicleCapacity / avgDemand;

    System.out.println("Average demand: " + avgDemand + "; Average loop length: " + avgLoopLength);

    // Calling solve for the complete model.
    completeInstance.solve(
        true,
        false,
        false,
        1, completeInstance.numCustomers);

    watch2.stop();

    System.out.println("*************");
    System.out.println("{\"Instance (COMPLETE) \": \"" + filename +
        "\", \"Time\": " + String.format("%.2f", watch2.getTime()) +
        ", \"Result\": " + String.format("%.2f",
        completeInstance.getTourLength(completeInstance.routes)) +
        ", \"Solution\": \"" + completeInstance.serializeRoutes(completeInstance.routes) + "\"}");
    System.out.println("*************");
     */

    System.out.println(
        "Amount over capacity (expect it to be 0): " + incompleteInstance.calculateExcessCapacity(
            incompleteInstance.incumbent));

    // Generate the solution files.
    String instanceHeader =
        String.format("%.2f", incompleteInstance.getTourLength(incompleteInstance.incumbent))
            + " 0\n";
    BufferedWriter writer = new BufferedWriter(new FileWriter("./solutions/" + filename + ".sol"));
    writer.write(instanceHeader);
    // Serialize routes one-by-one.
    for (List<Integer> route : incompleteInstance.incumbent) {
      for (Integer customer : route) {
        writer.write(customer + " ");
      }
      writer.write("\n");
    }
    writer.close();

    // Output the instance string.
    System.out.println("{\"Instance\": \"" + filename +
        "\", \"Time\": " + String.format("%.2f", watch.getTime()) +
        ", \"Result\": " + String.format("%.2f",
        incompleteInstance.getTourLength(incompleteInstance.incumbent)) +
        ", \"Solution\": \"" + incompleteInstance.serializeRoutes(incompleteInstance.incumbent)
        + "\"}");
  }
}
