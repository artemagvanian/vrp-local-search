package solver.ls;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import solver.ls.data.Route;
import solver.ls.instances.VRPInstanceIncomplete;
import solver.ls.utils.Timer;
import java.util.Scanner;
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

    final int maxIterations = 10000;

    Timer watch = new Timer();
    watch.start();
    VRPInstanceIncomplete incompleteInstance = new VRPInstanceIncomplete(input, maxIterations, watch);
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

    double amtOverCapacity = incompleteInstance.calculateExcessCapacity(
            incompleteInstance.incumbent);

    System.out.println(
        "Amount over capacity (expect it to be 0): " + amtOverCapacity);
    System.out.println("Average time per iteration (µs): " + String.format("%.2f",
        Math.pow(10, 6) * watch.getTime() / maxIterations));

    // Generate the solution files.
    // only replace the current one if the new solution is better
    Scanner read;
    String fullFileName = "./solutions/" + filename + ".sol";
    double currentBest = Double.MAX_VALUE;

    try {
      read = new Scanner(new File(fullFileName));
      currentBest = read.nextDouble();
    } catch (FileNotFoundException ignored) {
    }

    if (incompleteInstance.incumbent.length < currentBest && amtOverCapacity == 0) {
      String instanceHeader =
              String.format("%.2f", incompleteInstance.incumbent.length) + " 0\n";
      BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName));
      writer.write(instanceHeader);
      // Serialize routes one-by-one.
      for (Route route : incompleteInstance.incumbent.routes) {
        for (Integer customer : route.customers) {
          writer.write(customer + " ");
        }
        writer.write("\n");
      }
      writer.close();
    }

    // Output the instance string.
    System.out.println(
        "{\"Instance\": \"" + filename + "\", \"Time\": " + String.format("%.2f", watch.getTime())
            + ", \"Result\": " + String.format("%.2f", incompleteInstance.incumbent.length)
            + ", \"Solution\": \"" + incompleteInstance.serializeRoutes(
            incompleteInstance.incumbent) + "\"}");
  }
}
