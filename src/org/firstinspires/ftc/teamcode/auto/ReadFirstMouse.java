package org.firstinspires.ftc.teamcode.auto;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

/**
 * This class shows how to read the values in a polling loop for the first mouse
 * detected. It will show how to get the available controllers, how to check the
 * type of the controller, how to read the components of the controller, and how
 * to get the data from the component.
 *
 * @author Endolf
 */
public class ReadFirstMouse {

	public ReadFirstMouse() {
		/* Get the available controllers */
		Controller[] controllers = ControllerEnvironment
				.getDefaultEnvironment().getControllers();

		/*
		 * Loop through the controllers, check the type of each one, and save
		 * the first mouse we find.
		 */
		Controller firstGamepad = null;
		Controller.Type controllerType;

		for (int i = 0; i < controllers.length && firstGamepad == null; i++) {
			controllerType = controllers[i].getType();
			// System.out.println("Controller index " + i + ", type " + controllerType);
			if (controllerType == Controller.Type.GAMEPAD) {
				// Found a gamepad
				firstGamepad = controllers[i];
			}
		}

		if (firstGamepad == null) {
			// Couldn't find a gamepad
			System.out.println("Found no gamepad");
			System.exit(0);
		}

		//Controller index 16, type Gamepad
		//First gamepad is: Controller (Gamepad F310)
		System.out.println("First gamepad is: " + firstGamepad.getName());

		while (true) {
			/* Poll the controller */
			firstGamepad.poll();

			/* Get all the axis and buttons */
			Component[] components = firstGamepad.getComponents();
			StringBuffer buffer = new StringBuffer();

			/* For each component, get it's name, and it's current value */
			for (int i = 0; i < components.length; i++) {
				if (i > 0) {
					buffer.append(", ");
				}
				buffer.append(components[i].getName());
				buffer.append(": ");
				if (components[i].isAnalog()) {
					/* Get the value at the last poll of this component */
					buffer.append(components[i].getPollData());
				} else {
					if (components[i].getPollData() == 1.0f) {
						buffer.append("On");
					} else {
						buffer.append("Off");
					}
				}
			}
			System.out.println(buffer.toString());

			/*
			 * Sleep for 20 millis, this is just so the example doesn't thrash
			 * the system.
			 */
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
