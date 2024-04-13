# Pathfinder

Pathfinder is a Constraint Programming (CP) model to solve a realistic vehicle routing problem as proposed in [2018 AIMMS/MOPTA Competition](http://coral.ie.lehigh.edu/~mopta/competition). 

Our solution, developed by Brown students, is selected as a [finalist in the competition](https://awards.cs.brown.edu/2021/08/11/brown-cs-team-takes-third-place-thirteenth-modeling-and-optimization-competition/). 

## Problem Specification 
At a high level, the goal is to serve a number of customers, `numCustomers`, using a number of vehicles, `numVehicles`, while minimizing the total operational cost. It combines service assignment, routing, and appointment scheduling together. This applies to various services  (health care, beauty care, banking, and more) offered to individuals in their homes, aiming to address specific critical operational decision problems. 

Stochasticity is an intrinsic property of the home service assignment, routing, and appointment scheduling (H-SARA) problem. The focus was on solving H-SARA under three key random factors: service duration, travel time, and customer cancellation. The ultimate goal is to develop an efficient and implementable method to solve H-SARA that home service providers will then use.

There exist several considerations, including: 

* For every customer, we know the neighboring locations, `neighborsOfLocation`, and the distance between customers, `distanceFromTo`.
* The travel time between locations, `timeFromTo,` is estimated based on historical data. 
* Similarly, each customer's demand is estimated based on historical data, `demandOfCustomer`.
* The capacity of each vehicle is identical, `maxCapacity`.
* There is a time window, `latestDeliveryTime`, for the last minute of delivery without penalty.
* There is a fixed cost of operating a vehicle, `costFixed`.
* There is a fixed cost for every mile travelled by a vehicle, `costPerMile`.
* There is a fixed cost for every mile travelled by a vehicle, `costPerHour`.
* Each unit dropped after 8AM at each retailer is subject to a fixed penalty for late shipping, `costLateItem`.
* Each excess unit dropped at a retailer is subject to a fixed penalty for excess item, `costExcessItem`.
* Conversely, each missing unit not delivered at a retailer is subject to a fixed penalty for missing item, `costMissingItem`.

## CP Solution via Decomposition 
The CP approach is used for routing, and the solution method decomposes the problem into two main steps. 

### 1. Finding Feasible Tours
The first model is formulated to [find all feasible tours](https://github.com/skadio/pathfinder/blob/master/src/vrp/Model.java#L290). This CP model is used to search for [all unique solutions](https://github.com/skadio/pathfinder/blob/master/src/vrp/Model.java#L350), which is saved as input to the second step. Here is an [example output](https://github.com/skadio/pathfinder/blob/master/data/valid_routes_unique.txt). This example has 416 unique valid tours to serve 10 customers, starting from the depot and ending in the depot; hence, the length of each tour is 12. 

### 2. Finding the Best Routes
The second model is formulated to [find the best routes](https://github.com/skadio/pathfinder/blob/master/src/vrp/Model.java#L45) for each vehicle to serve customers, minimizing the total cost, subject to several considerations, some listed above. The solution of the first model is treated as [valid tuples](https://github.com/skadio/pathfinder/blob/master/src/vrp/Model.java#L138), and a [table constraint](https://github.com/skadio/pathfinder/blob/master/src/vrp/Model.java#L143) specifies the allowed assignments for visits of each vehicle. 


