package org.firstinspires.ftc.teamcode.auto;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;

public class TestGamepads {

    private static final String TAG = TestGamepads.class.getSimpleName();

    private Controller f310Gamepad1;
    private Controller f310Gamepad2;

    public TestGamepads(int pNumGamepads) throws InterruptedException {

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

        if (f310Gamepad1 == null)
            throw new AutonomousRobotException(TAG, "No F310 controllers found");
        if (pNumGamepads == 1 && f310Gamepad2 != null)
            throw new AutonomousRobotException(TAG, "Expected one F310 controller but found two");
        if (pNumGamepads == 2 && f310Gamepad2 == null)
            throw new AutonomousRobotException(TAG, "Required two F310 controllers but found only one");

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
    }

}
