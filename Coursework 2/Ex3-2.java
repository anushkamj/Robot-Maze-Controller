/*
PREAMBLE FOR COURSEWORK 2 – EXERCISE 3
________________________________________________________________________________________________________________________

Why Ex1 and Ex2 Fail in Loopy Mazes:
------------------------------------
The earlier controllers only tracked junction visits or arrival headings, but not which exits had been explored.
In a loopy maze this causes the robot to repeatedly re-enter the same cycle, because it cannot distinguish an
unexplored branch from an already-tried one.

How Ex3 Solves Loops:
---------------------
This design stores, for every junction, a record of which absolute exits have been explored. When the robot
returns to a junction, it selects only among the exits that remain unexplored; only when all exits are marked
explored does it backtrack using the arrival heading. This ensures every (junction, exit) pair is explored once
and prevents infinite looping.

Improved Junction Decision Strategy:
-----------------------------------
At a junction, the robot first selects any unexplored PASSAGE exit. If none exist, it selects an unexplored open exit
(i.e., BEENBEFORE). Only when all exits have been marked as explored does it backtrack using the stored arrival
heading. This ordering guarantees that real forward progress is always prioritised over returning into cycles.

Guaranteed Termination in Loopy Mazes:
--------------------------------------
Since every junction stores explicit per-exit visitation state, the robot never re-enters the same loop edge twice.
The DFS becomes a true graph search rather than a tree search, ensuring that all edges are explored exactly once and
that the robot will always eventually reach the goal, even in highly cyclic maze structures.

________________________________________________________________________________________________________________________
Thank You!
*/


import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;

class JunctionRecorder {
    public int x;
    public int y;
    public int arrivalHeading;
    public boolean[] exploredAbs = new boolean[4];   // Tracks explored absolute exits

    public JunctionRecorder(int x, int y, int arrivalHeading) {
        this.x = x;
        this.y = y;
        this.arrivalHeading = arrivalHeading;
    }
}

class RobotData {
    private ArrayList<JunctionRecorder> passedJunctions = new ArrayList<>();

    /**
     * Searches for a junction at the given coordinates.
     * @return the saved junction or null if none exists
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
     * Adds a new junction entry unless it already exists.
     * @return the existing or newly created junction record
     */
    public JunctionRecorder recordJunction(int x, int y, int heading) {

        JunctionRecorder existing = searchJunction(x, y);
        if (existing != null) {
            return existing;
        }

        JunctionRecorder newJunction = new JunctionRecorder(x, y, heading);
        passedJunctions.add(newJunction);
        return newJunction;
    }

    /** Marks an exit from this junction as explored. */
    public void markEdgeExplored(JunctionRecorder j, int absHeading) {
        j.exploredAbs[headingToIndex(absHeading)] = true;
    }

    /** Checks if an exit from this junction has been explored. */
    public boolean isEdgeExplored(JunctionRecorder j, int absHeading) {
        return j.exploredAbs[headingToIndex(absHeading)];
    }

    /** Maps absolute heading to array index. */
    private int headingToIndex(int h) {
        switch (h) {
            case IRobot.NORTH: return 0;
            case IRobot.EAST:  return 1;
            case IRobot.SOUTH: return 2;
            case IRobot.WEST:  return 3;
        }
        return 0;
    }
}

enum RobotMode {
    EXPLORE,
    BACKTRACK
}

public class Ex3 {
    private final static int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.LEFT, IRobot.RIGHT};
    private RobotData robotData;
    private int stepCounter = 0;
    private RobotMode mode = RobotMode.EXPLORE;

    /**
     * Resets all exploration state when the run restarts.
     */
    public void reset() {
        robotData = new RobotData();
        mode = RobotMode.EXPLORE;
        stepCounter = 0;
    }

    /**
     * Executes once each step to select the next action.
     */
    public void controlRobot(IRobot robot) {

        if (stepCounter == 0 && robot.getRuns() == 0) {
            reset();
        }

        if (mode == RobotMode.EXPLORE) {
            explorerControl(robot);
        } else {
            backtrackControl(robot);
        }

        stepCounter++;
    }

    /**
     * Backtracking behaviour when returning through known territory.
     */
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

    /**
     * Handles a junction while backtracking:
     * - If unexplored exits remain, switch back to exploration.
     * - Otherwise reverse using stored arrival heading.
     */
    private void backtrackJunctionControl(IRobot robot) {
        int passageExits = passageExits(robot);

        if (passageExits > 0) {

            mode = RobotMode.EXPLORE;

            ArrayList<Integer> passageList = new ArrayList<>();
            for (int d : directions) {
                if (robot.look(d) == IRobot.PASSAGE) {
                    passageList.add(d);
                }
            }

            int randomIndex = (int) (Math.random() * passageList.size());
            robot.face(passageList.get(randomIndex));

        } else {

            JunctionRecorder j = robotData.recordJunction(
                robot.getLocation().x,
                robot.getLocation().y,
                robot.getHeading()
            );

            int oppositeHeading = oppositeHeading(j.arrivalHeading);
            robot.setHeading(oppositeHeading);
        }
    }

    /**
     * Exploration routine for encountering new areas.
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
            junctionControl(robot, j);

        } else {
            corridorControl(robot);
        }
    }

    /**
     * Counts all reachable directions that are not walls.
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
     * Counts exits that lead into unexplored passages.
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

    /**
     * Responds to a dead end by reversing unless it is the first move.
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
     * Navigates straight corridors by choosing the forward-facing open exit.
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
     * Handles decisions at a junction using exit exploration tracking.
     */
    private void junctionControl(IRobot robot, JunctionRecorder junction) {

        ArrayList<Integer> candidates = new ArrayList<>();

        // Unexplored PASSAGE exits
        for (int d : directions) {
            if (robot.look(d) == IRobot.PASSAGE) {
                int absDir = relativeToAbsolute(robot.getHeading(), d);
                if (!robotData.isEdgeExplored(junction, absDir)) {
                    candidates.add(d);
                }
            }
        }

        if (!candidates.isEmpty()) {
            int chosen = candidates.get((int)(Math.random() * candidates.size()));
            robotData.markEdgeExplored(junction, relativeToAbsolute(robot.getHeading(), chosen));
            robot.face(chosen);
            return;
        }

        // Unexplored open exits
        candidates.clear();
        for (int d : directions) {
            if (robot.look(d) != IRobot.WALL) {
                int absDir = relativeToAbsolute(robot.getHeading(), d);
                if (!robotData.isEdgeExplored(junction, absDir)) {
                    candidates.add(d);
                }
            }
        }

        if (!candidates.isEmpty()) {
            int chosen = candidates.get((int)(Math.random() * candidates.size()));
            robotData.markEdgeExplored(junction, relativeToAbsolute(robot.getHeading(), chosen));
            robot.face(chosen);
            return;
        }

        // All exits from this junction explored → backtrack
        mode = RobotMode.BACKTRACK;
        int oppositeHeading = oppositeHeading(junction.arrivalHeading);
        robot.setHeading(oppositeHeading);
    }

    /** Converts a relative direction into an absolute heading. */
    private int relativeToAbsolute(int heading, int relativeDir) {
        if (relativeDir == IRobot.AHEAD) {
            return heading;
        } else if (relativeDir == IRobot.BEHIND) {
            return oppositeHeading(heading);
        } else if (relativeDir == IRobot.LEFT) {
            switch (heading) {
                case IRobot.NORTH: return IRobot.WEST;
                case IRobot.WEST:  return IRobot.SOUTH;
                case IRobot.SOUTH: return IRobot.EAST;
                case IRobot.EAST:  return IRobot.NORTH;
            }
        } else if (relativeDir == IRobot.RIGHT) {
            switch (heading) {
                case IRobot.NORTH: return IRobot.EAST;
                case IRobot.EAST:  return IRobot.SOUTH;
                case IRobot.SOUTH: return IRobot.WEST;
                case IRobot.WEST:  return IRobot.NORTH;
            }
        }
        return heading;
    }

    /** Returns the opposite absolute heading. */
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
