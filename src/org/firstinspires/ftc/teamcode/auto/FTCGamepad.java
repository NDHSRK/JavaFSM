package org.firstinspires.ftc.teamcode.auto;

import net.java.games.input.Component;
import net.java.games.input.Controller;

// Logitech F310 Button Mapping (XInput Mode) between JInput and FTC LinearOpMode.
public class FTCGamepad {
    
    /*
       Test gamepads in Windows by typing joy.cpl into the Start menu
       or Run command (Win+R) and selecting "Properties". This built-in
       tool allows you to verify button presses, analog stick movement,
       and calibrate your controller.

       If the switch on the back of the F310 is set to X,
       the buttons generally map to these JInput Identifiers:

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

        D-Pad (POV)
        The D-Pad is typically not registered as a button, but as a POV (Point of View) component.
        Its value is an angle (0.0 to 1.0, where -1 is neutral).
       */

    public enum FTCButtonId {
        GAMEPAD_A(Component.Identifier.Button._0), GAMEPAD_B(Component.Identifier.Button._1),
        GAMEPAD_X(Component.Identifier.Button._2), GAMEPAD_Y(Component.Identifier.Button._3),
        GAMEPAD_LEFT_BUMPER(Component.Identifier.Button._4), GAMEPAD_RIGHT_BUMPER(Component.Identifier.Button._5),
        GAMEPAD_BACK(Component.Identifier.Button._6), GAMEPAD_START(Component.Identifier.Button._7),
        GAMEPAD_LEFT_STICK_CLICK(Component.Identifier.Button._8), GAMEPAD_RIGHT_STICK_CLICK(Component.Identifier.Button._9),
        // For the DPAD use Component.Identifier.Axis.POV
        GAMEPAD_DPAD_UP(Component.Identifier.Axis.POV), GAMEPAD_DPAD_DOWN(Component.Identifier.Axis.POV),
        GAMEPAD_DPAD_LEFT(Component.Identifier.Axis.POV), GAMEPAD_DPAD_RIGHT(Component.Identifier.Axis.POV);

        private final Component.Identifier componentId;

        FTCButtonId(Component.Identifier pComponentId) {
            componentId = pComponentId;
        }

        public Component.Identifier getComponentId() {
            return componentId;
        }
    }

    /*
        Triggers
        X-Input Behavior: In X-Input (Xbox-compatible) mode, the triggers are not binary buttons;
        they are analog, returning values from 0.0 (released) to 1.0 (fully pressed).
        Axis Mapping: In many JInput scenarios on Windows, the triggers appear as:
        Left Trigger: Positive Z-Axis component.
        Right Trigger: Negative Z-Axis component.
     */
    public enum FTCTriggerId {
        // For the triggers use Component.Identifier.Axis.Z
        GAMEPAD_LEFT_TRIGGER(Component.Identifier.Axis.Z), GAMEPAD_RIGHT_TRIGGER(Component.Identifier.Axis.Z);

        private final Component.Identifier componentId;

        FTCTriggerId(Component.Identifier pComponentId) {
            componentId = pComponentId;
        }

        public Component.Identifier getComponentId() {
            return componentId;
        }
    }

    public static boolean gamepadButtonPressed(Controller pGamepad, FTCButtonId pButtonId) {
        pGamepad.poll(); // Poll the controller to get new data
        Component[] components = pGamepad.getComponents();
        for (Component component : components) {
            // Buttons
            if (component.getIdentifier() instanceof Component.Identifier.Button) {
                float value = component.getPollData();

                // Specifically check for the requested button.
                if (component.getIdentifier() == pButtonId.getComponentId() && value == 1.0f) {
                    return true;
                }

                continue;
            }

            // DPAD
            if (component.getIdentifier() == Component.Identifier.Axis.POV) {
                float povValue = component.getPollData();
                switch (pButtonId) {
                    case FTCButtonId.GAMEPAD_DPAD_UP: {
                        if (povValue == Component.POV.UP)
                            return true;
                        break;
                    }
                    case FTCButtonId.GAMEPAD_DPAD_DOWN: {
                        if (povValue == Component.POV.DOWN)
                            return true;
                        break;
                    }
                    case FTCButtonId.GAMEPAD_DPAD_LEFT: {
                        if (povValue == Component.POV.LEFT)
                            return true;
                        break;
                    }
                    case FTCButtonId.GAMEPAD_DPAD_RIGHT: {
                        if (povValue == Component.POV.RIGHT)
                            return true;
                        break;
                    }
                }

                continue;
            }
        }

        return false;
    }

    // To use a trigger as a button.
    /*
      Values from testing on Windows laptop:
      Trigger Axis (Z): -1.5258789E-5, i.e. 0.0000525
      Left trigger pulled 0.99612427, i.e. >= 0.01 && <= 1.0
      Right trigger pulled -0.9960937, i.e. <= -0.01 && >= -1.0
     */
    public static float gamepadTriggerPressed(Controller pGamepad, FTCTriggerId pTriggerId) {
        pGamepad.poll(); // Poll the controller to get new data
        Component[] components = pGamepad.getComponents();
        for (Component component : components) {
            if (component.getIdentifier() == Component.Identifier.Axis.Z) {
                float value = component.getPollData();
                if (pTriggerId == FTCTriggerId.GAMEPAD_LEFT_TRIGGER && value >= 0.01 && value <= 1.0)
                    return value;
                else if (pTriggerId == FTCTriggerId.GAMEPAD_RIGHT_TRIGGER && value <= -0.01 && value >= -1.0)
                    return Math.abs(value);
            }
        }

        return 0.0f; // not pressed
    }

}
