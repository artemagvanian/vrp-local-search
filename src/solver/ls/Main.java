package solver.ls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import solver.ls.data.Route;
import solver.ls.instances.SLSParams;
import solver.ls.instances.VRPInstanceSLS;
import solver.ls.utils.Timer;

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
    SLSParams params = new SLSParams();

    watch.start();
    VRPInstanceSLS incompleteInstance = new VRPInstanceSLS(input, watch, params);
    watch.stop();

    double excessCapacity = incompleteInstance.calculateExcessCapacity(
        incompleteInstance.incumbent);

    assert excessCapacity == 0;

    System.out.println(
        "Amount over capacity (expect it to be 0): " + excessCapacity);
    System.out.println("Average time per iteration (µs): " + String.format("%.2f",
        Math.pow(10, 6) * watch.getTime() / incompleteInstance.currentIteration));

    // Generate the solution files. Only replace the current one if the new solution is better.
    Scanner read;
    String fullFileName = "./solutions/" + filename + ".sol";
    double currentBest;

    try {
      read = new Scanner(new File(fullFileName));
      currentBest = read.nextDouble();
    } catch (FileNotFoundException e) {
      currentBest = Double.POSITIVE_INFINITY;
    }

    if (incompleteInstance.incumbent.length < currentBest) {
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
