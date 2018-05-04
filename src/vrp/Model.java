package vrp;

import ilog.cp.*;
import ilog.concert.*;

import java.util.HashMap;
import java.util.HashSet;

public class Model {
  // Ilog CP Solver
  IloCP cp;

  // Instance data
  Data data;

  // Decision Variables
  IloIntVar[][] visitVehicleStep; // [numVehicles][numSteps] the visit of each vehicle at each step
  IloIntVar[][] loadVehicleStep; // [numVehicles][numSteps-1] the load of each vehicle "after" each step
  IloNumVar[][] timeVehicleStep; // [numVehicles][numSteps-1] the arrival time of each visit, hence steps - 1
  IloIntVar numUsedVehicles; // total number of vehicles used
  IloNumVar costDrivingTotal; // total driving cost
  IloNumVar[] costDrivingVehicle; // [numVehicles] driving cost of each vehicle
  IloNumVar costLateTotal; // total cost of late deliveries
  IloNumVar[] costLateVehicle; // [numVehicles] cost of late delieveries for each vehicle
  IloNumVar costHourlyTotal; // total cost of hourly usage
  IloNumVar[] costHourlyVehicle; // [numVehicles] hourly cost of each vehicle
  IloNumVar costFixedTotal; // fixed usage cost
  IloNumVar costTotal; // total cost = driving + usage

  // Auxiliary Variables
  IloIntExpr[] isUsedVehicle; // [numVehicles] whether a vehicle is used
  IloIntExpr[][] loadDiffVehicleStep; // [numVehicles][numSteps-2] the load diff between two steps
  IloIntExpr[] dropOffCustomer; // [numCustomers] amount dropped at customers (ideally, this should meet demand)

  // Decision Variable used for generating valid tours in the second model
  IloIntVar[] visitStep; // [numSteps] the visit of a vehicle

  // Symmetry breaking for valid tour generation
  // Both torus 0-2-1-0 and 0-1-2-0 cover the same set of customers
  // Keep only the one with the lowest cost
  HashMap<HashSet<Integer>, Double> coveredToCost; // covered set of locations to route cost
  HashMap<HashSet<Integer>, String> coveredToRoute; // covered set of locations to route string

  // objective: (-) minimize, (0) satisfy, (+) fix the number of used vehicles
  public Model(Data _data, boolean isSymBreak, int objective) throws IloException {
    cp = new IloCP();
    data = _data;

    // Variable - I: Cost variables
    costDrivingTotal = cp.numVar(0, 10000);
    costDrivingVehicle = cp.numVarArray(data.numVehicles, 0, 10000);
    costLateTotal = cp.numVar(0, 10000);
    costLateVehicle = cp.numVarArray(data.numVehicles, 0, 10000);
    costHourlyTotal = cp.numVar(0, 10000);
    costHourlyVehicle = cp.numVarArray(data.numVehicles, 0, 10000);
    costFixedTotal = cp.numVar(0, 10000);
    costTotal = cp.numVar(0, 10000);

    // Variables - II: the visit of each vehicle at each step
    visitVehicleStep = new IloIntVar[data.numVehicles][data.numSteps];
    for (int v = 0; v < data.numVehicles; v++) {
      for (int s = 0; s < data.numSteps; s++) {
        // the first and the last step is depo
        if (s == 0)
          visitVehicleStep[v][s] = cp.intVar(0, 0);
        else if (s == data.numSteps - 1)
          visitVehicleStep[v][s] = cp.intVar(0, 0);
        else
          visitVehicleStep[v][s] = cp.intVar(0, data.numCustomers);
      }
    }

    // Variables - III: Load of each vehicle after each step
    loadVehicleStep = new IloIntVar[data.numVehicles][data.numSteps - 1]; // -1 because this is "after" step
    for (int v = 0; v < data.numVehicles; v++) {
      for (int s = 0; s < data.numSteps - 1; s++) // -1 because after each step
      {
        // the last one is zero because no load comes back to depo
        if (s == data.numSteps - 2)
          loadVehicleStep[v][s] = cp.intVar(0, 0);
        else
          loadVehicleStep[v][s] = cp.intVar(0, data.maxCapacity);
      }
    }

    // Variables - IV: Time of arrival of each vehicle at each visit, hence steps - 1
    timeVehicleStep = new IloNumVar[data.numVehicles][data.numSteps - 1];
    for (int v = 0; v < data.numVehicles; v++)
      timeVehicleStep[v] = cp.numVarArray(data.numSteps - 1, data.startTime, 1440);

    // Variables - V: The number of vehicles used
    numUsedVehicles = cp.intVar(data.lbNumVehicles, data.numVehicles);

    // Constraint - I: Vehicle is used and carries load if it leaves depo
    isUsedVehicle = new IloIntExpr[data.numVehicles];
    for (int v = 0; v < data.numVehicles; v++) {
      // vehicle is used if it leaves the depo
      IloConstraint leftDepo = cp.neq(visitVehicleStep[v][1], 0);
      IloConstraint hasLoad = cp.gt(loadVehicleStep[v][0], 1);
      isUsedVehicle[v] = leftDepo;

      // don't leave the depo with no load
      cp.add(cp.equiv(leftDepo, hasLoad));
    }

    // Constraint - II: Link isUsed to numUsed
    cp.add(cp.eq(numUsedVehicles, cp.sum(isUsedVehicle)));

    // Constraint - III: Link Load to Load Diff
    loadDiffVehicleStep = new IloIntExpr[data.numVehicles][data.numSteps - 2]; // -2 because this is "between" steps
    for (int v = 0; v < data.numVehicles; v++) {
      for (int s = 0; s < data.numSteps - 2; s++) // -2 because this is between steps
      {
        IloIntExpr loadBefore = loadVehicleStep[v][s];
        IloIntExpr loadAfter = loadVehicleStep[v][s + 1];
        loadDiffVehicleStep[v][s] = cp.diff(loadBefore, loadAfter);
        cp.add(cp.ge(loadDiffVehicleStep[v][s], 0));
      }
    }

    // Constraint - IV: No visit means no load (and vice-versa) and a visit means there is dropOff
    for (int v = 0; v < data.numVehicles; v++) {
      for (int s = 1; s < data.numSteps; s++) // start from 1, exclude first/depo step
      {
        IloConstraint noVisit = cp.eq(visitVehicleStep[v][s], 0);
        IloConstraint noLoad = cp.eq(loadVehicleStep[v][s - 1], 0); // -1 because s starts from 1
        cp.add(cp.equiv(noVisit, noLoad));

        if (s != data.numSteps - 1) {
          // But, if there is a visit (hence load), there must be some dropoff
          IloConstraint dropOff = cp.gt(loadDiffVehicleStep[v][s - 1], 0); // -1 because s starts from 1
          cp.add(cp.equiv(cp.not(noVisit), dropOff));
        }
      }
    }

    // Constraint - V: Routes should be valid tours (Table Constraint)
    IloIntTupleSet tours = cp.intTable(data.numSteps);
    for (int i = 0; i < data.validTours.length; i++)
      cp.addTuple(tours, data.validTours[i]);

    for (int v = 0; v < data.numVehicles; v++)
      cp.add(cp.allowedAssignments(visitVehicleStep[v], tours));

    // Constraint - VI: Amount dropped at each customer should match the demand
    dropOffCustomer = new IloIntExpr[data.numCustomers];
    for (int c = 0; c < data.numCustomers; c++) {
      // Amount dropped to this customer from each vehicle
      IloIntExpr[] dropFromVehicle = new IloIntExpr[data.numVehicles];
      for (int v = 0; v < data.numVehicles; v++) {
        // Amount dropped to this customer by this vehicle at each step
        IloIntExpr[] dropAtStep = new IloIntExpr[data.numSteps - 2]; // -2, no drop possible first/last step at depo
        for (int s = 1; s < data.numSteps - 1; s++) // skip first step (depo) and last step (depo)
        {
          IloIntExpr isVisitCustomer = cp.eq(visitVehicleStep[v][s], c + 1); // +1, c starts from 0 (customers are 1..J)
          dropAtStep[s - 1] = cp.prod(isVisitCustomer, loadDiffVehicleStep[v][s - 1]); // -1, s starts from 1
        }
        dropFromVehicle[v] = cp.sum(dropAtStep); // sum over steps
      }
      dropOffCustomer[c] = cp.sum(dropFromVehicle); // sum over vehicles

      // Demand should be dropped
      cp.add(cp.eq(dropOffCustomer[c], data.demandOfCustomer[c]));
    }
    // Redundant: sum of all drop offs = total demand
    cp.add(cp.eq(cp.sum(dropOffCustomer), data.totalDemand));

    // Constraint - VII: Symmetry breaking between vehicles, max visit first
    if (isSymBreak)
      for (int v = 0; v < data.numVehicles - 1; v++)
        cp.add(cp.ge(visitVehicleStep[v][1], visitVehicleStep[v + 1][1]));

    // Constraint - VIII: Link the route to miles and driving time
    for (int v = 0; v < data.numVehicles; v++) {
      IloNumExpr[] mileOfVisit = new IloNumExpr[data.numSteps - 1];
      for (int s = 0; s < data.numSteps - 1; s++) // for each edge/visit
      {
        IloIntExpr from = visitVehicleStep[v][s];
        IloIntExpr to = visitVehicleStep[v][s + 1];
        IloIntExpr flatIndex = cp.sum(cp.prod(from, data.numLocations), to);
        mileOfVisit[s] = cp.element(data.flatDistances, flatIndex);

        // time of this visit = time at previous stop + time it takes to get here
        IloNumExpr timeOfVisit = (s == 0) ? cp.sum(data.startTime, cp.element(data.flatTimes, flatIndex)) : cp.sum(timeVehicleStep[v][s - 1], cp.element(data.flatTimes, flatIndex));
        cp.add(cp.eq(timeVehicleStep[v][s], timeOfVisit));
      }
      cp.add(cp.eq(costDrivingVehicle[v], cp.prod(data.costPerMile, cp.sum(mileOfVisit))));
    }
    cp.add(cp.eq(costDrivingTotal, cp.sum(costDrivingVehicle)));

    // Constraint - X: Cost hourly
    for (int v = 0; v < data.numVehicles; v++) {
      // (lastTime - firstTime)/60.0 * hourlyCoust
      IloNumExpr hoursUsed = cp.quot(cp.diff(timeVehicleStep[v][data.numSteps - 2], data.startTime), 60);
      cp.add(cp.eq(costHourlyVehicle[v], cp.prod(data.costPerHour, hoursUsed)));
    }
    cp.add(cp.eq(costHourlyTotal, cp.sum(costHourlyVehicle)));

    // Constraint - XI: Cost of late deliveries
    for (int v = 0; v < data.numVehicles; v++) {
      IloNumExpr[] costOfLateVisit = new IloNumExpr[data.numSteps - 2]; // skip the visit back to depo
      for (int s = 0; s < data.numSteps - 2; s++) // for each edge/visit
      {
        IloConstraint isLate = cp.ge(timeVehicleStep[v][s], data.latestDeliveryTime);
        costOfLateVisit[s] = cp.prod(isLate, loadDiffVehicleStep[v][s]); // pay the price for every unit dropped late
      }
      cp.add(cp.eq(costLateVehicle[v], cp.prod(data.costLateItem, cp.sum(costOfLateVisit))));
    }
    cp.add(cp.eq(costLateTotal, cp.sum(costLateVehicle)));

    // Constraint - XII: Fixed usage cost
    cp.add(cp.eq(costFixedTotal, cp.prod(isUsedVehicle, data.costsFixed)));

    // Constraint - XIII: Total Cost: driving + hourly + late delivery + fixed usage
    IloNumExpr[] costs = new IloNumExpr[] { costDrivingTotal, costHourlyTotal, costLateTotal, costFixedTotal };
    cp.add(cp.eq(costTotal, cp.sum(costs)));

    // Objective
    if (objective < 0) {
      // Minimize the number of used vehicles
      cp.add(cp.minimize(numUsedVehicles));
    } else if (objective == 0) {
      // do nothing, just satisfy the model as is
    } else if (objective > 0) {
      // Fix the number of used trucks, don't exceed data.numVehicles
      cp.add(cp.eq(numUsedVehicles, Math.min(objective, data.numVehicles)));

      // Try to ship everything on time
      // cp.add(cp.eq(costLateTotal, 0));

      // cp.add(cp.minimize(costDrivingTotal));
      // cp.add(cp.le(costDrivingTotal, 70.0));

      // Another objective?: increase the number of zeros in the visitVS matrix
      // IloIntExpr[] numZeros = new IloIntExpr[data.numVehicles];
      // for(int v = 0; v < data.numVehicles; v++)
      // numZeros[v] = cp.count(visitVehicleStep[v], 0);
      // cp.add(cp.ge(cp.sum(numZeros), 81));
    }
  }

  public void solve() throws IloException {
    cp.setParameter(IloCP.IntParam.LogVerbosity, IloCP.ParameterValues.Quiet);
    if (cp.solve())
      printSolution();
    else
      System.out.println("Infeasible!");
  }

  private void printSolution() {
    System.out.println("numUsedVehicles: " + (int) cp.getValue(numUsedVehicles));
    System.out.println("costTotal: " + String.format("%.2f", cp.getValue(costTotal)) + " costDrivingTotal: "
        + String.format("%.2f", cp.getValue(costDrivingTotal)) + " costLateTotal: " + String.format("%.2f", cp.getValue(costLateTotal)) + " costHourlyTotal: "
        + String.format("%.2f", cp.getValue(costHourlyTotal)) + " costFixedTotal: " + String.format("%.2f", cp.getValue(costFixedTotal)) + "\n");

    for (int v = 0; v < data.numVehicles; v++) {
      System.out.print("Vehicle " + (v + 1) + " [" + String.format("%.2f", cp.getValue(costDrivingVehicle[v])) + " + "
          + String.format("%.2f", cp.getValue(costLateVehicle[v])) + " + " + String.format("%.2f", cp.getValue(costHourlyVehicle[v])) + "] " + "= ");

      for (int s = 0; s < data.numSteps; s++) {
        // the location visited at this step
        System.out.print((int) cp.getValue(visitVehicleStep[v][s]) + " ");

        // the time at this step
        if (s == 0) {
          System.out.print("@" + (int) data.startTime / 60 + ":00 ");
        } else {
          int hour = (int) cp.getValue(timeVehicleStep[v][s - 1]) / 60;
          double minute = cp.getValue(timeVehicleStep[v][s - 1]) - hour * 60;
          System.out.print("@" + hour + ":" + String.format("%.2f", minute) + " ");
        }

        // the load
        if (s == 0) {
          // For depo, printthe amount the load it leaves with
          System.out.print("{" + (int) cp.getValue(loadVehicleStep[v][s]) + "} -> ");
        } else if (s != data.numSteps - 1) {
          // For customer, print the amount delivered
          int before = (int) cp.getValue(loadVehicleStep[v][s - 1]);
          int after = (int) cp.getValue(loadVehicleStep[v][s]);
          int diff = before - after;
          System.out.print("[" + diff + "] -> ");
        }
      }
      System.out.println();
    }
  }

  // Second model to generate valid tours
  public Model(Data _data) throws IloException {
    coveredToCost = new HashMap<HashSet<Integer>, Double>();
    coveredToRoute = new HashMap<HashSet<Integer>, String>();

    cp = new IloCP();
    data = _data;

    // Change the cost of from depo to depo to something other than 0
    // So that we can differentiate staying at depo from pairs that don't have an edge/connected
    data.distanceFromTo[0][0] = 0.1;
    data.flatDistances[0] = data.distanceFromTo[0][0];

    // Variable - I: Cost variables
    costDrivingTotal = cp.numVar(0, 10000);

    // Variables - I: the visits of a tour
    visitStep = new IloIntVar[data.numSteps];
    for (int s = 0; s < data.numSteps; s++) {
      // the first and the last step is depo
      if (s == 0)
        visitStep[s] = cp.intVar(0, 0);
      else if (s == data.numSteps - 1)
        visitStep[s] = cp.intVar(0, 0);
      else
        visitStep[s] = cp.intVar(0, data.numCustomers);
    }

    // Constraint - I: Routes should be valid tours
    // visit each customer at most once
    for (int c = 0; c < data.numCustomers; c++)
      cp.add(cp.le(cp.count(visitStep, c + 1), 1)); // +1 because customers start from 1

    // If a step is to depo, then stay at depo
    for (int s = 1; s < data.numSteps - 2; s++)
      cp.add(cp.ifThen(cp.eq(visitStep[s], 0), cp.eq(visitStep[s + 1], 0)));

    IloNumExpr[] costOfVisit = new IloNumExpr[data.numSteps - 1];
    for (int s = 0; s < data.numSteps - 1; s++) // for each edge/visit
    {
      IloIntExpr from = visitStep[s];
      IloIntExpr to = visitStep[s + 1];
      IloIntExpr flatIndex = cp.sum(cp.prod(from, data.numLocations), to);
      costOfVisit[s] = cp.element(data.flatDistances, flatIndex);

      // Make sure every edge has a cost
      // That is, don't allow a zero cost edge (where distance pairs don't exists)
      cp.add(cp.not(cp.eq(costOfVisit[s], 0)));
    }
    cp.add(cp.eq(costDrivingTotal, cp.sum(costOfVisit)));

    // DFS with maximum inference on element
    cp.setParameter(IloCP.IntParam.SearchType, IloCP.ParameterValues.DepthFirst);
    cp.setParameter(IloCP.IntParam.ElementInferenceLevel, IloCP.ParameterValues.Extended);

    // Revert back the changes on data
    data.distanceFromTo[0][0] = 0.0;
    data.flatDistances[0] = data.distanceFromTo[0][0];
  }

  // Used to get all solutions the second model
  public void solveAll() throws IloException {
    cp.setParameter(IloCP.IntParam.LogVerbosity, IloCP.ParameterValues.Quiet);
    cp.startNewSearch();
    int numSol = 0;
    while (cp.next()) {
      numSol++;
      printSolution2();
      System.out.println();
    }
    cp.end();

    System.out.println("Total solutions: " + numSol);
    for (HashMap.Entry<HashSet<Integer>, String> entry : coveredToRoute.entrySet()) {
      HashSet<Integer> covered = entry.getKey();
      String route = entry.getValue();
      Double objective = coveredToCost.get(covered);
      for (Integer s : covered)
        System.out.print(s + " ");
      System.out.print(" - " + route + objective);
      System.out.println();
    }
  }

  // To print and store only best solutions
  private void printSolution2() {
    HashSet<Integer> covered = new HashSet<Integer>();
    for (int s = 0; s < data.numSteps; s++)
      covered.add((int) cp.getValue(visitStep[s]));

    StringBuffer buf = new StringBuffer();
    for (int s = 0; s < data.numSteps; s++)
      buf.append((int) cp.getValue(visitStep[s]) + " ");

    String route = buf.toString();
    Double objective = cp.getValue(costDrivingTotal);

    if (!coveredToRoute.containsKey(covered)) {
      coveredToRoute.put(covered, route);
      coveredToCost.put(covered, objective);
    } else {
      double currentObj = coveredToCost.get(covered);
      if (objective < currentObj) {
        coveredToRoute.put(covered, route);
        coveredToCost.put(covered, objective);
      }
    }

    for (Integer s : covered)
      System.out.print(s + " ");
    System.out.print(" - " + route + objective);
  }
}
