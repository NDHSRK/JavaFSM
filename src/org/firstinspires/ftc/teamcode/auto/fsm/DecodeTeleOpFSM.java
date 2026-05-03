package org.firstinspires.ftc.teamcode.auto.fsm;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.ftcdevcommon.Threading;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.WorkingDirectory;
import org.firstinspires.ftc.teamcode.auto.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

// Demonstration of the use of a generic Finite State Machine in TeleOp
// for the Decode game. Based on public class DecodeTeleOpFSM extends
// TeleOpBase in
//   package org.firstinspires.ftc.teamcode.teleop.opmodes.test.teleopfsm
// in Github commit 90e7194 of the project FtcDecode_11.1.0_RR_4348.

// The demonstration includes 1) the intake of artifacts, with the support
// of temporary outtake, up to the point that the driver interrupts intake
// or the Revolver is full and 2) the raising of the lifter.
public class DecodeTeleOpFSM {

    private static final String TAG = DecodeTeleOpFSM.class.getSimpleName();

    // Finite state machine.
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

    private final GenericFSM6<DecodeTeleOpState, DecodeTeleOpEvent> FSM6 =
            new GenericFSM6<>(DecodeTeleOpState.START, DecodeTeleOpState.class, DecodeTeleOpEvent.class);

    private Controller f310Gamepad1;
    private Controller f310Gamepad2;

    // Intake
    private final FTCButton intakeButton;
    private final IntakeMotion intakeMotion;
    private CompletableFuture<Integer> intakeFuture;
    private int artifactsToIntake;
    private int artifactsInRevolver = 0;

    private final FTCButton outtakeButton;
    private final FTCButton lifterButton;
    private final FTCButton stopLifterButton;
    private Instant lifterTimerStart;
    private static final long lifterDone = 5; // simulate a five-second lift

    private final FTCButton exitButton;


    public DecodeTeleOpFSM(int pNumGamepads, int pArtifactsToIntake) {
        String logDirPath = WorkingDirectory.getWorkingDirectory() + RobotConstants.logDir;
        RobotLogCommon.OpenStatus openStatus = RobotLogCommon.initialize(RobotLogCommon.LogIdentifier.TELEOP_LOG,
                RobotLogCommon.LoggingMode.MIRROR_TO_SYSOUT, logDirPath);
        if (openStatus != RobotLogCommon.OpenStatus.NEW_LOGGER_CREATED)
            throw new AutonomousRobotException(TAG, "Logger not initialized");

        RobotLogCommon.c(TAG, "Constructing DecodeTeleOpFSM");

        artifactsToIntake = pArtifactsToIntake;

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

        intakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_A));
        outtakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_B));
        lifterButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_Y));
        stopLifterButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_LEFT_BUMPER));
        exitButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_X));

        // Set up all states and transitions.
        initializeFSM();

        // Initialize simulated intake.
        intakeMotion = new IntakeMotion();

        RobotLogCommon.c(TAG, "Finished constructing DecodeTeleOpFSM");
    }

    public void runIntakeFSM() throws Exception {
        try {
            DecodeTeleOpState previousCurrentState;
            DecodeTeleOpEvent previousEvent;
            DecodeTeleOpEvent nextEvent = DecodeTeleOpEvent.GET_GAMEPAD_EVENT;

            RobotLogCommon.d(TAG, "Starting FSM at state " + FSM6.getCurrentState() + ", next event " + nextEvent);

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

                previousCurrentState = FSM6.getCurrentState();
                previousEvent = nextEvent;

                // Move the FSM.
                nextEvent = FSM6.processEvent(nextEvent);

                // Limit logging to a change in state or event.
                if (FSM6.getCurrentState() != previousCurrentState || nextEvent != previousEvent) {
                    RobotLogCommon.d(TAG, "FSM new current state " + FSM6.getCurrentState() + ", next event " + nextEvent);
                }
            }

            RobotLogCommon.d(TAG, "Done traversing the state machine at state " + FSM6.getCurrentState());
            RobotLogCommon.d(TAG, "Artifacts in the Revolver " + artifactsInRevolver);
        } finally {
            RobotLogCommon.d(TAG, "In finally() block");
            intakeMotion.stopIntakeThread();
        }
    }

    private void initializeFSM() {
        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.GET_GAMEPAD_EVENT,
                // Button press to turn intake ON. Assume the Revolver is empty.
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> intakeButton.is(FTCButton.State.TAP),
                        // Action
                        () -> {
                            intakeOnAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ));

        // Intake is running.
        FSM6.defineTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, DecodeTeleOpEvent.GET_GAMEPAD_EVENT, new ArrayList<>(Arrays.asList(
                // The driver is still holding the intake button but intake
                // has completed automatically because the Revolver is full.
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        // Guard condition
                        () -> intakeButton.is(FTCButton.State.HELD) && intakeFuture.isDone(),
                        // Action
                        () -> {
                            intakeDoneAction(); // sets the artifactsInRevolver field
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ),

                // The driver has released the intake button; stop intake.
                // The Revolver may or may not be full.
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        // Guard condition
                        () -> intakeButton.is(FTCButton.State.UP),
                        // Action
                        () -> {
                            intakeOffAction();
                            intakeDoneAction(); // sets the artifactsInRevolver field
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ),

                // Turn outtake ON while intake is in progress.
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> outtakeButton.is(FTCButton.State.TAP),
                        // Action
                        () -> {
                            outtakeOnDuringIntakeAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ))));

        // Intake is complete: the revolver is full or intake has
        // been turned off with 0 - 2 artifacts in the revolver.
        FSM6.defineTransition(DecodeTeleOpState.INTAKE_DONE, DecodeTeleOpEvent.GET_GAMEPAD_EVENT, new ArrayList<>(Arrays.asList(
                // For the demonstration, exit on a button press.
                new GenericFSM6.Transition<>(DecodeTeleOpState.FINISH,
                        // Guard condition
                        () -> exitButton.is(FTCButton.State.TAP),
                        // Action
                        () -> DecodeTeleOpEvent.EXIT
                ),

                // Check button press to turn intake back ON; the revolver must not be full.
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> intakeButton.is(FTCButton.State.TAP) && !revolverIsFull(),
                        // Action
                        () -> {
                            intakeOnAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ),

                // Turn outtake ON when intake is not running.
                new GenericFSM6.Transition<>(DecodeTeleOpState.OUTTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> outtakeButton.is(FTCButton.State.TAP),
                        // Action
                        () -> {
                            outtakeOnAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ),

                // Start the lifter.
                new GenericFSM6.Transition<>(DecodeTeleOpState.LIFTER_IN_PROGRESS,
                        // Guard condition
                        () -> lifterButton.is(FTCButton.State.DOUBLE_TAP),
                        // Action
                        () -> {
                            startLifterAction();
                            return DecodeTeleOpEvent.CHECK_LIFTER_DONE;
                        }
                ))));

        // Outtake when intake is *not* running.
        FSM6.defineTransition(DecodeTeleOpState.OUTTAKE_IN_PROGRESS, DecodeTeleOpEvent.GET_GAMEPAD_EVENT,
                // The driver cancels outtake by letting go of the button.
                new GenericFSM6.Transition<>(DecodeTeleOpState.OUTTAKE_DONE,
                        // Guard condition
                        () -> outtakeButton.is(FTCButton.State.UP),
                        // Action
                        () -> {
                            outtakeOffAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ));

        // Outtake has been turned OFF.
        // At this point the revolver may contain from 0 to 3 artifacts.
        FSM6.defineTransition(DecodeTeleOpState.OUTTAKE_DONE, DecodeTeleOpEvent.GET_GAMEPAD_EVENT, new ArrayList<>(Arrays.asList(
                // Check button press to turn intake back ON; the revolver must not be full.
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> intakeButton.is(FTCButton.State.TAP) && !revolverIsFull(),
                        // Action
                        () -> {
                            intakeOnAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ),

                // Turn outtake ON (intake is not running).
                new GenericFSM6.Transition<>(DecodeTeleOpState.OUTTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> outtakeButton.is(FTCButton.State.TAP),
                        // Action
                        () -> {
                            outtakeOnAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ))));

        // Outtake when intake is paused.
        FSM6.defineTransition(DecodeTeleOpState.INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS, DecodeTeleOpEvent.GET_GAMEPAD_EVENT,
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> outtakeButton.is(FTCButton.State.UP),
                        // Action
                        () -> {
                            outtakeOffDuringIntakeAction();
                            return DecodeTeleOpEvent.GET_GAMEPAD_EVENT;
                        }
                ));

        // The lifter is running; wait for completion.
        // Demonstrates the use of an event other than GET_GAMEPAD_EVENT.
        FSM6.defineTransition(DecodeTeleOpState.LIFTER_IN_PROGRESS, DecodeTeleOpEvent.CHECK_LIFTER_DONE, new ArrayList<>(Arrays.asList(
                // The driver lets the lifter run to completion.
                new GenericFSM6.Transition<>(DecodeTeleOpState.FINISH,
                        // Guard condition
                        this::checkLiftDone,
                        // Action
                        () -> DecodeTeleOpEvent.EXIT
                ),

                // The driver requests an immediate stop.
                new GenericFSM6.Transition<>(DecodeTeleOpState.FINISH,
                        // Guard condition
                        () -> stopLifterButton.is(FTCButton.State.TAP),
                        // Action
                        () -> DecodeTeleOpEvent.EXIT
                ))));
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
