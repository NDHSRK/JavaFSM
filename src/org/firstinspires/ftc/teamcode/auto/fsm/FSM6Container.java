package org.firstinspires.ftc.teamcode.auto.fsm;

import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.ftcdevcommon.Pair;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.WorkingDirectory;
import org.firstinspires.ftc.teamcode.auto.FTCButton;
import org.firstinspires.ftc.teamcode.auto.FTCToggleButtonNWay;
import org.firstinspires.ftc.teamcode.auto.RobotConstants;
import org.firstinspires.ftc.teamcode.auto.RobotConstantsDecode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.lang.Thread.sleep;

public class FSM6Container {
    private static final String TAG = FSM6Container.class.getSimpleName();

    private enum DecodeTeleOpState {
        START,
        INTAKE_IN_PROGRESS, INTAKE_COMPLETE,
        FINISH
    }

    private enum DecodeTeleOpEvent {
        OPMODE_INACTIVE, GET_NEXT_EVENT,
        INTAKE_STARTED, INTAKE_DONE,
        FINISH
    }

    private final GenericFSM6<DecodeTeleOpState, DecodeTeleOpEvent> FSM6 =
            new GenericFSM6<>(DecodeTeleOpState.START, DecodeTeleOpState.class, DecodeTeleOpEvent.class);

    enum PatternTimerState {PATTERN_TIMER_NOT_RUNNING, PATTERN_TIMER_RUNNING}

    enum PatternTimerEvent {
        GET_NEXT_EVENT, START_PATTERN_TIMER, CHECK_PATTERN_TIMER_EXPIRED,
        ALL_OTHER
    }

    private final GenericFSM6<PatternTimerState, PatternTimerEvent> patternTimerFSM =
            new GenericFSM6<>(PatternTimerState.PATTERN_TIMER_NOT_RUNNING, PatternTimerState.class, PatternTimerEvent.class);

    // Simulate button values and other conditions.
    private enum IntakeState {OFF, ON}

    private final FTCToggleButtonNWay<IntakeState> intakeToggleButton;
    private boolean intakeToggleOn = true;
    private int artifactsInRevolver = MAX_ARTIFACTS_IN_REVOLVER;

    // Pattern selection
    private FTCButton greenSelectionButton;
    private FTCButton purpleSelectionButton;
    private FTCButton patternConfirmationButton;
    private FTCButton patternCancellationButton;

    enum ArtifactColor {GREEN, PURPLE}

    private final List<ArtifactColor> artifactPattern = new ArrayList<>();
    public static final int MAX_ARTIFACTS_IN_REVOLVER = 3;
    private boolean customPatternTimerStarted = false;

    public FSM6Container() {
        intakeToggleButton = new FTCToggleButtonNWay<>(() -> false, EnumSet.allOf(IntakeState.class));

        greenSelectionButton = new FTCButton(() -> false);
        purpleSelectionButton = new FTCButton(() -> false);
        patternConfirmationButton = new FTCButton(() -> false);
        patternCancellationButton = new FTCButton(() -> false);
    }

    public void testFSM6() {
        String logDirPath = WorkingDirectory.getWorkingDirectory() + RobotConstants.logDir;
        RobotLogCommon.OpenStatus openStatus = RobotLogCommon.initialize(RobotLogCommon.LogIdentifier.AUTO_LOG,
                logDirPath);

        //**TODO Guard condition methods call [button].update() and
        // return true or false for presence of the condition of interest.
        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.OPMODE_INACTIVE, DecodeTeleOpState.FINISH);

        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.GET_NEXT_EVENT, new ArrayList<>(Arrays.asList(
                FSM6.new Transition(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> {
                            intakeToggleButton.update();
                            if (!intakeToggleButton.is(FTCButton.State.TAP) || revolverIsFull())
                                return false;
                            return true;
                        },
                        // Action
                        () -> {
                            intakeToggleOnAction();
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }
                ),
                FSM6.new Transition(DecodeTeleOpState.START,
                        // Guard condition
                        () -> {
                            greenSelectionButton.update();
                            if (!greenSelectionButton.is(FTCButton.State.TAP))
                                return false;
                            return true;
                        },
                        // Action
                        //**TODO selectionButtonAction will move the patternSelectionFSM.
                        () -> {
                            selectionButtonAction(RobotConstantsDecode.ArtifactColor.GREEN);
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }
                ),
                FSM6.new Transition(DecodeTeleOpState.START,
                        // Guard condition
                        () -> {
                            purpleSelectionButton.update();
                            if (!purpleSelectionButton.is(FTCButton.State.TAP))
                                return false;
                            return true;
                        },
                        // Action
                        //**TODO selectionButtonAction will move the patternSelectionFSM.
                        () -> {
                            selectionButtonAction(RobotConstantsDecode.ArtifactColor.PURPLE);
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }
                ),
                FSM6.new Transition(DecodeTeleOpState.START,
                        // Guard condition
                        () -> {
                            patternConfirmationButton.update();
                            if (!patternConfirmationButton.is(FTCButton.State.TAP))
                                return false;
                            return true;
                        },
                        // Action
                        //**TODO patternConfirmationButtonAction will move the patternSelectionFSM.
                        () -> {
                            patternConfirmationButtonAction();
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }
                ),
                FSM6.new Transition(DecodeTeleOpState.START,
                        // Guard condition
                        () -> {
                            patternCancellationButton.update();
                            if (!patternCancellationButton.is(FTCButton.State.TAP))
                                return false;
                            return true;
                        },
                        // Action
                        //**TODO cancellationButtonAction will move the patternSelectionFSM.
                        () -> {
                            patternCancellationButtonAction();
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }
                ),
                // Default
                FSM6.new Transition(DecodeTeleOpState.START,
                        null,
                        // Action
                        () -> {
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }
                ))));

        FSM6.defineTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, DecodeTeleOpEvent.OPMODE_INACTIVE, DecodeTeleOpState.FINISH);

        FSM6.defineTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, DecodeTeleOpEvent.GET_NEXT_EVENT,
                FSM6.new Transition(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        null,
                        // Action
                        () -> {
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }));

        //**TODO What to do about the pattern timer?


        //**TODO TEMP
        FSM6.defineTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, DecodeTeleOpEvent.INTAKE_DONE, DecodeTeleOpState.START);


        // ***** STATE MACHINE DEFINITIONS ARE COMPLETE *****

        //**TODO No need for a loop but do need to handle an
        // OPMDDE_INACTIVE event throughout the FSM.

        //**TODO Intake can be restarted if the revolver is *not* full [where is
        // this tested in the FSM?].

        System.out.println("Starting the state machine at state " + FSM6.getCurrentState());
        RobotLogCommon.d(TAG, "Starting the state machine at state " + FSM6.getCurrentState());
        Pair<DecodeTeleOpState, DecodeTeleOpEvent> processEventOutput = FSM6.processEvent(DecodeTeleOpEvent.GET_NEXT_EVENT);
        if (processEventOutput.first == null)
            throw new AutonomousRobotException(TAG, "No transition for state " + FSM6.getCurrentState() + " and event " + DecodeTeleOpEvent.GET_NEXT_EVENT);
        else {
            RobotLogCommon.d(TAG, "New current state " + processEventOutput.first);
            if (processEventOutput.second == null)
                RobotLogCommon.d(TAG, "No internal event supplied by an action routine");
            else
                RobotLogCommon.d(TAG, "Internal event " + processEventOutput.second + " supplied by an action routine");
        }

        Pair<DecodeTeleOpState, DecodeTeleOpEvent> processEvent2Output = null;
        if (processEventOutput.second != null) {
            processEvent2Output = FSM6.processEvent(processEventOutput.second);
            if (processEvent2Output.first == null)
                throw new AutonomousRobotException(TAG, "No transition for state " + FSM6.getCurrentState() + " and event " + processEventOutput.second);
            else {
                RobotLogCommon.d(TAG, "New current state " + processEvent2Output.first);
                if (processEvent2Output.second == null)
                    RobotLogCommon.d(TAG, "No internal event supplied by an action routine");
                else
                    RobotLogCommon.d(TAG, "Internal event " + processEvent2Output.second + " supplied by an action routine");
            }
        }

        Pair<DecodeTeleOpState, DecodeTeleOpEvent> processEvent3Output = null;
        if (processEvent2Output.second != null) {
            processEvent3Output = FSM6.processEvent(processEvent2Output.second);
            if (processEvent3Output.first == null)
                throw new AutonomousRobotException(TAG, "No transition for state " + FSM6.getCurrentState() + " and event " + processEventOutput.second);
            else {
                RobotLogCommon.d(TAG, "New current state " + processEvent3Output.first);
                if (processEvent3Output.second == null)
                    RobotLogCommon.d(TAG, "No internal event supplied by an action routine");
                else
                    RobotLogCommon.d(TAG, "Internal event " + processEvent3Output.second + " supplied by an action routine");
            }
        }

        Pair<DecodeTeleOpState, DecodeTeleOpEvent> processEvent4Output = null;
        if (processEvent3Output.second != null) {
            processEvent4Output = FSM6.processEvent(processEvent3Output.second);
            if (processEvent4Output.first == null)
                throw new AutonomousRobotException(TAG, "No transition for state " + FSM6.getCurrentState() + " and event " + processEventOutput.second);
            else {
                RobotLogCommon.d(TAG, "New current state " + processEvent4Output.first);
                if (processEvent4Output.second == null)
                    RobotLogCommon.d(TAG, "No internal event supplied by an action routine");
                else
                    RobotLogCommon.d(TAG, "Internal event " + processEvent4Output.second + " supplied by an action routine");
            }
        }

        RobotLogCommon.closeLog();
    }

    private boolean revolverIsFull() {
        return artifactsInRevolver == MAX_ARTIFACTS_IN_REVOLVER;
    }

    private void intakeToggleOnAction() {
        //**TODO intakeToggleOnAction
    }

    private void intakeToggleOffAction() {
        //**TODO intakeToggleOffAction
    }

    private void patternConfirmationButtonAction() {
        //**TODO patternConfirmationButtonAction
    }

    private void patternCancellationButtonAction() {
        //**TODO patternCancellationButtonAction
    }


    private void selectionButtonAction(RobotConstantsDecode.ArtifactColor pColor) {
//**TODO Embed the logic for custom pattern selection in the patternSelectionFSM.
    }

    ;

}
