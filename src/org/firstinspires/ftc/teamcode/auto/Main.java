package org.firstinspires.ftc.teamcode.auto;

import net.java.games.input.*;
import org.firstinspires.ftc.teamcode.auto.fsm.FSM6Container;

public class Main {

    //**TODO In our application we usually have two gamepads,
    // FTC Gamepad1 and Gamepad2. Need to support this configuration
    // here. Need hub to test on Windows.

         /*
       Test gamepads in Windows by typing joy.cpl into the Start menu or Run command (Win+R) and selecting "Properties".
       This built-in tool allows you to verify button presses, analog stick movement, and calibrate your controller.

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

        Handling D-Pad (POV)
        The D-Pad is typically not registered as a button, but as a POV (Point of View) component.
        Its value is an angle (0.0 to 1.0, where -1 is neutral).

        Handling triggers
        X-Input Behavior: In X-Input (Xbox-compatible) mode, the triggers are not binary buttons;
        they are analog, returning values from 0.0 (released) to 1.0 (fully pressed).
        Axis Mapping: In many JInput scenarios on Windows, the triggers appear as:
        Left Trigger: Positive Z-Axis component.
        Right Trigger: Negative Z-Axis component.
       */

    enum FTCGamepad1ComponentId {
        GAMEPAD_1_A(Component.Identifier.Button._0), GAMEPAD_1_B(Component.Identifier.Button._1),
        GAMEPAD_1_X(Component.Identifier.Button._2), GAMEPAD_1_Y(Component.Identifier.Button._3),
        GAMEPAD_1_LEFT_BUMPER(Component.Identifier.Button._4), GAMEPAD_1_RIGHT_BUMPER(Component.Identifier.Button._5),
        GAMEPAD_1_BACK(Component.Identifier.Button._6), GAMEPAD_1_START(Component.Identifier.Button._7),
        GAMEPAD_1_LEFT_STICK_CLICK(Component.Identifier.Button._8), GAMEPAD_1_RIGHT_STICK_CLICK(Component.Identifier.Button._9),
        // For the DPAD use Component.Identifier.Axis.POV
        GAMEPAD_1_DPAD_UP(Component.Identifier.Axis.POV), GAMEPAD_1_DPAD_DOWN(Component.Identifier.Axis.POV),
        GAMEPAD_1_DPAD_LEFT(Component.Identifier.Axis.POV), GAMEPAD_1_DPAD_RIGHT(Component.Identifier.Axis.POV),
        // For the triggers use Component.Identifier.Axis.Z
        GAMEPAD_1_LEFT_TRIGGER(Component.Identifier.Axis.Z), GAMEPAD_1_RIGHT_TRIGGER(Component.Identifier.Axis.Z);

        private final Component.Identifier componentId;

        FTCGamepad1ComponentId(Component.Identifier pComponentId) {
            componentId = pComponentId;
        }

        public Component.Identifier getComponentId() {
            return componentId;
        }
    }

    private static Controller f310;

    public static void main(String[] args) throws InterruptedException {

        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
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

        //FTCButton intakeButton = new FTCButton(() -> getGamepad1Value(f310, FTCGamepad1ComponentId.GAMEPAD_1_A));
        //FTCButton aimByAprilTagButton = new FTCButton(() -> getGamepad1Value(f310, FTCGamepad1ComponentId.GAMEPAD_1_DPAD_UP));

        //**TODO How do you do this?
        //         shootButton = new FTCButton(() -> linearOpMode.gamepad1.right_trigger > 0.5);

        // Poll for gamepad 1 events.
        while (true) {
            if (getGamepad1Value(f310, FTCGamepad1ComponentId.GAMEPAD_1_A)) {
                System.out.println("Successful detection of button press on " + FTCGamepad1ComponentId.GAMEPAD_1_A);
                return;
            }
            //intakeButton.update();
            //aimByAprilTagButton.update();

            //if (intakeButton.is(FTCButton.State.TAP)) {
            //    System.out.println("Successful detection of button press on " + FTCGamepad1ComponentId.GAMEPAD_1_A);
            //    return;
            ///}

            //if (aimByAprilTagButton.is(FTCButton.State.TAP)) {
            //    System.out.println("Successful detection of button press on " + FTCGamepad1ComponentId.GAMEPAD_1_DPAD_UP);
            //    return;
            //}

            Thread.sleep(20); // Small delay to reduce CPU usage
        }

        //## 1/15/2022 GenericFSM, GenericFSM2 - 4 are retained as documentation for various
        // attempts and false starts.
        //FSM6Container fsm6C = new FSM6Container();
        //fsm6C.testFSM6();
    }

    private static boolean getGamepad1Value(Controller pGamepad1, FTCGamepad1ComponentId pComponentId) throws InterruptedException {
        boolean continuePolling = true;
        while (continuePolling) {
            pGamepad1.poll(); // Poll the controller to get new data
            Component[] components = pGamepad1.getComponents();

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
                            return true;
                        }
                    }
                    continue;
                }

                /*
                Handling D-Pad (POV)
                The D-Pad is typically not registered as a button, but as a POV (Point of View) component.
                Its value is an angle (0.0 to 1.0, where -1 is neutral).
                */


                if (component.getIdentifier() == Component.Identifier.Axis.POV) {
                    float povValue = component.getPollData();
                    if (povValue == Component.POV.UP) {
                        System.out.println("DPAD up pressed");
                        return true;
                    } // DP up
                    else if (povValue == Component.POV.DOWN) {

                    } // DP down
                }


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
                //continuePolling = false;
                //break;
            }

            Thread.sleep(20); // Small delay to reduce CPU usage
        }
        return false;

    }


}
