package org.firstinspires.ftc.teamcode.auto;

import net.java.games.input.*;
import org.firstinspires.ftc.teamcode.auto.fsm.DecodeTeleOpFSM;

public class Main {

    //**TODO In our application we usually have two gamepads,
    // FTC Gamepad1 and Gamepad2. Need to support this configuration
    // here. Need hub to test on Windows.

    private static Controller f310Gamepad1;
    private static Controller f310Gamepad2;

    public static void main(String[] args) throws Exception {

        // TEMP to test the JInput/FTC gamepad translation.
        /*
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller c : controllers) {
            if (c.getName().contains("Logitech") || c.getName().contains("F310")) {
                if (f310Gamepad1 == null)
                    f310Gamepad1 = c; // gamepad 1
                else
                    f310Gamepad2 = c; // gamepad 2
                break;
            }
        }

        if (f310Gamepad1 == null) {
            System.out.println("F310 not found!");
            return;
        }

        FTCButton intakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_A));
        FTCButton aimByAprilTagButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_DPAD_UP));
        FTCButton shootButton = new FTCButton(() -> FTCGamepad.gamepadTriggerPressed(f310Gamepad1, FTCGamepad.FTCTriggerId.GAMEPAD_RIGHT_TRIGGER) > 0.5);

        while (true) {

            intakeButton.update();
            aimByAprilTagButton.update();
            shootButton.update();

            if (intakeButton.is(FTCButton.State.TAP)) {
                System.out.println("Successful detection of button press on " + FTCGamepad.FTCButtonId.GAMEPAD_A);
                return;
            }

            if (aimByAprilTagButton.is(FTCButton.State.TAP)) {
                System.out.println("Successful detection of button press on " + FTCGamepad.FTCButtonId.GAMEPAD_DPAD_UP);
                return;
            }

            if (shootButton.is(FTCButton.State.TAP)) {
                System.out.println("Successful detection of trigger press on " + FTCGamepad.FTCTriggerId.GAMEPAD_RIGHT_TRIGGER);
                return;
            }

            Thread.sleep(20); // Small delay to reduce CPU usage
        }
         */

        //**TODO Put the number of gamepads on the command line.
        //**TODO Put the number of artifacts to intake on the command line.
        System.out.println("Argument count: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("Argument " + i + ": " + args[i]);
        }

        DecodeTeleOpFSM teleOpFSM = new DecodeTeleOpFSM(1, 3);
        teleOpFSM.runIntakeFSM();
    }

}
