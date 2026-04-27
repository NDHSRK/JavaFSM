package org.firstinspires.ftc.teamcode.auto.fsm;

// Reconstruction of the FSM from Pratt's video https://www.youtube.com/watch?v=RweqIqouYqM
// adapted to the use of JInput for the gamepad and the states in the generic DecodeTeleOpFSM.

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.teamcode.auto.FTCButton;
import org.firstinspires.ftc.teamcode.auto.FTCGamepad;
import org.firstinspires.ftc.teamcode.auto.TestGamepads;

public class BasicTeleOpFSM {

    private static final String TAG = BasicTeleOpFSM.class.getSimpleName();

    private enum DecodeTeleOpState {
        START,
        INTAKE_IN_PROGRESS, INTAKE_DONE,
        OUTTAKE_IN_PROGRESS, OUTTAKE_DONE,
        INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS,
        LIFTER_IN_PROGRESS,
        FINISH
    }

    private Controller f310Gamepad1;

    private final FTCButton intakeButton;
    private final FTCButton outtakeButton;
    private final FTCButton lifterButton;
    private final FTCButton stopLifterButton;
    private final FTCButton exitButton;

    public BasicTeleOpFSM() {

        // Connect the gamepad.
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller c : controllers) {
            if (c.getName().contains("Gamepad F310")) {
                System.out.println("Found a Logitech controller with the name " + c.getName());
                f310Gamepad1 = c; // gamepad 1
                break;
            }
        }

        if (f310Gamepad1 == null)
            throw new AutonomousRobotException(TAG, "No F310 controllers found");

        intakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_A));
        outtakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_B));
        lifterButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_Y));
        stopLifterButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_LEFT_BUMPER));
        exitButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_X));

        System.out.println("To start intake, press A");
        System.out.println("From state " + DecodeTeleOpState.INTAKE_IN_PROGRESS + " press B to start outtake, Y to start the lifter, or X to exit");
        System.out.println("From state " + DecodeTeleOpState.OUTTAKE_IN_PROGRESS + " press Y to start the lifter, or X to exit");
        System.out.println("From state " + DecodeTeleOpState.LIFTER_IN_PROGRESS + " press X to exit");

        DecodeTeleOpState state = DecodeTeleOpState.START;
        boolean runFSM = true;
        while (runFSM) {

            intakeButton.update();
            outtakeButton.update();
            lifterButton.update();
            stopLifterButton.update();
            exitButton.update();

            switch (state) {
                case START: {
                    if (intakeButton.is(FTCButton.State.TAP)) {
                        state = DecodeTeleOpState.INTAKE_IN_PROGRESS;
                        System.out.println("Intake button pressed; transition to state " + DecodeTeleOpState.INTAKE_IN_PROGRESS);
                    }
                    break;
                }

                case INTAKE_IN_PROGRESS: {
                    if (outtakeButton.is(FTCButton.State.TAP)) {
                        state = DecodeTeleOpState.OUTTAKE_IN_PROGRESS;
                        System.out.println("Outtake button pressed; transition to state " + DecodeTeleOpState.OUTTAKE_IN_PROGRESS);
                    } else if (lifterButton.is(FTCButton.State.TAP)) {
                        state = DecodeTeleOpState.LIFTER_IN_PROGRESS;
                        System.out.println("Lifter button pressed; transition to state " + DecodeTeleOpState.LIFTER_IN_PROGRESS);
                    } else if (exitButton.is(FTCButton.State.TAP)) {
                        state = DecodeTeleOpState.FINISH;
                        System.out.println("Exit button pressed; transition to state " + DecodeTeleOpState.FINISH);
                    }
                    break;
                }

                case OUTTAKE_IN_PROGRESS: {
                    if (lifterButton.is(FTCButton.State.TAP)) {
                        state = DecodeTeleOpState.LIFTER_IN_PROGRESS;
                        System.out.println("Lifter button pressed; transition to state " + DecodeTeleOpState.LIFTER_IN_PROGRESS);
                    } else if (exitButton.is(FTCButton.State.TAP)) {
                        state = DecodeTeleOpState.FINISH;
                        System.out.println("Exit button pressed; transition to state " + DecodeTeleOpState.FINISH);
                    }
                    break;
                }

                case LIFTER_IN_PROGRESS: {
                    if (exitButton.is(FTCButton.State.TAP)) {
                        state = DecodeTeleOpState.FINISH;
                        System.out.println("Exit button pressed; transition to state " + DecodeTeleOpState.FINISH);
                    }
                    break;
                }

                case FINISH: {
                    System.out.println("State machine finished");
                    runFSM = false;
                }
            }
        }
    }
}
