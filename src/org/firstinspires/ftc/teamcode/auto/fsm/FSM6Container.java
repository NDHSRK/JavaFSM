package org.firstinspires.ftc.teamcode.auto.fsm;

import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.ftcdevcommon.Pair;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.WorkingDirectory;
import org.firstinspires.ftc.teamcode.auto.RobotConstants;

import java.util.ArrayList;
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
        GET_NEXT_EVENT,
        INTAKE_STARTED, INTAKE_DONE,
        FINISH, ALL_OTHER //**TODO -> DEFAULT
    }

    private final GenericFSM6<DecodeTeleOpState, DecodeTeleOpEvent> FSM6 =
            new GenericFSM6<>(DecodeTeleOpState.START, DecodeTeleOpState.class, DecodeTeleOpEvent.class);

    enum PatternTimerState {PATTERN_TIMER_NOT_RUNNING, PATTERN_TIMER_RUNNING}
    enum PatternTimerEvent {GET_NEXT_EVENT, START_PATTERN_TIMER, CHECK_PATTERN_TIMER_EXPIRED,
    ALL_OTHER}

    private final GenericFSM6<PatternTimerState, PatternTimerEvent> patternTimerFSM =
            new GenericFSM6<>(PatternTimerState.PATTERN_TIMER_NOT_RUNNING, PatternTimerState.class, PatternTimerEvent.class);

    // Simulate button values and other conditions.
    private enum IntakeToggle {OFF, ON}

    private IntakeToggle intakeToggle = IntakeToggle.OFF;
    private boolean intakeToggleOn = true;
    private int artifactsInRevolver = MAX_ARTIFACTS_IN_REVOLVER;

    // Pattern selection
    private boolean patternSelectionGreenOn = false;
    private boolean patternSelectionPurpleOn = false;
    private boolean patternConfirmationOn = false;
    private boolean patternCancellationOn = false;

    enum ArtifactColor {GREEN, PURPLE}

    private final List<ArtifactColor> artifactPattern = new ArrayList<>();
    public static final int MAX_ARTIFACTS_IN_REVOLVER = 3;
    private boolean customPatternTimerStarted = false;

    public FSM6Container() {
    }

    public void testFSM6() {
        String logDirPath = WorkingDirectory.getWorkingDirectory() + RobotConstants.logDir;
        RobotLogCommon.OpenStatus openStatus = RobotLogCommon.initialize(RobotLogCommon.LogIdentifier.AUTO_LOG,
                logDirPath);

        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.GET_NEXT_EVENT,
                FSM6.new Transition(DecodeTeleOpState.START,
                        // Guard condition
                        null,
                        // Action
                        //**TODO Do you want to look at the buttons
                        // and conditions here or get the Event from
                        // a Monitor? A: it's clearer to look at the
                        // buttons and conditions here.
                        () -> {
                            if (updateIntakeOn()) {
                                return DecodeTeleOpEvent.INTAKE_STARTED;
                            }
                            if (updatePatternSelectionGreen()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            if (updatePatternSelectionPurple()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            if (updatePatternConfirmation()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            if (updatePatternCancellation()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }));

        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.INTAKE_STARTED,
                FSM6.new Transition(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        null,
                        // Action
                        () -> {
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }
                ));

        //**TODO This will return a null next state Catch-all for all other events applied to this state.
        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.ALL_OTHER, DecodeTeleOpState.START);

        FSM6.defineTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, DecodeTeleOpEvent.GET_NEXT_EVENT,
                FSM6.new Transition(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        null,
                        // Action
                        () -> {
                            if (revolverIsFull() || updateIntakeOff()) {
                                return DecodeTeleOpEvent.INTAKE_DONE;
                            }
                            if (updatePatternSelectionGreen()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            if (updatePatternSelectionPurple()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            if (updatePatternConfirmation()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            if (updatePatternCancellation()) {
                                return DecodeTeleOpEvent.GET_NEXT_EVENT;
                            }
                            return DecodeTeleOpEvent.GET_NEXT_EVENT;
                        }));

        //**TODO What to do about the pattern timer?
        /*
               // Custom pattern selection.
        // The driver must select 3 colors within the timeout value.
        // If customPatternTimerStarted is true and the timer has expired
        // simply reset the current pattern by artifactPattern.clear()
        // and set the customPatternTimerStarted to false.
        if (customPatternTimerStarted && customPatternTimer.milliseconds() >= PATTERN_TIMEOUT) {
            artifactPattern.clear();
            customPatternTimerStarted = false;
            RobotLogCommon.d(TAG, "Pattern selection timed out");
        }
         */

        //**TODO TEMP
        FSM6.defineTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, DecodeTeleOpEvent.INTAKE_DONE, DecodeTeleOpState.START);


        // ***** STATE MACHINE DEFINITIONS ARE COMPLETE *****

        //**TODO Need a loop that ends when ...
        // INTAKE_IN_PROGRESS loops until the revolver is full or the
        // driver toggles intake to OFF; at that point intake is complete.
        // Intake can be restarted if the revolver is *not* full [where is
        // this tested in the FSM?].
        //**TODO STOPPED HERE 11/29/25 15:45 ...

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

    private boolean updateIntakeOn() {
        return intakeToggleOn;
    }

    private boolean revolverIsFull() {
        return artifactsInRevolver == MAX_ARTIFACTS_IN_REVOLVER;
    }

    private boolean updateIntakeOff() {
        // Check future complete (revolver full) or toggle to OFF
        return !intakeToggleOn;
    }

    private boolean updatePatternSelectionGreen() {
        return patternSelectionGreenOn;
    }

    private boolean updatePatternSelectionPurple() {
        return patternSelectionPurpleOn;
    }

    private boolean updatePatternConfirmation() {
        return patternConfirmationOn;
    }

    private boolean updatePatternCancellation() {
        return patternCancellationOn;
    }

}
