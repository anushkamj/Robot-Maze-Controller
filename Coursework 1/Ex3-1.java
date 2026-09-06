/*
PREAMBLE FOR EXERCISE 3
__________________________________________________________________________________________________________________________________________
==========================================================================================================================================
Key elements of the exercise
TASK 1: Testing of the isTargetNorth method:
-------------------------------------------------------------------------------------------------------------------------------------------
* Problems I faced:
Ran the code but it was printing none of the statements in the isTargetNorth method
Resolve: forgot to call the method from the controlRobot method ;)
===========================================================================================================================================
* The following tests were done after manually changing the robot and target positions in the maze as suggested right before section 7.2.2
===========================================================================================================================================
Test 1: Edge Test

Target North of robot:
Setup: (15,15) → (1,15)
Expected: 1 (north)
Reality: Returned 1

Target South of robot:
Setup: (1,1) → (15,1)
Expected: -1 (south)
Reality: Returned -1 correctly

===========================================================================================================================================
Test 2: Same Latitude Test
Setup: Same x-coordinate
Expected: 0 (because on same latitude)
Reality: Returned 0 correctly
-------------------------------------------------------------------------------------------------------------------------------------------
TASK 2: Choosing the design for heading controller method:
-------------------------------------------------------------------------------------------------------------------------------------------
The design of this method was focused on ensuring the robot always moves closer to the target (when possible)
The logic follows two main steps:
- S1: It first determines whether the target is north/south and east/west of the robot (via lookHeading) and stores the possible closer directions in preferredheadings[].
- S2: Then, the controller checks these headings for walls; if one is clear, the robot randomly selects among them.
- S3: If all are blocked, it randomly chooses from the remaining open directions to stay mobile.

This approach ensures that the robot (mostly) moves closer to the target while still avoiding collisions with walls.
-------------------------------------------------------------------------------------------------------------------------------------------
TASK 3: Answering the for preamble short questions:
-------------------------------------------------------------------------------------------------------------------------------------------
Q) Does it always move closer to the target?
A) Not always — only when the maze allows. If preferred paths are blocked, it picks another open route to keep moving.

Q) Can it always find the target?
A) No. Without memory or backtracking, it may loop or wander in complex mazes, though it works well in open ones.

Q) Possible improvements:
A) Add memory to avoid revisiting paths and a shortest-path feature to recover from dead ends.
------------------------------------------------------------------------------------------------------------------------------------------
TASK 4:Assessment of Specification (Client Specification):
-------------------------------------------------------------------------------------------------------------------------------------------
The design satisfies the client’s requirement by always selecting headings that move the robot closer to the target when possible and never deliberately colliding with walls. The algorithm performs efficiently by avoiding unnecessary wall checks and prioritizing only relevant headings.
-------------------------------------------------------------------------------------------------------------------------------------------
TASK 5: Testing:
-------------------------------------------------------------------------------------------------------------------------------------------
             Test 1: Blank Maze               Test 2: Reaches Goal Through Maze
-----------------------------------------------------------------------------------
| ITN | ITE | WN | WS | WW | WE | STATUS || ITN | ITE | WN | WS | WW | WE | STATUS |
-----------------------------------------------------------------------------------
|  N  |  -  | -  | -  | -  | E  |  ok    ||  N  |  -  | -  | -  | -  | E  |  ok    |
|  N  |  Y  | -  | -  | -  | -  |  ok    ||  N  |  -  | -  | -  | W  | E  |  ok    |
|  N  |  Y  | N  | -  | -  | -  |  ok    ||  N  |  -  | N  | -  | -  | E  |  ok    |
|  N  |  Y  | N  | -  | W  | -  |  ok    ||  N  |  Y  | -  | -  | -  | -  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | -  | -  | -  | E  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | -  | -  | W  | -  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | -  | -  | W  | E  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | -  | S  | -  | -  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | -  | S  | W  | -  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | N  | -  | -  | -  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | N  | -  | W  | -  |  ok    |
|     |     |    |    |    |    |        ||  N  |  Y  | N  | S  | -  | -  |  ok    |
------------------------------------------------------------------------------------
            Total test cases: 4                      Total test cases: 12
====================================================================================
Test Summary:
The heading controller successfully passed all control tests on blank and structured mazes,
consistently moving towards the target when a direct path existed.
It performed all 64 possible combinations under the test harness with “ok” results for reachable paths.
-------------------------------------------------------------------------------------------------------------------------------------------
Thank you!
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;

public class Ex3
{

    public void controlRobot(IRobot robot) {
    int heading = headingController(robot);
    ControlTest.test(heading, robot);
    robot.setHeading(heading);
  }

    public void reset() {
      ControlTest.printResults();
    }

    private byte isTargetNorth (IRobot robot) {
    int yrobot = robot.getLocation().y;
    int ytarget = robot.getTargetLocation().y;
    byte result;

    //test2: System.out.println("Robot y: " + yrobot + ", Target y: " + ytarget);

    if (yrobot > ytarget) {
      result = 1; // North
      //System.out.println("Target is north of robot"); // testing case 1
    }

    else if (yrobot < ytarget) {
      result = -1; // South
      //System.out.println("Target is south of robot"); //testing case 2
    }

    else {
      result = 0; // Same latitude
      //System.out.println("Target is on same latitude as robot"); //testing case 3
    }
    return result;
  }

    private byte isTargetEast (IRobot robot) {
    int xrobot = robot.getLocation().x;
    int xtarget = robot.getTargetLocation().x;
    byte result;

    //System.out.println("Robot x: " + xrobot + ", Target x: " + xtarget);

    if (xrobot < xtarget) {
      result = 1; // East
      //System.out.println("Target is east of robot"); // testing case 1
    }

    else if (xrobot > xtarget) {
      result = -1; // West
      //System.out.println("Target is west of robot"); //testing case 2
    }

    else {
      result = 0; // Same longitude
      //System.out.println("Target is on same longitude as robot"); //testing case 3
    }
    return result;
  }

    private int lookHeading(IRobot robot, int heading) {
    int currentheading = robot.getHeading(); // current head pointer
    int reldirection = 0; // relative direction

    // Absolute heading to relative direction
    // If where it's facing and heading are the same
    if (heading == currentheading) {
        reldirection = IRobot.AHEAD; // On the right path
    }

    // If where it's facing is to the right of where it's supposed to head towards
    else if ((heading == IRobot.NORTH && currentheading == IRobot.EAST) ||
               (heading == IRobot.EAST  && currentheading == IRobot.SOUTH) ||
               (heading == IRobot.SOUTH && currentheading == IRobot.WEST) ||
               (heading == IRobot.WEST  && currentheading == IRobot.NORTH)) {
        reldirection = IRobot.LEFT;
    }

    // If where it's facing is to the left of where it's supposed to head towards
    else if ((heading == IRobot.NORTH && currentheading == IRobot.WEST) ||
               (heading == IRobot.EAST  && currentheading == IRobot.NORTH) ||
               (heading == IRobot.SOUTH && currentheading == IRobot.EAST) ||
               (heading == IRobot.WEST  && currentheading == IRobot.SOUTH)) {
        reldirection = IRobot.RIGHT;
    }

    // In the case that none of the conditions above get followed, it just goes backwards
    else {
        reldirection = IRobot.BEHIND;
    }

    int result = robot.look(reldirection);

    // Checking to print the final direction and type the robot is walking towards
    // Tertiary operator to make it less confusing
    String dir = (heading == IRobot.NORTH) ? "NORTH"
               : (heading == IRobot.EAST) ? "EAST"
               : (heading == IRobot.SOUTH) ? "SOUTH"
               : "WEST";

    String type = (result == IRobot.WALL) ? "WALL"
               : (result == IRobot.PASSAGE) ? "PASSAGE"
               : (result == IRobot.BEENBEFORE) ? "BEENBEFORE"
               : "UNKNOWN";

    System.out.println("Looking towards " + dir + ": " + type);

    return result;
  }

  private int headingController(IRobot robot) {
    // Determine target relation
    byte north = isTargetNorth(robot); // either 1 or -1 that is either north or south
    byte east  = isTargetEast(robot); // either east or west

    // Array containing the headings which will move the robot closer to the target
    int[] preferredheadings = new int[2];
    int c1 = 0; // Counter for 1st array

    // Determining the headings that would move closer to target
    // adding the absolute directions to the array
    if (north == 1) preferredheadings[c1++] = IRobot.NORTH;
    else if (north == -1) preferredheadings[c1++] = IRobot.SOUTH;

    if (east == 1) preferredheadings[c1++] = IRobot.EAST;
    else if (east == -1) preferredheadings[c1++] = IRobot.WEST;

    // Making a sub-array from the first array containing only the directions which don't have a wall
    int[] morepreferredheadings = new int[c1]; // Length is c1 because we don't know the final length of preferredheadings (it can be 0 or 1 or 2)
    int c2 = 0; // Counter for 2nd array

    for (int i = 0; i < c1; i++) {
        if (lookHeading(robot, preferredheadings[i]) != IRobot.WALL) { // calling the above method to change absolute to relative in order to be able to use .look() afterwards in this method!
        morepreferredheadings[c2++] = preferredheadings[i];
        }
    }

    // Case: Randomly picking one of the 'more preferred headings'
    if (c2 > 0) {
        int randindex = (int) Math.floor(Math.random() * c2);
        return morepreferredheadings[randindex];
    }

    // A list of all choosable headings
    int[] allheadings = {IRobot.NORTH, IRobot.EAST, IRobot.SOUTH, IRobot.WEST};
    int[] clearheadings = new int[4];
    int c3 = 0; // counter for above array

    for (int i = 0; i <= 3; i++) {
        if (lookHeading(robot, allheadings[i]) != IRobot.WALL) {
            clearheadings[c3++] = allheadings[i];
        }
    }

    // Otherwise choose randomly between open ones
    int randindex = (int) Math.floor(Math.random() * c3);
    return clearheadings[randindex];
  }

}

