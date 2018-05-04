package vrp;

import ilog.concert.IloException;

public class Test {
  public static void main(String[] args) throws IloException {
    // This is used to generate all valid tours
    // generateValidTours();

    // Problem instance "toy", "mopta" etc.
    Data data = new Data("mopta");
    System.out.println(data);

    // Constraint Model for Vehicle Routing
    boolean isSymBreak = false;
    int objective = -1; // (-) minimize, (0) satisfy, (+) limit vehicles with this number
    Model vrp = new Model(data, isSymBreak, objective);

    // Solution
    Timer timer = new Timer();
    timer.start();
    vrp.solve();
    timer.stop();
    System.out.println("\nTime: " + timer.getTime());
  }

  static void generateValidTours() throws IloException {
    // Problem instance "toy", "mopta" etc.
    Data data = new Data("mopta");
    System.out.println(data);

    // Constraint Model for Valid Tour Generation
    Model tours = new Model(data);

    // Solution
    Timer timer = new Timer();
    timer.start();
    tours.solveAll(); // find all valid tours (this can include duplicates!)
    timer.stop();
    System.out.println("\nTime: " + timer.getTime());
  }
}