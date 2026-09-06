/*
PREAMBLE FOR COURSEWORK 2 – GRAND FINALE
________________________________________________________________________________________________________________________

Approach and route choice:
--------------------------
My GrandFinale robot uses a junction–based memory that is closest to Route A. For each junction (and the start
square) I store its (x,y) coordinates, the absolute heading the robot first arrived with, and a single preferred
exit heading that is later used on “perfect” runs. This lets the robot behave like my Ex1/Ex3 style backtracking
explorer on the first run, and then reuse the stored exit headings as a virtual map on later runs.

First run vs later runs:
------------------------
On the first run (detected using getRuns()==0 and an internal step counter) the robot starts in EXPLORE mode. It
performs a systematic depth-first search with backtracking: dead ends reverse, corridors never reverse, and at
junctions/crossroads it prefers PASSAGE exits, otherwise any safe non-WALL exit. Whenever the robot chooses a way
out of a junction or the start square, that absolute heading is recorded as the preferred exit for that location.
When a previously unseen maze is loaded (getRuns()==0 again with stepCounter==0), a fresh RobotData object is
created so old information cannot leak across mazes.

Learning and “perfect” runs:
----------------------------
On subsequent runs of the *same* maze (getRuns()>0 with stepCounter==0), the controller switches to ROUTE mode.
In this mode, when it reaches a stored junction or the start square it looks up the saved exit heading and directs
the robot straight along that path; corridors are followed in the usual way. If no exit heading is stored yet, or
if the stored heading would immediately reverse the current direction, the controller temporarily falls back to
exploration/backtracking to repair the route and then updates the stored exit. This means that each junction’s
exit heading converges towards a route that reaches the target without entering dead ends, giving a “perfect”
second (and subsequent) run for the mazes I tested.

Loopy mazes and multiple mazes:
-------------------------------
The exploration and backtracking phases reuse the same systematic search ideas as earlier exercises, so the robot
can cope with loopy mazes as long as they are solvable by depth-first search with backtracking. Because I always
create a new RobotData when getRuns()==0 and the step counter is zero, the robot correctly forgets old maps when
a new maze is generated, but keeps its learned route between repeats of the same maze.
______________________________________________________________________________________________________________________
Thank You!
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;

class JunctionRecorder {
    public int x;
    public int y;
    public int arrivalHeading;
    public int exitHeading = -1;   // preferred exit for route runs (absolute heading)
    public boolean isVisited = false;

    public JunctionRecorder(int x, int y, int arrivalHeading) {
        this.x = x;
        this.y = y;
        this.arrivalHeading = arrivalHeading;
    }
}

class RobotData {
    public ArrayList<JunctionRecorder> passedJunctions = new ArrayList<>();

    /**
     * Returns the junction stored at the specified coordinates, or null if none exist.
     */
    public JunctionRecorder searchJunction(int x, int y) {
        for (JunctionRecorder j : passedJunctions) {
            if (j.x == x && j.y == y) {
                return j;
            }
        }
        return null;
    }

    /**
     * Creates a new junction entry unless one already exists at that location.
     */
    public JunctionRecorder recordJunction(int x, int y, int heading) {

        JunctionRecorder j = searchJunction(x, y);
        if (j != null) {
            return j;
        }

        JunctionRecorder newJunction = new JunctionRecorder(x, y, heading);
        passedJunctions.add(newJunction);
        return newJunction;
    }

    /**
     * Stores the chosen exit direction for a known junction (coordinate lookup).
     */
    public void setExitHeading(int x, int y, int heading) {
        JunctionRecorder j = searchJunction(x, y);
        if (j != null) {
            j.exitHeading = heading;
        }
    }

    /**
     * Stores an exit direction for a specific junction reference.
     */
    public void setExitHeading(JunctionRecorder junction, int heading) {
        if (junction != null) {
            junction.exitHeading = heading;
        }
    }

    /**
     * Retrieves the recorded arrival heading for the junction at the given location.
     */
    public int getArrivalHeading(int x, int y) {
        JunctionRecorder j = searchJunction(x, y);
        return (j == null) ? -1 : j.arrivalHeading;
    }

    /**
     * Retrieves the saved exit heading for the junction at the given location.
     */
    public int getExitHeading(int x, int y) {
        JunctionRecorder j = searchJunction(x, y);
        return (j == null) ? -1 : j.exitHeading;
    }
}

enum RobotMode {
    EXPLORE,
    BACKTRACK,
    ROUTE
}

public class GrandFinale {
    private final static int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
    private RobotData robotData = new RobotData();
    private int stepCounter = 0;
    private RobotMode mode = RobotMode.EXPLORE;
    private int startX;
    private int startY;

    /**
     * Resets tracking data at the beginning of a run.
     * For repeat runs of the same maze, we keep RobotData so the robot can use
     * the learned route, but we always reset the step counter and mode.
     */
    public void reset() {
        stepCounter = 0;
        mode = RobotMode.EXPLORE;
    }

    /**
     * Prepares internal state when a new maze is detected.
     */
    private void newMaze() {
        robotData = new RobotData();   // forget old map
        mode = RobotMode.EXPLORE;
        stepCounter = 0;
    }

    /**
     * Executes movement logic every simulation step.
     */
    public void controlRobot(IRobot robot) {

        if (stepCounter == 0) {
            startX = robot.getLocation().x;
            startY = robot.getLocation().y;

            // First run in this maze → build a new map.
            if (robot.getRuns() == 0) {
                newMaze();
            } else {
                // Later runs in the same maze → follow stored route if possible.
                mode = RobotMode.ROUTE;
            }

            // Ensure the start square is always treated as a junction in the map.
            robotData.recordJunction(startX, startY, -1);
        }

        if (mode == RobotMode.EXPLORE) {
            explorerControl(robot);
        } else if (mode == RobotMode.BACKTRACK) {
            backtrackControl(robot);
        } else {
            routeControl(robot);
        }

        stepCounter++;
    }

    /**
     * Manages exploration behaviour on first pass through the maze.
     */
    private void explorerControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits <= 1) {

            if (stepCounter != 0) {
                mode = RobotMode.BACKTRACK;
            }

            deadEndControl(robot);

        } else if (exits > 2) {

            JunctionRecorder j = robotData.recordJunction(
                    robot.getLocation().x,
                    robot.getLocation().y,
                    robot.getHeading()
            );

            exploreJunctionControl(robot, j);
            // Record the exit we actually take from this junction.
            robotData.setExitHeading(j, robot.getHeading());

        } else { // corridor or corner

            // Special case: if the very first square behaves like a corridor,
            // still record the preferred exit for the start square.
            if (stepCounter == 0) {
                JunctionRecorder j = robotData.recordJunction(
                        robot.getLocation().x,
                        robot.getLocation().y,
                        -1
                );
                corridorControl(robot);
                robotData.setExitHeading(j, robot.getHeading());
                return;
            }

            corridorControl(robot);
        }
    }

    /**
     * Handles robot movement while retracing discovered pathways.
     */
    private void backtrackControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits <= 1) {
            deadEndControl(robot);

        } else if (exits > 2) {

            backtrackJunctionControl(robot);
            // Whatever heading we choose here is the preferred way out
            // of this junction for future route runs.
            robotData.setExitHeading(
                    robot.getLocation().x,
                    robot.getLocation().y,
                    robot.getHeading()
            );

        } else {

            corridorControl(robot);

            // If we happen to backtrack all the way to the start square,
            // update its exit heading as well.
            if (startX == robot.getLocation().x && startY == robot.getLocation().y) {
                robotData.setExitHeading(startX, startY, robot.getHeading());
            }
        }

        // If we discover new unexplored passages ahead, switch back to explore mode.
        if (robot.look(IRobot.AHEAD) == IRobot.PASSAGE) {
            mode = RobotMode.EXPLORE;
        }
    }

    /**
     * Follows stored exit headings to retrace the optimal path on later runs.
     */
    private void routeControl(IRobot robot) {
        int exits = nonwallExits(robot);

        if (exits <= 1) {
            // Should not normally happen on a perfect run, but be robust.
            deadEndControl(robot);

        } else if (exits > 2) {
            routeJunctionControl(robot);

        } else { // corridor or corner

            // At the start square on a route run, use the stored heading directly.
            if (startX == robot.getLocation().x && startY == robot.getLocation().y) {
                int heading = robotData.getExitHeading(startX, startY);
                if (heading != -1) {
                    robot.setHeading(heading);
                }
            }

            corridorControl(robot);
        }
    }

    /**
     * Handles junction logic during exploration.
     */
    private void exploreJunctionControl(IRobot robot, JunctionRecorder junction) {
        int passageExits = passageExits(robot);

        if (junction.isVisited) {

            robot.face(IRobot.BEHIND);
            mode = RobotMode.BACKTRACK;

        } else {

            junction.isVisited = true;

            ArrayList<Integer> passageList = new ArrayList<>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageList.add(d);
                }
            }

            if (passageList.size() == 0) {

                ArrayList<Integer> openList = new ArrayList<>();
                for (int d : directions) {
                    if (robot.look(d) != IRobot.WALL) {
                        openList.add(d);
                    }
                }

                robot.face(openList.get((int) (Math.random() * openList.size())));
                return;
            }

            int rand = (int) (Math.random() * passageList.size());
            robot.face(passageList.get(rand));
        }
    }

    /**
     * Handles junction navigation when backtracking.
     */
    private void backtrackJunctionControl(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits == 0) {

            int arrivalHeading = robotData.getArrivalHeading(
                    robot.getLocation().x,
                    robot.getLocation().y
            );
            int oppositeHeading = ((arrivalHeading + 2) % 4) + IRobot.NORTH;
            robot.setHeading(oppositeHeading);

        } else {

            mode = RobotMode.EXPLORE;

            ArrayList<Integer> passageList = new ArrayList<>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageList.add(d);
                }
            }

            int rand = (int) (Math.random() * passageList.size());
            robot.face(passageList.get(rand));
        }
    }

    /**
     * Handles route-following at junctions on non-first runs.
     */
    private void routeJunctionControl(IRobot robot) {
        int storedHeading = robotData.getExitHeading(
                robot.getLocation().x,
                robot.getLocation().y
        );

        // If we have no stored heading yet, or if it would immediately reverse,
        // fall back to exploration to repair the route.
        if (storedHeading == -1 ||
            ((robot.getHeading() - storedHeading + 4) % 4 == 2)) {

            mode = RobotMode.EXPLORE;

            JunctionRecorder j = robotData.recordJunction(
                    robot.getLocation().x,
                    robot.getLocation().y,
                    robot.getHeading()
            );

            junctionControl(robot);
            robotData.setExitHeading(j, robot.getHeading());
            return;
        }

        // Follow the learned preferred exit.
        robot.setHeading(storedHeading);
    }

    /**
     * Responds to dead ends by reversing unless at the first move.
     */
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

    /**
     * Moves forward through straight corridors by taking the only forward-valid exit.
     */
    private void corridorControl(IRobot robot) {
        for (int d : directions) {
            if (d != IRobot.BEHIND && robot.look(d) != IRobot.WALL) {
                robot.face(d);
                return;
            }
        }
    }

    /**
     * Picks a direction at a junction (used during route fallback).
     */
    private void junctionControl(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits == 0) {

            ArrayList<Integer> openList = new ArrayList<>();
            for (int d : directions) {
                if (robot.look(d) != IRobot.WALL) {
                    openList.add(d);
                }
            }

            robot.face(openList.get((int) (Math.random() * openList.size())));

        } else {

            ArrayList<Integer> passageList = new ArrayList<>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageList.add(d);
                }
            }

            robot.face(passageList.get((int) (Math.random() * passageList.size())));
        }
    }

    /**
     * Counts how many directions do NOT contain walls.
     */
    private int nonwallExits(IRobot robot) {
        int exits = 0;
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL) {
                exits++;
            }
        }
        return exits;
    }

    /**
     * Counts how many directions lead into unexplored passages.
     */
    private int passageExits(IRobot robot) {
        int exits = 0;
        for (int d : directions) {
            if (robot.look(d) == IRobot.PASSAGE) {
                exits++;
            }
        }
        return exits;
    }
}
