package org.firstinspires.ftc.teamcode.auto.fsm;

import org.firstinspires.ftc.ftcdevcommon.AutonomousRobotException;
import org.firstinspires.ftc.ftcdevcommon.Pair;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.WorkingDirectory;
import org.firstinspires.ftc.teamcode.auto.FTCButton;
import org.firstinspires.ftc.teamcode.auto.FTCToggleButtonNWay;
import org.firstinspires.ftc.teamcode.auto.RobotConstants;
import org.firstinspires.ftc.teamcode.auto.RobotConstantsDecode;

import java.util.*;

import static java.lang.Thread.sleep;

public class FSM6Container {
    private static final String TAG = FSM6Container.class.getSimpleName();

    private enum DecodeTeleOpState {
        START,
        INTAKE_IN_PROGRESS, INTAKE_DONE,
        FINISH
    }

    private enum DecodeTeleOpEvent {
        GET_NEXT_EVENT, EXIT
    }

    private final GenericFSM6<DecodeTeleOpState, DecodeTeleOpEvent> FSM6 =
            new GenericFSM6<>(DecodeTeleOpState.START, DecodeTeleOpState.class, DecodeTeleOpEvent.class);

    enum PatternTimerState {PATTERN_TIMER_NOT_RUNNING, PATTERN_TIMER_RUNNING}

    enum PatternTimerEvent {
        GET_NEXT_EVENT, START_PATTERN_TIMER, CHECK_PATTERN_TIMER_EXPIRED
    }

    private final GenericFSM6<PatternTimerState, PatternTimerEvent> patternTimerFSM =
            new GenericFSM6<>(PatternTimerState.PATTERN_TIMER_NOT_RUNNING, PatternTimerState.class, PatternTimerEvent.class);

    // Simulate button values and other conditions.
    private enum IntakeState {OFF, ON}

    private final FTCToggleButtonNWay<IntakeState> intakeToggleButton;
    private boolean intakeToggleOn = true;
    private int artifactsInRevolver = MAX_ARTIFACTS_IN_REVOLVER - 1;

    // Pattern selection
    private final FTCButton greenSelectionButton;
    private final FTCButton purpleSelectionButton;
    private final FTCButton patternConfirmationButton;
    private final FTCButton patternCancellationButton;

    enum ArtifactColor {GREEN, PURPLE}

    private final List<ArtifactColor> artifactPattern = new ArrayList<>();
    public static final int MAX_ARTIFACTS_IN_REVOLVER = 2; //**TODO command line?
    private boolean customPatternTimerStarted = false;

    public FSM6Container() {
        intakeToggleButton = new FTCToggleButtonNWay<>(() -> true, EnumSet.allOf(IntakeState.class));
        greenSelectionButton = new FTCButton(() -> false);
        purpleSelectionButton = new FTCButton(() -> false);
        patternConfirmationButton = new FTCButton(() -> false);
        patternCancellationButton = new FTCButton(() -> false);
    }

    public void testFSM6() {
        String logDirPath = WorkingDirectory.getWorkingDirectory() + RobotConstants.logDir;
        RobotLogCommon.OpenStatus openStatus = RobotLogCommon.initialize(RobotLogCommon.LogIdentifier.AUTO_LOG,
                logDirPath);

        // Methodology: guard condition methods call [button].update() and
        // return true or false for presence of the condition of interest.

        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.GET_NEXT_EVENT, new ArrayList<>(Arrays.asList(
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        // Guard condition
                        () -> artifactsInRevolver == MAX_ARTIFACTS_IN_REVOLVER,
                        // Action
                        null
                ),
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> {
                            intakeToggleButton.update();
                            return intakeToggleButton.is(FTCButton.State.TAP);
                        },
                        // Action
                        () -> {
                            intakeToggleOnAction();
                            return null;
                        }
                ),
                getPatternSelectionTransition(DecodeTeleOpState.START,RobotConstantsDecode.ArtifactColor.GREEN),
                getPatternSelectionTransition(DecodeTeleOpState.START,RobotConstantsDecode.ArtifactColor.PURPLE),
                getPatternConfirmationTransition(DecodeTeleOpState.START),
                getPatternCancellationTransition(DecodeTeleOpState.START),

                // Default
                new GenericFSM6.Transition<>(DecodeTeleOpState.START,
                        null,
                        // Action
                        () -> null
                ))));

        FSM6.defineTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, DecodeTeleOpEvent.GET_NEXT_EVENT, new ArrayList<>(Arrays.asList(
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        // Guard condition
                        () -> artifactsInRevolver == MAX_ARTIFACTS_IN_REVOLVER,
                        // Action
                        null
                ),
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        // Guard condition
                        () -> {
                    //**TODO prevents the running of the FSM because the
                            // state of the button is HELD - need temp
                            // method to set the state to OFF.
                            intakeToggleButton.update();
                            return intakeToggleButton.is(FTCButton.State.TAP);
                        },
                        // Action
                        () -> {
                            intakeToggleOffAction();
                            return null;
                        }
                ),
                getPatternSelectionTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS,RobotConstantsDecode.ArtifactColor.GREEN),
                getPatternSelectionTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS,RobotConstantsDecode.ArtifactColor.PURPLE),
                getPatternConfirmationTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS),
                getPatternCancellationTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS),

                // Default
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        null,
                        // Action
                        () -> null
                ))));

        FSM6.defineTransition(DecodeTeleOpState.INTAKE_DONE, DecodeTeleOpEvent.GET_NEXT_EVENT,
                new GenericFSM6.Transition<>(DecodeTeleOpState.FINISH,
                        // Guard condition
                        null,
                        // Action
                        () -> {
                            intakeDoneAction();
                            return DecodeTeleOpEvent.EXIT;
                        }
                ));

        FSM6.defineTransition(DecodeTeleOpState.FINISH, DecodeTeleOpEvent.EXIT, DecodeTeleOpState.FINISH);


        // ***** STATE MACHINE DEFINITIONS ARE COMPLETE *****
        System.out.println("Starting the state machine at state " + FSM6.getCurrentState());
        RobotLogCommon.d(TAG, "Starting the state machine at state " + FSM6.getCurrentState());

        //**TODO while (linearOpMode.opModeIsActive() && nextEvent != DecodeTeleOpEvent.EXIT) {}
        
        DecodeTeleOpEvent nextEvent = DecodeTeleOpEvent.GET_NEXT_EVENT;
        while (nextEvent != DecodeTeleOpEvent.EXIT) {
            //***TODO You could update all button states here
            nextEvent = moveTeleOpFSM(nextEvent);
        }

        RobotLogCommon.d(TAG, "Done traversing the state machine at state " + FSM6.getCurrentState());
        RobotLogCommon.closeLog();
    }

    //**TODO see GenericFSM6 - do not allow null next state?
    // or allow as terminating condition.
    private DecodeTeleOpEvent moveTeleOpFSM(DecodeTeleOpEvent pEvent) {
        DecodeTeleOpEvent nextEvent;
        Pair<DecodeTeleOpState, DecodeTeleOpEvent> processEventOutput = FSM6.processEvent(pEvent);
        if (processEventOutput.first == null)
            throw new AutonomousRobotException(TAG, "No transition for state " + FSM6.getCurrentState() + " and event " + pEvent);
        else {
            RobotLogCommon.d(TAG, "New current state " + processEventOutput.first);
            if (processEventOutput.second == null) {
                nextEvent = DecodeTeleOpEvent.GET_NEXT_EVENT;
                RobotLogCommon.d(TAG, "No event supplied by an action routine; defaulting to GET_NEXT_EVENT");
            }
            else {
                nextEvent = processEventOutput.second;
                RobotLogCommon.d(TAG, "Internal event " + processEventOutput.second + " supplied by an action routine");
            }
        }

        return nextEvent;
    }

    private GenericFSM6.Transition<DecodeTeleOpState, DecodeTeleOpEvent> getPatternSelectionTransition(DecodeTeleOpState pNextState, RobotConstantsDecode.ArtifactColor pColor) {
        return new GenericFSM6.Transition<>(pNextState,
                // Guard condition
                () -> {
            //**TODO either put all calls to update at the top of the next event loop,
                    // which is safer, or update the button that corresponds to pColor.
                    greenSelectionButton.update();
                    return greenSelectionButton.is(FTCButton.State.TAP);
                },
                // Action
                //**TODO selectionButtonAction will move the patternSelectionFSM.
                () -> {
                    selectionButtonAction(pColor);
                    return null;
                }
        );
    }

    private GenericFSM6.Transition<DecodeTeleOpState, DecodeTeleOpEvent> getPatternConfirmationTransition(DecodeTeleOpState pNextState) {
        return new GenericFSM6.Transition<>(pNextState,
                // Guard condition
                () -> {
                    patternConfirmationButton.update();
                    return patternConfirmationButton.is(FTCButton.State.TAP);
                },
                // Action
                //**TODO patternConfirmationButtonAction will move the patternSelectionFSM.
                () -> {
                    patternConfirmationButtonAction();
                    return null;
                }
        );
    }

    private GenericFSM6.Transition<DecodeTeleOpState, DecodeTeleOpEvent> getPatternCancellationTransition(DecodeTeleOpState pNextState) {
        return new GenericFSM6.Transition<>(DecodeTeleOpState.START,
                // Guard condition
                () -> {
                    patternCancellationButton.update();
                    return patternCancellationButton.is(FTCButton.State.TAP);
                },
                // Action
                //**TODO cancellationButtonAction will move the patternSelectionFSM.
                () -> {
                    patternCancellationButtonAction();
                    return null;
                }
        );
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

    private void intakeDoneAction() {
        //**TODO intakeDoneAction from DecodeTeleOp
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

}
