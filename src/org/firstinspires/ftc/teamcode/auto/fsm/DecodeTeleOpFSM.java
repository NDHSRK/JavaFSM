package org.firstinspires.ftc.teamcode.auto.fsm;


// Demonstration of the use of a Finite State Machine in TeleOp for the
// Decode game. Based on public class DecodeTeleOpFSM extends TeleOpBase
// in the package package org.firstinspires.ftc.teamcode.teleop.opmodes.test.teleopfsm
// in Github commit 90e7194 of the project FtcDecode_11.1.0_RR_4348.

// The demonstration is limited to the intake of artifacts, with the support
// of temporary outtake, up to the point that the driver interrupts intake
// or the Revolver is full.

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.ftcdevcommon.Pair;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.WorkingDirectory;
import org.firstinspires.ftc.teamcode.auto.FTCButton;
import org.firstinspires.ftc.teamcode.auto.FTCGamepad;
import org.firstinspires.ftc.teamcode.auto.RobotConstants;

public class DecodeTeleOpFSM {

    private static final String TAG = DecodeTeleOpFSM.class.getSimpleName();

    // Finite state machine.
    private enum DecodeTeleOpState {
        START,
        INTAKE_IN_PROGRESS, INTAKE_DONE,
        OUTTAKE_IN_PROGRESS, OUTTAKE_DONE,
        INTAKE_PAUSED_AND_OUTTAKE_IN_PROGRESS,
        FINISH
    }

    private enum DecodeTeleOpEvent {
        GET_NEXT_EVENT, EXIT
    }

    private final GenericFSM6<DecodeTeleOpState, DecodeTeleOpEvent> FSM6 =
            new GenericFSM6<>(DecodeTeleOpState.START, DecodeTeleOpState.class, DecodeTeleOpEvent.class);

    // Starting contents of the revolver assuming it is empty.
    // The representation of the revolver is from the point of view of an observer
    // standing behind the robot.
    // private final EnumMap<RevolverMotion.RevolverTrackingPosition, RevolverMotion.RevolverSlotInfo> revolverTracking = new EnumMap<>(Map.of(
    //         RevolverMotion.RevolverTrackingPosition.REAR_VIEW_LEFT, new RevolverMotion.RevolverSlotInfo(RobotConstantsDecode.ArtifactColor.NPOS, RevolverServo.RevolverSlot.SLOT_1),
    //         RevolverMotion.RevolverTrackingPosition.REAR_VIEW_CENTER, new RevolverMotion.RevolverSlotInfo(RobotConstantsDecode.ArtifactColor.NPOS, RevolverServo.RevolverSlot.SLOT_0),
    //         RevolverMotion.RevolverTrackingPosition.REAR_VIEW_RIGHT, new RevolverMotion.RevolverSlotInfo(RobotConstantsDecode.ArtifactColor.NPOS, RevolverServo.RevolverSlot.SLOT_2)
    // ));

    // Gamepad controllers.
    private Controller f310Gamepad1;
    private Controller f310Gamepad2;

    // Intake
    private final FTCButton intakeButton;
    //private final IntakeMotion intakeMotion;
    //private CompletableFuture<Integer> intakeFuture;

    private final FTCButton outtakeButton;

    //**TODO The number of artifacts in the revolver after intake comes from the command line.
    //private final RevolverMotion revolverMotion;
    private int artifactsInRevolver = 0;


    public DecodeTeleOpFSM() {
        String logDirPath = WorkingDirectory.getWorkingDirectory() + RobotConstants.logDir;
        RobotLogCommon.OpenStatus openStatus = RobotLogCommon.initialize(RobotLogCommon.LogIdentifier.AUTO_LOG,
                logDirPath);
        RobotLogCommon.c(TAG, "Constructing DecodeTeleOpFSM");

        // Connect with the gamepad(s).
        //**TODO Put the number of gamepads on the command line.
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller c : controllers) {
            if (c.getName().contains("Logitech") || c.getName().contains("F310")) {
                System.out.println("Found a Logitech controller with the name " + c.getName());
                if (f310Gamepad1 == null)
                    f310Gamepad1 = c; // gamepad 1
                else
                    f310Gamepad2 = c; // gamepad 2
                break;
            }
        }

        if (f310Gamepad1 == null)
            throw new AutonomousRobotException(TAG, "No F310 controllers found!");

        intakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_A));
        outtakeButton = new FTCButton(() -> FTCGamepad.gamepadButtonPressed(f310Gamepad1, FTCGamepad.FTCButtonId.GAMEPAD_X));


        // Set up all states and transitions.
        initializeFSM();

        RobotLogCommon.c(TAG, "Finished constructing DecodeTeleOpFSM");
    }

    public void runIntakeFSM() throws Exception {
        try {
            DecodeTeleOpState previousState;
            DecodeTeleOpEvent previousNextEvent;
            DecodeTeleOpEvent nextEvent = DecodeTeleOpEvent.GET_NEXT_EVENT;
            RobotLogCommon.d(TAG, "Starting FSM at state " + FSM6.getCurrentState() + ", next event " + nextEvent);

            while (nextEvent != DecodeTeleOpEvent.EXIT) {
                // Updating all button states here is safer even though
                // not all buttons are queried at each step of the process.
                if (nextEvent == DecodeTeleOpEvent.GET_NEXT_EVENT) {
                    intakeButton.update();
                    outtakeButton.update();
                }

                previousState = FSM6.getCurrentState();
                previousNextEvent = nextEvent;

                // Move the FSM.
                nextEvent = moveTeleOpFSM(nextEvent);

                // Limit logging to a change in state or event.
                if (FSM6.getCurrentState() != previousState || nextEvent != previousNextEvent) {
                    RobotLogCommon.d(TAG, "FSM previous state " + previousState + ", previous next event " + previousNextEvent);
                    RobotLogCommon.d(TAG, "FSM new current state " + FSM6.getCurrentState() + ", next event " + nextEvent);
                }
            }

            RobotLogCommon.d(TAG, "Done traversing the state machine at state " + FSM6.getCurrentState());
            RobotLogCommon.d(TAG, "Artifacts in the Revolver " + artifactsInRevolver);
        } finally {
            RobotLogCommon.d(TAG, "In finally() block");
            //intakeMotion.stopIntakeThread();
        }
    }

    private DecodeTeleOpEvent moveTeleOpFSM(DecodeTeleOpEvent pEvent) throws Exception {
        DecodeTeleOpEvent nextEvent;
        Pair<DecodeTeleOpState, DecodeTeleOpEvent> processEventOutput = FSM6.processEvent(pEvent);
        if (processEventOutput.first == null)
            throw new AutonomousRobotException(TAG, "No transition for state " + FSM6.getCurrentState() + " and event " + pEvent);
        else {
            if (processEventOutput.second == null) {
                nextEvent = DecodeTeleOpEvent.GET_NEXT_EVENT;
            } else {
                nextEvent = processEventOutput.second;
                RobotLogCommon.d(TAG, "Internal event " + processEventOutput.second + " supplied by an action routine");
            }
        }

        return nextEvent;
    }

    private void initializeFSM() {

    }

}
