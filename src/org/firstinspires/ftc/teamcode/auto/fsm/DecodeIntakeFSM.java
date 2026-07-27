package org.firstinspires.ftc.teamcode.auto.fsm;

// IntelliJ testbed for the development of a Finite State Machine that can
// later be ported into the FTC environment in Android Studio.
// This testbed uses the JInput library to get input from a gamepad.

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

// Emulate a portion of the intake logic from FTC Team 4348's TeleOp Opmode
// in the FTC Decode game.
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
        GAMEPAD_EVENT, CHECK_LIFTER_DONE, EXIT
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

    public DecodeIntakeFSM(int pNumGamepads) throws IOException, InterruptedException, TimeoutException {
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

        System.out.println("To start intake, press and HOLD A");
        System.out.println("From state " + DecodeTeleOpState.INTAKE_IN_PROGRESS + " press B to start outtake, DOUBLE press Y to start the lifter, or X to exit");
        System.out.println("From state " + DecodeTeleOpState.OUTTAKE_IN_PROGRESS + " DOUBLE press Y to start the lifter, or X to exit");
        System.out.println("From state " + DecodeTeleOpState.LIFTER_IN_PROGRESS + " press X to exit");

        DecodeTeleOpState nextState = DecodeTeleOpState.START;
        DecodeTeleOpEvent nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
        while (nextEvent != DecodeTeleOpEvent.EXIT) {
            // Updating all button states here is safer even though
            // not all buttons are queried at each step of the process.
            if (nextEvent == DecodeTeleOpEvent.GAMEPAD_EVENT) {
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

            //!! To be correct, each state should ensure that the current event
            // applies to the state. If a state reacts only to one event then a
            // simple "if (nextEvent != DecodeTeleOpEvent.GET_GAMEPAD_EVENT)" ->
            // state error is good enough. Otherwise a switch/case construct will
            // be required.
            switch (nextState) {
                case START: {
                    if (nextEvent != DecodeTeleOpEvent.GAMEPAD_EVENT)
                        throw new AutonomousRobotException(TAG, "Unexpected event " + nextEvent + " for state " + nextState);

                    // Button press to turn intake ON. Assume the Revolver is empty.
                    // Guard
                    if (intakeButton.is(FTCButton.State.TAP)) {
                        nextState = DecodeTeleOpState.INTAKE_IN_PROGRESS;
                        // Action
                        System.out.println("Intake button pressed; transition to state " + DecodeTeleOpState.INTAKE_IN_PROGRESS);
                        intakeOnAction();
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    }
                    break;
                }

                case INTAKE_IN_PROGRESS: {
                    if (nextEvent != DecodeTeleOpEvent.GAMEPAD_EVENT)
                        throw new AutonomousRobotException(TAG, "Unexpected event " + nextEvent + " for state " + nextState);

                    if (intakeButton.is(FTCButton.State.HELD) && intakeFuture.isDone()) {
                        // The driver is still holding the intake button but intake
                        // has completed automatically because the Revolver is full.
                        nextState = DecodeTeleOpState.INTAKE_DONE;
                        // Action
                        System.out.println("Intake finished; transition to state " + DecodeTeleOpState.INTAKE_DONE);
                        intakeDoneAction();
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    } else if (intakeButton.is(FTCButton.State.UP)) {
                        // The driver has released the intake button; stop intake.
                        nextState = DecodeTeleOpState.INTAKE_DONE;
                        // Action
                        System.out.println("Intake stopped; revolver may or may not be full; transition to state" + DecodeTeleOpState.INTAKE_DONE);
                        intakeOffAction();
                        intakeDoneAction();
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    } else if (outtakeButton.is(FTCButton.State.TAP)) {
                        // Turn outtake ON while intake is in progress.
                        nextState = DecodeTeleOpState.INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS;
                        // Action
                        System.out.println("Intake paused and outtake in progress; transition to state " + DecodeTeleOpState.INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS);
                        outtakeOnDuringIntakeAction();
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    }
                    break;
                }

                // Intake is complete: the revolver is full or intake has
                // been turned off with 0 - 2 artifacts in the revolver.
                case INTAKE_DONE: {
                    if (nextEvent != DecodeTeleOpEvent.GAMEPAD_EVENT)
                        throw new AutonomousRobotException(TAG, "Unexpected event " + nextEvent + " for state " + nextState);

                    // For the demonstration, exit on a button press.
                    if (exitButton.is(FTCButton.State.TAP)) {
                        nextEvent = DecodeTeleOpEvent.EXIT;
                    } else if (intakeButton.is(FTCButton.State.TAP) && !revolverIsFull()) {
                        nextState = DecodeTeleOpState.INTAKE_IN_PROGRESS;
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                        // Check button press to turn intake back ON; the revolver must not be full.
                    } else if (outtakeButton.is(FTCButton.State.TAP)) {
                        outtakeOnAction();
                        nextState = DecodeTeleOpState.OUTTAKE_IN_PROGRESS;
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                        // Turn outtake ON when intake is not running.
                    } else if (lifterButton.is(FTCButton.State.DOUBLE_TAP)) {
                        // Start the lifter
                        startLifterAction();
                        nextState = DecodeTeleOpState.LIFTER_IN_PROGRESS;
                        nextEvent = DecodeTeleOpEvent.CHECK_LIFTER_DONE;
                    }

                    break;
                }

                // Outtake when intake is *not* running.
                case OUTTAKE_IN_PROGRESS: {
                    if (nextEvent != DecodeTeleOpEvent.GAMEPAD_EVENT)
                        throw new AutonomousRobotException(TAG, "Unexpected event " + nextEvent + " for state " + nextState);

                    // The driver cancels outtake by letting go of the button.
                    if (outtakeButton.is(FTCButton.State.UP)) {
                        outtakeOffAction();
                        nextState = DecodeTeleOpState.OUTTAKE_DONE;
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    }

                    break;
                }

                // Outtake has been turned OFF.
                // At this point the revolver may contain from 0 to 3 artifacts.
                case OUTTAKE_DONE: {
                    if (nextEvent != DecodeTeleOpEvent.GAMEPAD_EVENT)
                        throw new AutonomousRobotException(TAG, "Unexpected event " + nextEvent + " for state " + nextState);

                    // Allow an immediate exit.
                    if (exitButton.is(FTCButton.State.TAP)) {
                        nextEvent = DecodeTeleOpEvent.EXIT;
                    } else if (intakeButton.is(FTCButton.State.TAP) && !revolverIsFull()) {
                        // Check button press to turn intake back ON; revolver must not be full.
                        nextState = DecodeTeleOpState.INTAKE_IN_PROGRESS;
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    } else if (outtakeButton.is(FTCButton.State.TAP)) {
                        // Check button press to turn outtake back ON.
                        outtakeOnAction();
                        nextState = DecodeTeleOpState.OUTTAKE_IN_PROGRESS;
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    }

                    break;
                }

                // Outtake when intake is paused.
                case INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS: {
                    if (nextEvent != DecodeTeleOpEvent.GAMEPAD_EVENT)
                        throw new AutonomousRobotException(TAG, "Unexpected event " + nextEvent + " for state " + nextState);

                    if (outtakeButton.is(FTCButton.State.UP)) {
                        outtakeOffDuringIntakeAction();
                        nextState = DecodeTeleOpState.INTAKE_IN_PROGRESS;
                        nextEvent = DecodeTeleOpEvent.GAMEPAD_EVENT;
                    }

                    break;
                }

                // The lifter is running; wait for completion.
                // Demonstrates the use of an event other than GET_GAMEPAD_EVENT.
                case LIFTER_IN_PROGRESS: {
                    if (nextEvent != DecodeTeleOpEvent.CHECK_LIFTER_DONE)
                        throw new AutonomousRobotException(TAG, "Unexpected event " + nextEvent + " for state " + nextState);

                    // The driver lets the lifter run to completion.
                    if (checkLiftDone()) {
                        nextEvent = DecodeTeleOpEvent.EXIT;
                    }
                    break;
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

