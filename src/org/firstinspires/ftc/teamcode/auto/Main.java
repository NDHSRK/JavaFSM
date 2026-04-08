package org.firstinspires.ftc.teamcode.auto;

import net.java.games.input.*;
import org.firstinspires.ftc.teamcode.auto.fsm.FSM6Container;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        //**TODO In our application we usually have two gamepads,
        // FTC Gamepad1 and Gamepad2. Need to support this configuration
        // here. Need hub to test on Windows.

        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        Controller f310 = null;
        for (Controller c : controllers) {
            if (c.getName().contains("Logitech") || c.getName().contains("F310")) {
                f310 = c;
                break;
            }
        }

        if (f310 == null) {
            System.out.println("F310 not found!");
            return;
        }

        //**TODO Make this into a method that returns a boolean.
        // Parameters: FTCGamepadElement pGamepadElement, which
        // will determine the controller and Component to look
        // at - use pGamepadElement.toString().contains"GAMEPAD_1"
        /*
        public enum FTCGamepadElement { // was ButtonValue
        GAMEPAD_1_A, GAMEPAD_1_B, ... GAMEPAD_2_A ...
         */
        while (true) {
            f310.poll(); // Poll the controller to get new data
            Component[] components = f310.getComponents();

            for (Component component : components) {

                /*
                F310 Button Mapping (XInput Mode)
                If the switch on the back of the F310 is set to X, the buttons generally map to these JInput Identifiers:

                A Button: Component.Identifier.Button._0
                B Button: Component.Identifier.Button._1
                X Button: Component.Identifier.Button._2
                Y Button: Component.Identifier.Button._3
                Left Bumper: Component.Identifier.Button._4
                Right Bumper: Component.Identifier.Button._5
                Back: Component.Identifier.Button._6
                Start: Component.Identifier.Button._7
                Left Stick Click: Component.Identifier.Button._8
                Right Stick Click: Component.Identifier.Button._9
                 */
                if (component.getIdentifier() instanceof Component.Identifier.Button) {
                    float value = component.getPollData();
                    if (value == 1.0f) {
                        // Button is pressed
                        System.out.println(component.getName() + " pressed");

                        // Example: Specifically check for the 'A' button
                        if (component.getIdentifier() == Component.Identifier.Button._0) {
                            // This is usually the A button in XInput
                        }
                    }
                    continue;
                }

                /*
                Handling D-Pad (POV)
                The D-Pad is typically not registered as a button, but as a POV (Point of View) component.
                Its value is an angle (0.0 to 1.0, where -1 is neutral).


                if (comp.getIdentifier() == Component.Identifier.Axis.POV) {
                float povValue = comp.getPollData();
                if (povValue == Component.POV.UP) { } // DP up
                else if (povValue == Component.POV.DOWN) { } // DP down
                }
                 */

                /*
                Key Information for X-Input Triggers
                Switch Position: Ensure the switch on the back of the F310 is set to 'X'.
                X-Input Behavior: In X-Input (Xbox-compatible) mode,
                 the triggers are not binary buttons; they are analog,
                 returning values from 0.0 (released) to 1.0 (fully pressed).

                 Axis Mapping: In many JInput scenarios on Windows, the triggers appear as:
                 Left Trigger: Positive Z-Axis component.
                 Right Trigger: Negative Z-Axis component.

                Values from testing on Windows laptop:
                Trigger Axis (Z): -1.5258789E-5, i.e. 0.0000525
                Left trigger pulled 0.99612427, i.e. >= 0.01 && <= 1.0
                Right trigger pulled -0.9960937, i.e. <= -0.01 && >= -1.0
                 */
                if (component.getIdentifier() == Component.Identifier.Axis.Z) {
                    float value = component.getPollData();
                    if (value >= 0.01 && value <= 1.0)
                        System.out.println("Left trigger pulled, value: " + value);
                    else if (value <= -0.01 && value >= -1.0)
                        System.out.println("Right trigger pulled, value: " + value);
                }
            }
            Thread.sleep(20); // Small delay to reduce CPU usage
        }

        //## 1/15/2022 GenericFSM, GenericFSM2 - 4 are retained as documentation for various
        // attempts and false starts.
        //FSM6Container fsm6C = new FSM6Container();
        //fsm6C.testFSM6();
    }
}
