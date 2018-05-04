package vrp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class Data {
  // Problem input
  public int numVehicles;
  public int numCustomers; // customers are from 1 to n, 0 is for depo
  public int[] demandOfCustomer; // this can be learned from historical data
  public double[][] distanceFromTo; // [numCustomer+1][numCustomer+1] this is known
  public double[][] timeFromTo; // [numCustomer+1][numCustomer+1] this can be learned from historical data
  public double startTime; // the starting time to leave depo in minutes 0..1440
  public double latestDeliveryTime; // the last minute to deliver without penalty
  public int maxCapacity; // same capacity for every vehicle
  public double costFixed; // fixed cost per day, per vehicle M_F
  public double[] costsFixed; // fixed cost duplicated for each vehicle
  public double costPerMile; // cost per mile, m_v
  public double costPerHour; // cost per hour, m_t
  public double costLateItem; // penalty for each unit dropped after 8am at each retailer, c_m
  public double costExcessItem; // penalty for each excess unit at each retailer, c_h,
  public double costMissingItem; // penalty for each missing unit at each retailer, c_p
  public int[][] neighborsOfLocation; // [numLocation][numNeighbhors] the set of reachable location from a location

  // Read from a file, all possible valid tours
  public int[][] validTours;

  // Generated based on the input parameters
  public int numLocations; // numCustomers + 1 (depo)
  public int numSteps; // from depo + customers + to depo
  public int totalDemand; // the sum of all demand
  public int lbNumVehicles; // lower bound on the number of vehicles ceil(totalDemand/capacity)
  public double[] flatDistances; // [(numCustomer+1)^2] flat version of distanceFromTo
  public double[] flatTimes; // [(numCustomer+1)^2] flat version of timeFromTo

  public Data(String instance) {
    if (instance.equals("toy")) {
      numVehicles = 3;
      numCustomers = 3;
      maxCapacity = 9;
      demandOfCustomer = new int[] { 5, 5, 5 };
      startTime = 300; // day starts at 5:00 am
      latestDeliveryTime = 481; // deliver by 8:00 am

      // Penalty
      costFixed = 300;
      costPerMile = 2;
      costPerHour = 15;
      costLateItem = 10;
      costExcessItem = 0.5;
      costMissingItem = 1.0;

      // Enumerate all valid tours
      validTours = new int[][] { { 0, 0, 0, 0, 0 }, { 0, 1, 0, 0, 0 }, { 0, 2, 0, 0, 0 }, { 0, 3, 0, 0, 0 }, { 0, 1, 2, 0, 0 }, { 0, 1, 3, 0, 0 },
          { 0, 2, 1, 0, 0 }, { 0, 2, 3, 0, 0 }, { 0, 3, 1, 0, 0 }, { 0, 3, 2, 0, 0 }, { 0, 1, 2, 3, 0 }, { 0, 1, 3, 2, 0 }, { 0, 2, 1, 3, 0 },
          { 0, 2, 3, 1, 0 }, { 0, 3, 1, 2, 0 }, { 0, 3, 2, 1, 0 } };

      distanceFromTo = new double[numCustomers + 1][numCustomers + 1];
      distanceFromTo[0][1] = 10;
      distanceFromTo[0][2] = 20;
      distanceFromTo[0][3] = 30;

      distanceFromTo[1][0] = 10;
      distanceFromTo[1][2] = 12;
      distanceFromTo[1][3] = 13;

      distanceFromTo[2][0] = 20;
      distanceFromTo[2][1] = 12;
      distanceFromTo[2][3] = 23;

      distanceFromTo[3][0] = 30;
      distanceFromTo[3][1] = 13;
      distanceFromTo[3][2] = 23;

      timeFromTo = new double[numCustomers + 1][numCustomers + 1];
      timeFromTo[0][1] = 10;
      timeFromTo[0][2] = 20;
      timeFromTo[0][3] = 30;

      timeFromTo[1][0] = 10;
      timeFromTo[1][2] = 12;
      timeFromTo[1][3] = 13;

      timeFromTo[2][0] = 20;
      timeFromTo[2][1] = 12;
      timeFromTo[2][3] = 23;

      timeFromTo[3][0] = 30;
      timeFromTo[3][1] = 13;
      timeFromTo[3][2] = 23;
    } else if (instance.equals("mopta")) {
      // Read valid tours
      File file = new File("data/valid_routes_unique.txt");
      Scanner read = null;
      try {
        read = new Scanner(file);
        int numValidRoutes = read.nextInt();
        int tourLength = read.nextInt();

        validTours = new int[numValidRoutes][tourLength];
        for (int i = 0; i < validTours.length; i++)
          for (int j = 0; j < validTours[i].length; j++)
            validTours[i][j] = read.nextInt();
      } catch (FileNotFoundException e) {
        System.out.println("Error: valid tours file not found!");
      }
      numVehicles = 8;
      numCustomers = 10;
      maxCapacity = 360;
      startTime = 300; // day starts at 5:00am
      latestDeliveryTime = 481; // deliver by 8:00 am

      // TODO: learn demandOfCustomer from data
      demandOfCustomer = new int[] { 156, 131, 112, 116, 162, 151, 102, 183, 195, 128 };

      // TODO: learn timeFromTo from data
      timeFromTo = new double[numCustomers + 1][numCustomers + 1];
      timeFromTo[0][1] = 10.6;
      timeFromTo[0][2] = 32.08;
      timeFromTo[0][3] = 6.95;
      timeFromTo[0][5] = 27.64;
      timeFromTo[0][7] = 24.13;
      timeFromTo[0][8] = 18.42;
      timeFromTo[0][9] = 21.2;
      timeFromTo[0][10] = 19.0;

      timeFromTo[1][0] = 10.6;
      timeFromTo[1][2] = 20.32;
      timeFromTo[1][3] = 14.0;
      timeFromTo[1][5] = 22.0;
      timeFromTo[1][7] = 27.13;
      timeFromTo[1][8] = 26.25;
      timeFromTo[1][9] = 6.7;
      timeFromTo[1][10] = 24.79;

      timeFromTo[2][0] = 32.08;
      timeFromTo[2][1] = 20.32;
      timeFromTo[2][7] = 20.19;
      timeFromTo[2][9] = 16.35;
      timeFromTo[2][10] = 30.55;

      timeFromTo[3][0] = 6.95;
      timeFromTo[3][1] = 14.0;
      timeFromTo[3][5] = 22.36;
      timeFromTo[3][6] = 30.38;
      timeFromTo[3][8] = 18.98;
      timeFromTo[3][9] = 16.15;
      timeFromTo[3][10] = 27.65;

      timeFromTo[4][7] = 18.97;
      timeFromTo[4][8] = 18.34;
      timeFromTo[4][10] = 12.12;

      timeFromTo[5][0] = 27.64;
      timeFromTo[5][1] = 22.0;
      timeFromTo[5][3] = 22.36;
      timeFromTo[5][6] = 21.61;
      timeFromTo[5][9] = 25.9;

      timeFromTo[6][3] = 30.38;
      timeFromTo[6][5] = 21.61;

      timeFromTo[7][0] = 24.13;
      timeFromTo[7][1] = 27.13;
      timeFromTo[7][2] = 20.19;
      timeFromTo[7][4] = 18.97;
      timeFromTo[7][8] = 19.92;
      timeFromTo[7][10] = 10.48;

      timeFromTo[8][0] = 18.42;
      timeFromTo[8][1] = 26.25;
      timeFromTo[8][3] = 18.98;
      timeFromTo[8][4] = 18.34;
      timeFromTo[8][7] = 19.92;
      timeFromTo[8][10] = 8.01;

      timeFromTo[9][0] = 21.2;
      timeFromTo[9][1] = 6.7;
      timeFromTo[9][2] = 16.35;
      timeFromTo[9][3] = 16.15;
      timeFromTo[9][5] = 25.9;

      timeFromTo[10][0] = 19.0;
      timeFromTo[10][1] = 24.79;
      timeFromTo[10][2] = 30.55;
      timeFromTo[10][3] = 27.65;
      timeFromTo[10][4] = 12.12;
      timeFromTo[10][7] = 10.48;
      timeFromTo[10][8] = 8.01;

      // Penalty
      costFixed = 300;
      costPerMile = 2;
      costPerHour = 15;
      costLateItem = 10;
      costExcessItem = 0.5;
      costMissingItem = 1.0;

      // The graph is NOT fully connected (this is not used currently)
      neighborsOfLocation = new int[numCustomers + 1][];
      neighborsOfLocation[0] = new int[] { 0, 1, 2, 3, 5, 7, 8, 9, 10 }; // include self, depo can be followed by depo
      neighborsOfLocation[1] = new int[] { 0, 2, 3, 5, 7, 8, 9, 10 };
      neighborsOfLocation[2] = new int[] { 0, 1, 7, 9, 10 };
      neighborsOfLocation[3] = new int[] { 0, 1, 5, 6, 8, 9, 10 };
      neighborsOfLocation[4] = new int[] { 7, 8, 10 };
      neighborsOfLocation[5] = new int[] { 0, 1, 3, 6, 9 };
      neighborsOfLocation[6] = new int[] { 3, 5 };
      neighborsOfLocation[7] = new int[] { 0, 1, 2, 4, 8, 10 };
      neighborsOfLocation[8] = new int[] { 0, 1, 3, 4, 7, 10 };
      neighborsOfLocation[9] = new int[] { 0, 1, 2, 3, 5 };
      neighborsOfLocation[10] = new int[] { 0, 1, 2, 3, 4, 7, 8 };

      distanceFromTo = new double[numCustomers + 1][numCustomers + 1];
      distanceFromTo[0][0] = 0.0;
      distanceFromTo[0][1] = 1.96468827043885;
      distanceFromTo[0][2] = 5.0643007760336936;
      distanceFromTo[0][3] = 1.1854219150394074;
      distanceFromTo[0][5] = 4.907218491015141;
      distanceFromTo[0][7] = 4.640221721305585;
      distanceFromTo[0][8] = 2.6596014859808714;
      distanceFromTo[0][9] = 3.080543715187625;
      distanceFromTo[0][10] = 3.585117510979431;

      distanceFromTo[1][0] = 1.96468827043885;
      distanceFromTo[1][2] = 3.608830236783567;
      distanceFromTo[1][3] = 2.6400831314914046;
      distanceFromTo[1][5] = 4.921232308870278;
      distanceFromTo[1][7] = 4.717041635672212;
      distanceFromTo[1][8] = 4.120651436973349;
      distanceFromTo[1][9] = 1.2596981846086392;
      distanceFromTo[1][10] = 4.483110549310852;

      distanceFromTo[2][0] = 5.0643007760336936;
      distanceFromTo[2][1] = 3.608830236783567;
      distanceFromTo[2][7] = 3.8125014837397604;
      distanceFromTo[2][9] = 3.7610074859206524;
      distanceFromTo[2][10] = 5.097154605916928;

      distanceFromTo[3][0] = 1.1854219150394074;
      distanceFromTo[3][1] = 2.6400831314914046;
      distanceFromTo[3][5] = 3.9603850053646843;
      distanceFromTo[3][6] = 4.2284009238396125;
      distanceFromTo[3][8] = 3.453934890729809;
      distanceFromTo[3][9] = 3.4480499930985538;
      distanceFromTo[3][10] = 4.603146042889638;

      distanceFromTo[4][7] = 2.8705173286733583;
      distanceFromTo[4][8] = 3.270348339390213;
      distanceFromTo[4][10] = 2.2167807986842525;

      distanceFromTo[5][0] = 4.907218491015141;
      distanceFromTo[5][1] = 4.921232308870278;
      distanceFromTo[5][3] = 3.9603850053646843;
      distanceFromTo[5][6] = 4.412446144735891;
      distanceFromTo[5][9] = 4.571665408744175;

      distanceFromTo[6][3] = 4.2284009238396125;
      distanceFromTo[6][5] = 4.412446144735891;

      distanceFromTo[7][0] = 4.640221721305585;
      distanceFromTo[7][1] = 4.717041635672212;
      distanceFromTo[7][2] = 3.8125014837397604;
      distanceFromTo[7][4] = 2.8705173286733583;
      distanceFromTo[7][8] = 3.351784281672397;
      distanceFromTo[7][10] = 1.9870721685071835;

      distanceFromTo[8][0] = 2.6596014859808714;
      distanceFromTo[8][1] = 4.120651436973349;
      distanceFromTo[8][3] = 3.453934890729809;
      distanceFromTo[8][4] = 3.270348339390213;
      distanceFromTo[8][7] = 3.351784281672397;
      distanceFromTo[8][10] = 1.4754000196183448;

      distanceFromTo[9][0] = 3.080543715187625;
      distanceFromTo[9][1] = 1.2596981846086392;
      distanceFromTo[9][2] = 3.7610074859206524;
      distanceFromTo[9][3] = 3.4480499930985538;
      distanceFromTo[9][5] = 4.571665408744175;

      distanceFromTo[10][0] = 3.585117510979431;
      distanceFromTo[10][1] = 4.483110549310852;
      distanceFromTo[10][2] = 5.097154605916928;
      distanceFromTo[10][3] = 4.603146042889638;
      distanceFromTo[10][4] = 2.2167807986842525;
      distanceFromTo[10][7] = 1.9870721685071835;
      distanceFromTo[10][8] = 1.4754000196183448;
    }

    // Generate from parameters
    numLocations = numCustomers + 1; // +1 because add the depo
    numSteps = numCustomers + 2; // +2 because depo_start + customers + depo_finish

    costsFixed = new double[numVehicles];
    for (int v = 0; v < numVehicles; v++)
      costsFixed[v] = costFixed;

    // Total demand from all customers
    totalDemand = 0;
    for (int c = 0; c < numCustomers; c++)
      totalDemand += demandOfCustomer[c];

    // Lower bound on the number of vehicles
    lbNumVehicles = (int) Math.ceil(totalDemand / (double) maxCapacity);

    // Flatten distances to use for element array indexing when calculating distance cost
    int index = 0;
    flatDistances = new double[numLocations * numLocations];
    for (int i = 0; i < numLocations; i++)
      for (int j = 0; j < numLocations; j++)
        flatDistances[index++] = distanceFromTo[i][j];

    // Flatten distances to use for element array indexing when calculating time cost
    index = 0;
    flatTimes = new double[numLocations * numLocations];
    for (int i = 0; i < numLocations; i++)
      for (int j = 0; j < numLocations; j++)
        flatTimes[index++] = timeFromTo[i][j];
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("numVehicles: " + numVehicles + "\n");
    buf.append("numCustomers: " + numCustomers + "\n");
    buf.append("maxCapacity: " + maxCapacity + "\n");
    buf.append("demandOfCustomer: " + Arrays.toString(demandOfCustomer) + "\n");
    buf.append("totalDemand: " + totalDemand + "\n");
    buf.append("lbNumVehicles: " + lbNumVehicles + "\n");
    buf.append("flatDistances: " + Arrays.toString(flatDistances) + "\n");
    return buf.toString();
  }
}
