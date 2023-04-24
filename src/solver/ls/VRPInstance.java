package solver.ls;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
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

  public List<List<Integer>> routes;       // routes for each of the trucks


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

    distances = getDistances();
  }

  private static double distance(double x1, double x2, double y1, double y2) {
    return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
  }

  private double[][] getDistances() {
    double[][] distances = new double[numCustomers][numCustomers];

    // Calculate distances.
    for (int i = 0; i < numCustomers; i++) {
      for (int j = 0; j < numCustomers; j++) {
        distances[i][j] = distance(xCoordOfCustomer[i], xCoordOfCustomer[j],
            yCoordOfCustomer[i], yCoordOfCustomer[j]);
      }
    }

    return distances;
  }

  // Serialize all routes into the required format.
  public String serializeRoutes() {
    // Add the vehicles that didn't go
    int excessVehicles = numVehicles - routes.size();
    for (int i = 0; i < excessVehicles; i++) {
      ArrayList<Integer> excess = new ArrayList<>();
      excess.add(0);
      excess.add(0);
      routes.add(excess);
    }

    System.out.println("Routes: " + routes.size());
    for (List<Integer> walk : routes) {
      for (int j : walk) {
        System.out.print(j + " ");
      }
      System.out.println();
    }

    // convert to a string
    List<Integer> flattenedList = new ArrayList<>();
    flattenedList.add(1); // NOTE: 1 HERE IF PROVED OPTIMAL, ELSE 0
    for (List<Integer> innerList : routes) {
      flattenedList.addAll(innerList);
    }
    StringBuilder sb = new StringBuilder();
    for (Integer number : flattenedList) {
      sb.append(number).append(" ");
    }

    return sb.toString().trim();
  }

  // Get tour length from the routes.
  public double getTourLength(List<List<Integer>> routes) {
    double totalTourLength = 0;
    for (List<Integer> route : routes) {
      for (int j = 0; j < route.size() - 1; j++) {
        totalTourLength += distances[route.get(j)][route.get(j + 1)];
      }
    }
    return totalTourLength;
  }

  // for each vehicle's route, check how much over capacity it is
  public double getAmtOverCapacity(List<List<Integer>> routes) {
    double overCapacity = 0;
    double capacity = 0;
    // for each vehicle
    for (List<Integer> route : routes) {
      capacity = 0;
      // calculate capacity
      for (int i : route) {
        capacity += demandOfCustomer[i];
      }
      // only if its over what it should be, add amount over
      if (vehicleCapacity < capacity){
        overCapacity += capacity - vehicleCapacity;
      }
    }
    return overCapacity;
  }
}