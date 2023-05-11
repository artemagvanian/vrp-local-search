package solver.ls.instances;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public abstract class VRPInstance {

  // VRP Input Parameters
  public int numCustomers;                 // the number of customers
  public int numVehicles;                  // the number of vehicles
  public int vehicleCapacity;              // the capacity of the vehicles
  public int[] demandOfCustomer;           // the demand of each customer
  public double[] xCoordOfCustomer;        // the x coordinate of each customer
  public double[] yCoordOfCustomer;        // the y coordinate of each customer
  public double[][] distances;             // distances between all customers
  public double[][] originalDistances;     // original distances between all customers


  protected VRPInstance(String fileName) {
    Scanner read = null;
    try {
      read = new Scanner(new File(fileName));
    } catch (FileNotFoundException e) {
      System.out.println("Error: in VRPInstance() " + fileName + "\n" + e.getMessage());
      System.exit(-1);
    }

    numCustomers = read.nextInt();
    numVehicles = read.nextInt();
    vehicleCapacity = read.nextInt();

    System.out.println("Number of customers: " + numCustomers);
    System.out.println("Number of vehicles: " + numVehicles);
    System.out.println("Vehicle capacity: " + vehicleCapacity);

    demandOfCustomer = new int[numCustomers];
    xCoordOfCustomer = new double[numCustomers];
    yCoordOfCustomer = new double[numCustomers];

    // all customers but the depot
    for (int i = 0; i < numCustomers; i++) {
      demandOfCustomer[i] = read.nextInt();
      xCoordOfCustomer[i] = read.nextDouble();
      yCoordOfCustomer[i] = read.nextDouble();
    }

    System.out.println("Parsed data (demand, x, y): ");
    for (int i = 0; i < numCustomers; i++) {
      System.out.println(
          demandOfCustomer[i] + " " + xCoordOfCustomer[i] + " " + yCoordOfCustomer[i]);
    }

    originalDistances = getDistances();
    distances = getDistances();
    normalizeDistances(distances);
  }

  private static double distance(double x1, double x2, double y1, double y2) {
    return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
  }

  private double[][] getDistances() {
    double[][] distances = new double[numCustomers][numCustomers];

    // Calculate distances.
    for (int i = 0; i < numCustomers; i++) {
      for (int j = 0; j < numCustomers; j++) {
        distances[i][j] = distance(xCoordOfCustomer[i], xCoordOfCustomer[j], yCoordOfCustomer[i],
            yCoordOfCustomer[j]);
      }
    }

    return distances;
  }

  private void normalizeDistances(double[][] distances) {
    double maxDistance = 0;
    for (int i = 0; i < numCustomers; i++) {
      for (int j = 0; j < numCustomers; j++) {
        if (maxDistance < distances[i][j]) {
          maxDistance = distances[i][j];
        }
      }
    }

    for (int i = 0; i < numCustomers; i++) {
      for (int j = 0; j < numCustomers; j++) {
        distances[i][j] *= 100;
        distances[i][j] /= maxDistance;
      }
    }
  }

  protected int bestTenureFromExperimentation(String fileName) {
    HashMap<String, Integer> bestTenureValues = new HashMap<>();
    bestTenureValues.put("input/5_4_10.vrp", 1);
    bestTenureValues.put("input/16_5_1.vrp", 5);
    bestTenureValues.put("input/21_4_1.vrp", 1);
    bestTenureValues.put("input/30_4_1.vrp", 12);
    bestTenureValues.put("input/41_14_1.vrp", 8);
    bestTenureValues.put("input/45_4_1.vrp", 13);
    bestTenureValues.put("input/51_5_1.vrp", 7);
    bestTenureValues.put("input/76_8_2.vrp", 8);
    bestTenureValues.put("input/101_8_1.vrp", 14);
    bestTenureValues.put("input/101_11_2.vrp", 10);
    bestTenureValues.put("input/121_7_1.vrp", 12);
    bestTenureValues.put("input/135_7_1.vrp", 40);
    bestTenureValues.put("input/151_15_1.vrp", 25);
    bestTenureValues.put("input/200_16_2.vrp", 16);
    bestTenureValues.put("input/241_22_1.vrp", 40);
    bestTenureValues.put("input/262_25_1.vrp", 15);
    bestTenureValues.put("input/386_47_1.vrp", 79);

    return bestTenureValues.get(fileName);
  }
}