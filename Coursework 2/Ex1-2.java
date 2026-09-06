/*
PREAMBLE FOR COURSEWORK 2 – EXERCISE 1
_____________________________________________________________________________________________________________________________

Design Overview:
----------------
My controller implements a depth-first systematic search using two states: EXPLORE and BACKTRACK. The control logic
is built around the number of adjacent non-wall squares, obtained through my nonwallExits() method. This allows the
robot to correctly classify the current situation as a dead end (1 exit), corridor (2 exits), junction (3 exits), or
crossroad (4 exits), and delegate behaviour to one of the four controller methods: deadEnd(), corridor(), junction(),
and crossroad().

Use of passageExits and beenbeforeExits:
----------------------------------------
To ensure exploration is prioritised, passageExits() identifies exits that are still PASSAGE. At junctions and
crossroads, if there is at least one passage exit, the robot selects randomly among them; otherwise, it randomly
selects among all non-wall exits. beenbeforeExits() is used to detect whether a junction has been visited previously
during the same run: if fewer than two adjacent squares are BEENBEFORE, the junction is considered “new” and is
recorded in RobotData.

RobotData and Backtracking:
---------------------------
I implemented RobotData using an array of JunctionRecorder objects, each storing x-coordinate, y-coordinate, and the
arrivalHeading at first entry. This allows O(n) lookup through searchJunction(), where n is the number of recorded
junctions, which is acceptable since junctions are limited by maze size. When backtracking, if a junction has no
unexplored exits, the stored arrivalHeading is used to compute the opposite absolute direction, enabling the robot
to “backtrack through” the junction exactly as required.

Controller Structure, Efficiency and Repetition:
------------------------------------------------
exploreControl() and backtrackControl() share common helper methods (deadEnd, corridor, junction, crossroad), reducing
repetition and keeping behaviour consistent. junction() and crossroad() use the same internal decision logic through
choosePassageThenRandomExit(), which avoids duplicating the selection code. All the exit-counting methods iterate over
a fixed set of four directions, keeping them constant-time and efficient. State transitions (EXPLORE ↔ BACKTRACK) are
kept simple to minimise overhead.

Worst-Case Analysis:
---------------------
The maze can be modelled as a tree in the worst case. A depth-first search visits each edge at most twice (once going
forward, once backtracking). If there are F free squares, then the number of edges is at most F–1. Hence the worst-case
number of robot steps is O(F), specifically bounded above by approximately 2(F−1) plus a small constant for initialisation.
_______________________________________________________________________________________________________
Thank You!
*/


import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;

public class Ex1 {
    // Mode constants
    private static final int EXPLORE   = 1;
    private static final int BACKTRACK = 0;

    // Relative directions to consider
    private final int[] directions = {
        IRobot.AHEAD,
        IRobot.LEFT,
        IRobot.RIGHT,
        IRobot.BEHIND
    };

    private RobotData robotData = new RobotData();
    private int explorerMode = EXPLORE; // 1 = explore, 0 = backtrack
    private int pollRun = 0;            // counts how many times controlRobot has been called in this run
    private int stepNumber = 0;         // used to detect the very first move

    /**
     * Called by the maze environment when the Reset button is pressed.
     * Ensures the stored data and mode are reset appropriately.
     */
    public void reset() {
        robotData.resetJunctionCounter();
        explorerMode = EXPLORE;
        pollRun = 0;
        stepNumber = 0;
    }

    /**
     * Main controller entry point, called once per robot move.
     */
    public void controlRobot(IRobot robot) {
        // On the first move of the first run through a new maze, reset RobotData.
        if ((robot.getRuns() == 0) && (pollRun == 0)) {
            robotData = new RobotData();
            explorerMode = EXPLORE;
            stepNumber = 0;
        }

        if (explorerMode == EXPLORE) {
            exploreControl(robot);
        } else {
            backtrackControl(robot);
        }

        pollRun++;
        stepNumber++;
    }

    /**
     * Behaviour when the robot is exploring new territory.
     */
    private void exploreControl(IRobot robot) {
        int exits = nonwallExits(robot);
        int direction;

        if (exits == 1) {
            // Dead end: reverse and start backtracking, except at the very start.
            direction = deadEnd(robot);
            if (stepNumber > 0) {
                explorerMode = BACKTRACK;
            }
            robot.face(direction);
        } else if (exits == 2) {
            // Corridor or corner.
            direction = corridor(robot);
            robot.face(direction);
        } else if (exits == 3) {
            // Junction.
            if (beenbeforeExits(robot) < 2) {
                robotData.recordJunction(
                    robot.getLocation().x,
                    robot.getLocation().y,
                    robot.getHeading()
                );
            }
            direction = junction(robot);
            robot.face(direction);
        } else { // exits == 4
            // Crossroad.
            if (beenbeforeExits(robot) < 2) {
                robotData.recordJunction(
                    robot.getLocation().x,
                    robot.getLocation().y,
                    robot.getHeading()
                );
            }
            direction = crossroad(robot);
            robot.face(direction);
        }
    }

    /**
     * Behaviour when the robot is backtracking.
     */
    private void backtrackControl(IRobot robot) {
        int exits = nonwallExits(robot);
        int direction;

        if (exits <= 1) {
            // Still at a dead end while backtracking: keep going BEHIND.
            direction = deadEnd(robot);
            robot.face(direction);
        } else if (exits == 2) {
            // Corridor while backtracking: just follow the corridor.
            direction = corridor(robot);
            robot.face(direction);
        } else {
            // Junction or crossroad while backtracking.
            int passageCount = passageExits(robot);

            if (passageCount > 0) {
                // There is at least one unexplored exit here: resume exploring.
                explorerMode = EXPLORE;
                if (exits == 3) {
                    direction = junction(robot);
                } else {
                    direction = crossroad(robot);
                }
                robot.face(direction);
            } else {
                // No unexplored exits: backtrack through the junction.
                int x = robot.getLocation().x;
                int y = robot.getLocation().y;
                int arrivalHeading = robotData.searchJunction(x, y);

                // Compute opposite absolute heading and set it directly.
                int opposite = oppositeHeading(arrivalHeading);
                robot.setHeading(opposite);
            }
        }
    }

    /**
     * Handles dead ends. At the start square (stepNumber == 0), choose
     * the unique non-wall direction. Otherwise, reverse (BEHIND).
     *
     * @return a relative direction constant for robot.face()
     */
    private int deadEnd(IRobot robot) {
        if (stepNumber == 0) {
            // First move and start is a dead end: go along the only non-wall exit.
            for (int d : directions) {
                if (robot.look(d) != IRobot.WALL) {
                    return d;
                }
            }
            // Fallback (should never be reached).
            return IRobot.AHEAD;
        } else {
            // Normal dead end: turn around.
            return IRobot.BEHIND;
        }
    }

    /**
     * Handles corridors and corners: pick the non-wall exit that is not BEHIND.
     * @return a relative direction constant for robot.face()
     */
    private int corridor(IRobot robot) {
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL && d != IRobot.BEHIND) {
                return d;
            }
        }
        // Fallback (should not occur in a valid corridor).
        return IRobot.AHEAD;
    }

    /**
     * Handles junctions (3 non-wall exits) while exploring or resuming exploration.
     * @return a relative direction constant for robot.face()
     */
    private int junction(IRobot robot) {
        return choosePassageThenRandomExit(robot);
    }

    /**
     * Handles crossroads (4 non-wall exits) while exploring or resuming exploration.
     * @return a relative direction constant for robot.face()
     */
    private int crossroad(IRobot robot) {
        return choosePassageThenRandomExit(robot);
    }

    /**
     * Shared logic for junction and crossroad:
     * - If there are one or more PASSAGE exits, choose randomly among them.
     * - Otherwise, choose randomly among all non-wall exits.
     */
    private int choosePassageThenRandomExit(IRobot robot) {
        ArrayList<Integer> passageDirs = new ArrayList<>();
        ArrayList<Integer> exitDirs = new ArrayList<>();

        for (int d : directions) {
            int look = robot.look(d);
            if (look != IRobot.WALL) {
                exitDirs.add(d);
                if (look == IRobot.PASSAGE) {
                    passageDirs.add(d);
                }
            }
        }

        ArrayList<Integer> choices = passageDirs.isEmpty() ? exitDirs : passageDirs;
        int index = (int) (Math.random() * choices.size());
        return choices.get(index);
    }

    /**
     * Counts how many adjacent squares are not WALL.
     */
    private int nonwallExits(IRobot robot) {
        int count = 0;
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts how many adjacent squares are PASSAGE.
     */
    private int passageExits(IRobot robot) {
        int count = 0;
        for (int d : directions) {
            if (robot.look(d) == IRobot.PASSAGE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts how many adjacent squares are BEENBEFORE.
     */
    private int beenbeforeExits(IRobot robot) {
        int count = 0;
        for (int d : directions) {
            if (robot.look(d) == IRobot.BEENBEFORE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the opposite absolute heading for a given absolute heading.
     * If an unknown heading is passed, it is returned unchanged.
     */
    private int oppositeHeading(int heading) {
        switch (heading) {
            case IRobot.NORTH: return IRobot.SOUTH;
            case IRobot.SOUTH: return IRobot.NORTH;
            case IRobot.EAST:  return IRobot.WEST;
            case IRobot.WEST:  return IRobot.EAST;
            default:           return heading;
        }
    }
}

/**
 * Stores the coordinates and original arrival heading for a junction.
 */
class JunctionRecorder {
    public int x;
    public int y;
    public int arrivalHeading;

    public JunctionRecorder(int x, int y, int arrivalHeading) {
        this.x = x;
        this.y = y;
        this.arrivalHeading = arrivalHeading;
    }
}

/**
 * Stores all junction data for a single run.
 */
class RobotData {
    private static final int maxJunctions = 20000;
    private int junctionCounter;
    private JunctionRecorder[] junctions;

    public RobotData() {
        junctions = new JunctionRecorder[maxJunctions];
        junctionCounter = 0;
    }

    /**
     * Resets the junction counter when the environment Reset button is pressed.
     */
    public void resetJunctionCounter() {
        junctionCounter = 0;
    }

    /**
     * Records a junction's position and arrival heading unless already stored.
     */
    public void recordJunction(int x, int y, int arrivalHeading) {
        // Check if this junction has already been stored.
        for (int i = 0; i < junctionCounter; i++) {
            JunctionRecorder j = junctions[i];
            if (j.x == x && j.y == y) {
                return; // Already recorded.
            }
        }

        if (junctionCounter < maxJunctions) {
            junctions[junctionCounter] =
                new JunctionRecorder(x, y, arrivalHeading);
            printJunction(junctionCounter, x, y, arrivalHeading);
            junctionCounter++;
        }
    }

    /**
     * Returns the arrival heading for the junction at (x, y),
     * or -1 if this junction is unknown.
     */
    public int searchJunction(int x, int y) {
        for (int i = 0; i < junctionCounter; i++) {
            JunctionRecorder j = junctions[i];
            if (j.x == x && j.y == y) {
                return j.arrivalHeading;
            }
        }
        return -1;
    }

    /**
     * Outputs basic details about a stored junction for debugging.
     */
    private void printJunction(int index, int x, int y, int heading) {
        String headingStr = "";
        switch (heading) {
            case IRobot.NORTH: headingStr = "NORTH"; break;
            case IRobot.EAST:  headingStr = "EAST";  break;
            case IRobot.SOUTH: headingStr = "SOUTH"; break;
            case IRobot.WEST:  headingStr = "WEST";  break;
        }
        System.out.println(
            "Junction " + index + " (x=" + x + ",y=" + y + ") heading " + headingStr
        );
    }
}
