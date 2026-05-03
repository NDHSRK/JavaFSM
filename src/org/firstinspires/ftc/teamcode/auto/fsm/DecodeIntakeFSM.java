package org.firstinspires.ftc.teamcode.auto.fsm;

// Reconstruction of the FSM from Pratt's video https://www.youtube.com/watch?v=RweqIqouYqM
// adapted to the use of JInput for the gamepad and the states in the generic DecodeTeleOpFSM.

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.ftcdevcommon.Threading;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.WorkingDirectory;
import org.firstinspires.ftc.teamcode.auto.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

public class DecodeIntakeFSM {

    private static final String TAG = DecodeIntakeFSM.class.getSimpleName();

    private enum DecodeTeleOpState {
        START,
        INTAKE_IN_PROGRESS, INTAKE_DONE,
        OUTTAKE_IN_PROGRESS, OUTTAKE_DONE,
        INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS,
        LIFTER_IN_PROGRESS,
        FINISH
    }

    private enum DecodeTeleOpEvent {
        GET_GAMEPAD_EVENT, CHECK_LIFTER_DONE, EXIT
    }

    private Controller f310Gamepad1;
    private Controller f310Gamepad2;

    // Intake
    private final FTCButton intakeButton;
    private final IntakeMotion intakeMotion;
    private CompletableFuture<Integer> intakeFuture;
    private int artifactsToIntake = RobotConstantsDecode.MAX_ARTIFACTS_IN_REVOLVER;
    private int artifactsInRevolver = 0;

    private final FTCButton outtakeButton;

    private final FTCButton lifterButton;
    private final FTCButton stopLifterButton;
    private Instant lifterTimerStart;
    private static final long lifterDone = 5; // simulate a five-second lift
    private final FTCButton exitButton;

    public DecodeIntakeFSM(int pNumGamepads) {
        String logDirPath = WorkingDirectory.getWorkingDirectory() + RobotConstants.logDir;
        RobotLogCommon.OpenStatus openStatus = RobotLogCommon.initialize(RobotLogCommon.LogIdentifier.TELEOP_LOG,
                RobotLogCommon.LoggingMode.MIRROR_TO_SYSOUT, logDirPath);
        if (openStatus != RobotLogCommon.OpenStatus.NEW_LOGGER_CREATED)
            throw new AutonomousRobotException(TAG, "Logger not initialized");

        RobotLogCommon.c(TAG, "Constructing BasicIntakeFSM");

        // Gamepad controllers.
        //**TODO On every start: INFO: Failed to initialize device HIDI2C Device because of: java.io.IOException: Failed to acquire device (8007001e)
        // Causes "No controllers found" System.setProperty("net.java.games.input.useDefaultPlugin", "false");

        // Connect with the gamepad(s).
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller c : controllers) {
            if (c.getName().contains("Gamepad F310")) {
                System.out.println("Found a Logitech controller with the name " + c.getName());
                if (f310Gamepad1 == null)
                    f310Gamepad1 = c; // gamepad 1
                else {
                    f310Gamepad2 = c; // gamepad 2
                    break;
                }
            }
        }

        if (f310Gamepad1 == null)
            throw new AutonomousRobotException(TAG, "No F310 controllers found");
        if (pNumGamepads == 1 && f310Gamepad2 != null)
            throw new AutonomousRobotException(TAG, "Expected one F310 controller but found two");
        if (pNumGamepads == 2 && f310Gamepad2 == null)
            throw new AutonomousRobotException(TAG, "Required two F310 controllers but found only one");

        intakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_A));
        outtakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_B));
        lifterButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_Y));
        stopLifterButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_LEFT_BUMPER));
        exitButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_X));

        // Initialize simulated intake.
        intakeMotion = new IntakeMotion();

        System.out.println("To start intake, press A");
        System.out.println("From state " + DecodeTeleOpState.INTAKE_IN_PROGRESS + " press B to start outtake, Y to start the lifter, or X to exit");
        System.out.println("From state " + DecodeTeleOpState.OUTTAKE_IN_PROGRESS + " press Y to start the lifter, or X to exit");
        System.out.println("From state " + DecodeTeleOpState.LIFTER_IN_PROGRESS + " press X to exit");

        //**TODO !!WARNING!! The code below will not run correctly
        // until the code for all of the cases has been filled in.

        DecodeTeleOpState nextState = DecodeTeleOpState.START;
        DecodeTeleOpEvent nextEvent = DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
        while (nextEvent != DecodeTeleOpEvent.EXIT) {
            // Updating all button states here is safer even though
            // not all buttons are queried at each step of the process.
            if (nextEvent == DecodeTeleOpEvent.GET_GAMEPAD_EVENT) {
                intakeButton.update();
                outtakeButton.update();
                lifterButton.update();
                stopLifterButton.update();
                exitButton.update();
            } else {
                // But if we have a state whose transitions don't
                // need to look at any buttons, make sure all the
                // buttons will be OFF on the next cycle.
                intakeButton.reset();
                outtakeButton.reset();
                lifterButton.reset();
                stopLifterButton.reset();
                exitButton.reset();
            }

            switch (nextState) {
                case START: {
                    // Button press to turn intake ON. Assume the Revolver is empty.
                    // Guard
                    if (intakeButton.is(FTCButton.State.TAP)) {
                        nextState = DecodeTeleOpState.INTAKE_IN_PROGRESS;
                        // Action
                        System.out.println("Intake button pressed; transition to state " + DecodeTeleOpState.INTAKE_IN_PROGRESS);
                        intakeOnAction();
                        nextEvent = DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                    }
                    break;
                }

                case INTAKE_IN_PROGRESS: {
                    // The driver is still holding the intake button but intake
                    // has completed automatically because the Revolver is full.
                    // Guard
                    // if (intakeButton.is(FTCButton.State.HELD) && intakeFuture.isDone())
                    // Action
                    // intakeDoneAction(); // sets the artifactsInRevolver field
                    nextState = DecodeTeleOpState.INTAKE_DONE;
                    nextEvent = DecodeTeleOpEvent.GET_GAMEPAD_EVENT;

                    // The driver has released the intake button; stop intake.
                    // The Revolver may or may not be full.
                    // else
                    // Guard
                    // if (intakeButton.is(FTCButton.State.UP))
                    // Action
                    nextState = DecodeTeleOpState.INTAKE_DONE;
                    nextEvent = DecodeTeleOpEvent.GET_GAMEPAD_EVENT;

                    // Turn outtake ON while intake is in progress.
                    // else
                    // Guard
                    // if (outtakeButton.is(FTCButton.State.TAP))
                    // Action
                    nextState = DecodeTeleOpState.INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS;
                    nextEvent = DecodeTeleOpEvent.GET_GAMEPAD_EVENT;

                    break;
                }

                // Intake is complete: the revolver is full or intake has
                // been turned off with 0 - 2 artifacts in the revolver.
                case INTAKE_DONE: {
                    // For the demonstration, exit on a button press.
                    // Gaurd
                    // Action

                    // Check button press to turn intake back ON; the revolver must not be full.
                    // Guard
                    // Action

                    // Turn outtake ON when intake is not running.
                    // Guard
                    // Action

                    // Start the lifter.
                    // Guard
                    // nextEvent = DecodeTeleOpEvent.CHECK_LIFTER_DONE;
                    // Action

                    break;
                }

                // Outtake when intake is *not* running.
                case OUTTAKE_IN_PROGRESS: {
                    // The driver cancels outtake by letting go of the button.
                    // Guard
                    // Action
                    break;
                }

                // Outtake has been turned OFF.
                // At this point the revolver may contain from 0 to 3 artifacts.
                case OUTTAKE_DONE: {
                    // Allow an immediate exit.
                    // Guard
                    // Action

                    // Check button press to turn intake back ON; the revolver must not be full.
                    // Guard
                    // Action

                    // Check button press to turn outtake back ON.
                    // Guard
                    // Action
                    break;
                }

                // Outtake when intake is paused.
                case INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS: {
                    // Guard
                    // Action
                    break;
                }

                // The lifter is running; wait for completion.
                // Demonstrates the use of an event other than GET_GAMEPAD_EVENT.
                case LIFTER_IN_PROGRESS: {
                    // The driver lets the lifter run to completion.
                    // Guard
                    nextState = DecodeTeleOpState.FINISH;
                    nextEvent = DecodeTeleOpEvent.EXIT;
                    // Action
                    break;
                }

                case FINISH: {
                    System.out.println("State machine finished");
                    nextEvent = DecodeTeleOpEvent.EXIT;
                }
            }
        }
    }

    // Guard condition methods.
    private boolean revolverIsFull() {
        return artifactsInRevolver == RobotConstantsDecode.MAX_ARTIFACTS_IN_REVOLVER;
    }

    // Action routine methods.
    // Intake
    private void intakeOnAction() {
        /*
        robot.frontIntakeMotor.intake();
        robot.middleIntakeServo.intake();
        robot.backIntakeServo.intake();
        */

        artifactsToIntake -= artifactsInRevolver;
        CompletableFuture<Integer> localIntakeFuture = intakeMotion.startIntake(artifactsToIntake);
        if (localIntakeFuture == null)
            throw new AutonomousRobotException(TAG, "Illegal state: request to intake when intake is already in progress");

        intakeFuture = localIntakeFuture; // intake thread is running
    }

    private void intakeOffAction() {
        intakeMotion.stopIntake();
        RobotLogCommon.d(TAG, "Intake interrupted by driver: intake turned off");
    }

    // If the IntakeFuture completed because the revolver is full
    // then getFutureCompletion() will return immediately. If the
    // driver has toggled the intake button to OFF then
    // getFutureCompletion() will wait here until the IntakeFuture
    // is complete.
    private void intakeDoneAction() throws IOException, InterruptedException, TimeoutException {
        artifactsInRevolver = Threading.getFutureCompletion(intakeFuture);
        RobotLogCommon.d(TAG, "Intake complete with " + artifactsInRevolver + " artifacts in the revolver");

        intakeFuture = null;

        // Turn off intake.
        //robot.frontIntakeMotor.stop();
        //robot.backIntakeServo.stop();
    }

    // Outtake
    private void outtakeOnAction() {
        RobotLogCommon.d(TAG, "Outtake button pressed; intake is not running");
        //robot.frontIntakeMotor.outtake();
        //robot.middleIntakeServo.outtake();
        //robot.backIntakeServo.outtake();
    }

    private void outtakeOffAction() {
        //robot.frontIntakeMotor.stop();
        //robot.backIntakeServo.stop();
        RobotLogCommon.d(TAG, "Outtake cancelled by driver while intake was not running");
    }

    private void outtakeOnDuringIntakeAction() {
        RobotLogCommon.d(TAG, "Outtake button pressed during intake");
        //robot.frontIntakeMotor.outtake();
        //robot.middleIntakeServo.outtake();
        //robot.backIntakeServo.outtake();

        intakeMotion.pause();
        RobotLogCommon.d(TAG, "Pause intake during reversal");
    }

    private void outtakeOffDuringIntakeAction() {
        //robot.frontIntakeMotor.intake();
        //robot.middleIntakeServo.intake();
        //robot.backIntakeServo.intake();

        intakeMotion.resume();
        RobotLogCommon.d(TAG, "Resume intake in forward direction");
    }

    private void startLifterAction() {
        // For the simulation, just run a 5-second timer.
        RobotLogCommon.d(TAG, "Simulated lifter is running");

        lifterTimerStart = Instant.now();
    }

    // Check whether the lifter simulation is done.
    private boolean checkLiftDone() {
        Instant checkLifterTimer = Instant.now();
        long secondsElapsed = Duration.between(lifterTimerStart, checkLifterTimer).toSeconds();
        return secondsElapsed == lifterDone;
    }
}

