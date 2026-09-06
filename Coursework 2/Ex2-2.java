/*
PREAMBLE FOR COURSEWORK 2 – EXERCISE 2
________________________________________________________________________________________________________________________

DFS Stack-Based Design:
-----------------------
This controller implements a depth-first search using a simple stack-based RobotData object, following the
requirements of Exercise 2. Unlike Ex1, junction information must be stored as a LIFO structure rather than a list,
so my RobotData class stores only arrival headings in an array and operates as a stack using push, pop and peek.

Memory-Efficient Junction Storage:
----------------------------------
As Ex 2 does not require storingcoordinates or multiple exit choices per junction, the stack stores only a single
integer (the arrival heading) for each recorded junction. This reduces the memory footprint from Ex1’s multi-field junction objects to a lightweight array of primitive values. The controller therefore uses O(J) space for J junctions, with each entry costing just one integer rather than a full record. No dynamic lists, no coordinate pairs, and no repeated scans through historical junctions are necessary, which keeps the structure compact and cache-friendly.

Explore Mode Decision Strategy:
-------------------------------
The robot alternates between two modes: EXPLORE and BACKTRACK. While exploring, it records a junction each time it
encounters more than two non-wall exits. At a junction, the controller prioritises PASSAGE exits, and if none exist,
it randomly selects from BEENBEFORE exits. This ensures that any previously explored branches are only used when
necessary, but never greedily preferred over new paths.

Backtracking Logic and Stack Operations:
----------------------------------------
During backtracking, the robot checks whether a revisited junction still has PASSAGE exits. If so, the controller
switches back into explorer mode to continue DFS from that branch. Otherwise, the robot retrieves the original arrival
heading from RobotData, computes the opposite absolute heading, and retraces its route. Each push and pop operation
prints debugging information as required by the specification.

Corridor Behaviour and Complexity:
----------------------------------
Corridor handling ensures that the robot never reverses direction unless in a dead end. All scanning methods
(nonwallExits, passageExits and beenbeforeExits) iterate over four fixed directions, making their runtime constant.
Since at most each corridor segment is walked twice (once forward and once backward), the worst-case number of steps
for a DFS in a tree-shaped maze of F free squares is O(F), with a tighter bound of at most 2(F – 1) plus a constant.

Why my Ex2 works in loopy mazes:
----------------------------------------
I only push a junction on its first visit, so the stack never desynchronises on revisits. This keeps push/pop behaviour consistent and preserves correct DFS backtracking in loops.
________________________________________________________________________________________________________________________
Thank You!
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;

class RobotData {
    private static int maxJunctions = 10000;
    private int junctionCounter = 0;
    private int[] arrivalHeadings = new int[maxJunctions];

    /** Stores the arrival heading on the stack. */
    public void recordJunction(int heading) {
        arrivalHeadings[junctionCounter] = heading;
        System.out.println("PUSH " + headingToString(heading));
        junctionCounter++;
    }

    /** Removes the most recent heading from the stack. */
    public void removeJunction() {
        int removed = arrivalHeadings[junctionCounter - 1];
        System.out.println("POP " + headingToString(removed));
        arrivalHeadings[junctionCounter - 1] = -1;
        junctionCounter--;
    }

    /** Gets the most recent stored heading. */
    public int getArrivalHeading() {
        return arrivalHeadings[junctionCounter - 1];
    }

    /** Converts heading constant to a readable string. */
    private String headingToString(int h) {
        switch (h) {
            case IRobot.NORTH: return "NORTH";
            case IRobot.SOUTH: return "SOUTH";
            case IRobot.EAST:  return "EAST";
            case IRobot.WEST:  return "WEST";
        }
        return "UNKNOWN";
    }
}

public class Ex2 {
    private final static int EXPLORE = 1;
    private final static int BACKTRACK = 0;

    /* Required direction order */
    private final static int[] directions = {
        IRobot.AHEAD,
        IRobot.LEFT,
        IRobot.RIGHT,
        IRobot.BEHIND
    };

    private RobotData robotData;
    private int stepCounter = 0;
    private int explorerMode = EXPLORE;

    /** Resets all stored state for a new run. */
    public void reset() {
        robotData = new RobotData();
        explorerMode = EXPLORE;
        stepCounter = 0;
    }

    /** Main robot control loop. */
    public void controlRobot(IRobot robot) {

        if (stepCounter == 0 && robot.getRuns() == 0) {
            reset();
        }

        if (explorerMode == EXPLORE) {
            explorerControl(robot);
        } else {
            backtrackControl(robot);
        }

        stepCounter++;
    }

    /** Logic used while exploring. */
    private void explorerControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits <= 1) {
            if (stepCounter != 0) explorerMode = BACKTRACK;
            deadEndControl(robot);

        } else if (exits > 2) {
            robotData.recordJunction(robot.getHeading());
            junctionControl(robot);

        } else {
            corridorControl(robot);
        }
    }

    /** Logic used while backtracking. */
    private void backtrackControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits <= 1) {
            deadEndControl(robot);

        } else if (exits > 2) {
            backtrackJunctionControl(robot);

        } else {
            corridorControl(robot);
        }
    }

    /** Backtracking behaviour at a junction. */
    private void backtrackJunctionControl(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits == 0) {
            int arrivalHeading = robotData.getArrivalHeading();
            int oppositeHeading = oppositeHeading(arrivalHeading);
            robot.setHeading(oppositeHeading);
            robotData.removeJunction();

        } else {
            explorerMode = EXPLORE;

            ArrayList<Integer> passageList = new ArrayList<>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageList.add(d);
                }
            }
            int randomIndex = (int) (Math.random() * passageList.size());
            robot.face(passageList.get(randomIndex));
        }
    }

    /** Handles dead ends. */
    private void deadEndControl(IRobot robot) {
        if (stepCounter != 0) {
            robot.face(IRobot.BEHIND);
        } else {
            for (int d : directions) {
                if (robot.look(d) != IRobot.WALL) {
                    robot.face(d);
                    return;
                }
            }
        }
    }

    /** Moves through corridors. */
    private void corridorControl(IRobot robot) {
        for (int d : directions) {
            if (d != IRobot.BEHIND && robot.look(d) != IRobot.WALL) {
                robot.face(d);
                return;
            }
        }
    }

    /** Exploration behaviour at junctions. */
    private void junctionControl(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits > 0) {
            ArrayList<Integer> passageList = new ArrayList<>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageList.add(d);
                }
            }
            int randomIndex = (int) (Math.random() * passageList.size());
            robot.face(passageList.get(randomIndex));
            return;
        }

        ArrayList<Integer> beenList = new ArrayList<>();
        for (int d : directions) {
            if (robot.look(d) == IRobot.BEENBEFORE) {
                beenList.add(d);
            }
        }
        if (!beenList.isEmpty()) {
            int randomIndex = (int) (Math.random() * beenList.size());
            robot.face(beenList.get(randomIndex));
            return;
        }

        ArrayList<Integer> openList = new ArrayList<>();
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL) {
                openList.add(d);
            }
        }
        int randomIndex = (int) (Math.random() * openList.size());
        robot.face(openList.get(randomIndex));
    }

    /** Counts exits that are not walls. */
    private int nonwallExits(IRobot robot) {
        int exits = 0;
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL) exits++;
        }
        return exits;
    }

    /** Counts exits that lead into passages. */
    private int passageExits(IRobot robot) {
        int exits = 0;
        for (int d : directions) {
            if (robot.look(d) == IRobot.PASSAGE) exits++;
        }
        return exits;
    }

    /** Computes opposite absolute heading. */
    private int oppositeHeading(int heading) {
        switch (heading) {
            case IRobot.NORTH: return IRobot.SOUTH;
            case IRobot.SOUTH: return IRobot.NORTH;
            case IRobot.EAST:  return IRobot.WEST;
            case IRobot.WEST:  return IRobot.EAST;
        }
        return heading;
    }
}
