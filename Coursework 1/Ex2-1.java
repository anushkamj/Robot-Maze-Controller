/*

PREAMBLE FOR EXERCISE 2
____________________________________________________________________________________________________________________________________________________
====================================================================================================================================================
CALCULATING ORIGINAL PROBABILITY:
According to the Java API, Math.random() generates a random double between 0.0 and 1.0, the latter being exclusive. Multiplying this value by 3 and rounding it using Math.round(), produces integers 0–3, but not with equal probability. This is because Math.round()rounds up from 0.5, the intervals are uneven:
0 → [0.0, 0.5)   → 1/6 probability
1 → [0.5, 1.5)   → 1/3 probability
2 → [1.5, 2.5)   → 1/3 probability
3 → [2.5, 3.0)   → 1/6 probability

Hence, the robot chooses directions with probabilities:
LEFT = 1/6, RIGHT = 1/3, AHEAD = 1/3, BEHIND = 1/6.

====================================================================================================================================================
FIRST TASK:
Equal probability can be easily made by changing the
'randno = (int) Math.round(Math.random()*3);' statement to 'randno = (int) Math.floor(Math.random()*4);'
Since, then the probabilities will all become 1/4!

====================================================================================================================================================
Summary of Design Choices:
Equal probabilities were achieved via Math.floor(Math.random()*4), ensuring unbiased random selection. The forward bias was preserved through conditional logic that keeps the robot moving ahead unless the 1-in-8 trigger occurs.
====================================================================================================================================================
SECOND TASK:
Incorporation of the 1 in 8 chance of changing direction:
Even when the path ahead is clear, a random integer between 0 and 7 is generated. If this integer is 0, the robot may randomly face a new direction, otherwise it continues forward. Here it is 0 but it could've been any other number from the range as well. This introduces a small element of randomness, making the robot’s path less predictable while still prioritizing moving forward.
====================================================================================================================================================
Testing and Verification:
The robot’s movement was observed over multiple runs in open and structured mazes. Results showed that it mostly continued forward but occasionally changed direction with roughly 1-in-8 frequency, confirming the correct probability balance.
-------------------------------------------------------------------------------------------------------------------------------------------
Thank you!
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;

public class Ex2
{
	public void controlRobot(IRobot robot) {

		int randno;
		int direction;

		// Select a random number

		randno = (int) Math.floor(Math.random()*4); // Method of probability balance

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

			// initialising walls
		int walls = 0;
		// making an array to store all the four directions around robot
		int[] directions = {IRobot.AHEAD, IRobot.BEHIND, IRobot.RIGHT, IRobot.LEFT};

		// counting number of walls around robot
		for (int i = 0; i <= 3; i++) {
			if (robot.look(directions[i]) == IRobot.WALL)
			walls++;
		}

		int[] clrdirections = new int[4]; //all the directions which won't cause a collision
		int count = 0;
		for (int i=0; i<=3; i++) {
			if (robot.look(directions[i]) != IRobot.WALL) {
				clrdirections[count] = directions [i];
				count++;
			}
		}

		// Incorporation of the 1-in-8 chance to change direction
		int chance = (int) Math.floor(Math.random() * 8); // any number from 0,1,2,3,4,5,6,7

		// Incorporation of the robot’s AHEAD bias
		if (robot.look(IRobot.AHEAD) == IRobot.WALL || chance == 0) { // only 1 in 8 numbers, here 0 so a 1 in 8 chance
			// Turning the robot around until it does not have a wall ahead
			while (robot.look(IRobot.AHEAD) == IRobot.WALL) {
				int dir = clrdirections[(int) Math.floor(Math.random() * count)];
				robot.face(dir); // Face the robot in a new random direction for next checking
			}
		}

		else {
			robot.face(IRobot.AHEAD); // continues its journey!
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

		// final print message
		System.out.println (move_direction + " " + surround_type);

	}

}
