/*
PREAMBLE FOR EXERCISE 1
__________________________________________________________________________________________________________________________________________
==========================================================================================================================================
I implemented a while loop that keeps checking the robot's intended direction until it finds a clear path ahead

- COLLISION AVOIDANCE: The robot constantly checks if the square in front of it is a wall. If so, it randomly selects a new direction and repeats this process until the path is clear. This ensures the robot never moves into a wall

- CODE DESIGN: I chose a while loop over a for loop because it continues running until a safe move is found, making the logic simple and robust. Randomly selecting directions adds unpredictability while keeping the robot safe

- LOGGING OUTPUTt:
Each decision prints the move and environment type, confirming correct interpretation of surroundings

- Test process: The code was tested in different maze scenarios, including crossroads, junctions, corridors, and dead-ends. At each step, the robot's movement and surroundings were printed, verifying that it always avoids walls and moves logically according to its environment
------------------------------------------------------------------------------------------------------------------------------------------
Thank you!

*/

import uk.ac.warwick.dcs.maze.logic.IRobot;

public class Ex1 {
	public void controlRobot(IRobot robot) {

		int randno;
		int direction;

		// Select a random number

		randno = (int) Math.round(Math.random()*3);

		// Convert this to a direction

		if (randno == 0)
		direction = IRobot.LEFT;
		else if (randno == 1)
		direction = IRobot.RIGHT;
		else if (randno == 2)
		direction = IRobot.BEHIND;
		else
		direction = IRobot.AHEAD;

		robot.face(direction); /* Face the robot in this direction */

		// Turning the robot around until it does not have a wall ahead
		while (robot.look(IRobot.AHEAD) == IRobot.WALL) {
		int randdir = (int) Math.round(Math.random() * 3);
		int newdir;

		if (randdir == 0)
		newdir = IRobot.LEFT;
		else if (randdir == 1)
		newdir = IRobot.RIGHT;
		else if (randdir == 2)
		newdir = IRobot.BEHIND;
		else
		newdir = IRobot.AHEAD;

		robot.face(newdir);
}

		// initialising walls
		int walls = 0;
		// making an array to store all the four directions around robot
		int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.RIGHT, IRobot.LEFT};

		// counting number of walls around robot
		for (int i = 0; i <= 3; i++) {
			if (robot.look(directions[i]) == IRobot.WALL)
			walls++;
		}

		// matching number of walls with type of road
		String surround_type = "";
		if (walls == 0)
		surround_type = "at a crossroad";
		else if (walls == 1)
		surround_type = "at a junction";
		else if (walls == 2)
		surround_type = "down a corridor";
		else if (walls == 3)
		surround_type = "at a dead-end";

		// determining which direction robot moves in
		String move_direction = "";
		if (direction == IRobot.AHEAD)
		move_direction = "I'm going forward";
		if (direction == IRobot.BEHIND)
		move_direction = "I'm going backwards";
		if (direction == IRobot.RIGHT)
		move_direction = "I'm going right";
		if (direction == IRobot.LEFT)
		move_direction = "I'm going left";

		// final logging output message
		System.out.println (move_direction + " " + surround_type);

	}

}
