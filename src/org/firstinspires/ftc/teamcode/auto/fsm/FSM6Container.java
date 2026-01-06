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
        INTAKE_IN_PROGRESS, INTAKE_DONE, AIM,
        FINISH
    }

    private enum DecodeTeleOpEvent {
        GET_NEXT_EVENT, EXIT
    }

    private final GenericFSM6<DecodeTeleOpState, DecodeTeleOpEvent> FSM6 =
            new GenericFSM6<>(DecodeTeleOpState.START, DecodeTeleOpState.class, DecodeTeleOpEvent.class);

    // Simulate button values and other conditions.
    private enum IntakeState {OFF, ON}

    private final FTCToggleButtonNWay<IntakeState> intakeToggleButton;
    private boolean intakeToggleOn = true;
    private int artifactsInRevolver = MAX_ARTIFACTS_IN_REVOLVER - 1; //**TODO command line?

    // Pattern selection
    private final FTCButton greenSelectionButton;
    private final FTCButton purpleSelectionButton;
    private final FTCButton patternCancellationButton;
    private final FTCButton aimButton;
    private boolean aimButtonValue = true;

    private int iterationCount = 0;

    private final List<RobotConstantsDecode.ArtifactColor> artifactPattern = new ArrayList<>();
    public static final int MAX_ARTIFACTS_IN_REVOLVER = 3;

    public FSM6Container() {
        intakeToggleButton = new FTCToggleButtonNWay<>(() -> intakeToggleOn, EnumSet.allOf(IntakeState.class));
        greenSelectionButton = new FTCButton(() -> false);
        purpleSelectionButton = new FTCButton(() -> true);
        patternCancellationButton = new FTCButton(() -> false);
        aimButton = new FTCButton(() -> aimButtonValue);
    }

    public void testFSM6() throws InterruptedException {
        String logDirPath = WorkingDirectory.getWorkingDirectory() + RobotConstants.logDir;
        RobotLogCommon.OpenStatus openStatus = RobotLogCommon.initialize(RobotLogCommon.LogIdentifier.AUTO_LOG,
                logDirPath);

        FSM6.defineTransition(DecodeTeleOpState.START, DecodeTeleOpEvent.GET_NEXT_EVENT, new ArrayList<>(Arrays.asList(
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        // Guard condition
                        this::revolverIsFull,
                        // Action
                        null
                ),
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> intakeToggleButton.is(FTCButton.State.TAP),
                        // Action
                        () -> {
                            intakeToggleOnAction();
                            return null;
                        }
                ),
                getPatternSelectionTransition(DecodeTeleOpState.START, greenSelectionButton, RobotConstantsDecode.ArtifactColor.GREEN),
                getPatternSelectionTransition(DecodeTeleOpState.START, purpleSelectionButton, RobotConstantsDecode.ArtifactColor.PURPLE),
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
                        this::revolverIsFull,
                        // Action
                        () -> {
                            intakeDoneAction();
                            return null;
                        }
                ),
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        // Guard condition
                        () -> {
                            //**TODO TEMP for testing Since DOUBLE_TAP_INTERVAL_MS = 250, sleep for 300ms
                            System.out.println("Guard at transition to " + DecodeTeleOpState.INTAKE_DONE);
                            System.out.println("Intake toggle state on entry " + intakeToggleButton.getState());
                            intakeToggleOn = false;
                            intakeToggleButton.update();
                            System.out.println("Intake toggle button after set to false " + intakeToggleButton.getState());
                            try {
                                sleep(300);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            intakeToggleOn = true;
                            intakeToggleButton.update();
                            System.out.println("Intake toggle button after set to true " + intakeToggleButton.getState());
                            System.out.println("Intake toggle TAP " + intakeToggleButton.is(FTCButton.State.TAP));
                            return intakeToggleButton.is(FTCButton.State.TAP);
                        },
                        // Action
                        () -> {
                            intakeDoneAction();
                            return null;
                        }
                ),
                getPatternSelectionTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, greenSelectionButton, RobotConstantsDecode.ArtifactColor.GREEN),
                getPatternSelectionTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS, purpleSelectionButton, RobotConstantsDecode.ArtifactColor.PURPLE),
                getPatternCancellationTransition(DecodeTeleOpState.INTAKE_IN_PROGRESS),

                // Default
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        null,
                        // Action
                        () -> {
                            //System.out.println("Transition to " + DecodeTeleOpState.INTAKE_IN_PROGRESS + "; return null");
                            return null;
                        }
                ),
                // Default
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                        null,
                        // Action
                        () -> null
                ))));

        // B: pattern selection after intake ends (by revolver full or driver toggle)
        //  1. the revolver and the pattern may both be full; intake not supported, only cancellation is supported
        //  2. the revolver may be full but the pattern may not be full; intake not supported, adding color(s) supported, cancellation supported
        //  3. the revolver may not be full but the pattern may be full; intake supported, only cancellation supported
        //  4. the revolver and the pattern may both not be full; intake supported, adding color(s) supported, cancellation supported
        FSM6.defineTransition(DecodeTeleOpState.INTAKE_DONE, DecodeTeleOpEvent.GET_NEXT_EVENT, new ArrayList<>(Arrays.asList(
                // Check intake toggle back ON; revolver must not be full.
                new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> intakeToggleButton.is(FTCButton.State.TAP) && !revolverIsFull(),
                        // Action
                        () -> {
                            intakeToggleOnAction();
                            return null;
                        }
                ),
                getPatternSelectionTransition(DecodeTeleOpState.INTAKE_DONE, greenSelectionButton, RobotConstantsDecode.ArtifactColor.GREEN),
                getPatternSelectionTransition(DecodeTeleOpState.INTAKE_DONE, purpleSelectionButton, RobotConstantsDecode.ArtifactColor.PURPLE),
                getPatternCancellationTransition(DecodeTeleOpState.INTAKE_DONE),

                // The driver hits the aim button.
                new GenericFSM6.Transition<>(DecodeTeleOpState.AIM,
                        // Guard condition
                        //**TODO Doesn't work because state is HELD
                        () -> {
                            //**TODO TEMP for testing Since DOUBLE_TAP_INTERVAL_MS = 250, sleep for 300ms
                            System.out.println("Guard at transition to " + DecodeTeleOpState.AIM);
                            System.out.println("Aim button state on entry " + aimButton.getState());
                            aimButtonValue = false;
                            aimButton.update();
                            System.out.println("Aim button after set to false " + aimButton.getState());
                            try {
                                sleep(300);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            aimButtonValue = true;
                            aimButton.update();
                            System.out.println("Aim button after set to true " + aimButton.getState());
                            System.out.println("Aim TAP " + intakeToggleButton.is(FTCButton.State.TAP));
                            return aimButton.is(FTCButton.State.TAP) && artifactsInRevolver != 0;
                        },
                        // Action
                        () -> DecodeTeleOpEvent.EXIT
                ),
                // Default
                 new GenericFSM6.Transition<>(DecodeTeleOpState.INTAKE_DONE,
                    null,
                    // Action
                    () -> null
                ))));

        //**TODO TEMP stop here
        FSM6.defineTransition(DecodeTeleOpState.AIM, DecodeTeleOpEvent.EXIT, DecodeTeleOpState.AIM);


        // ***** STATE MACHINE DEFINITIONS ARE COMPLETE *****
        System.out.println("Starting the state machine at state " + FSM6.getCurrentState());
        RobotLogCommon.d(TAG, "Starting the state machine at state " + FSM6.getCurrentState());

        //**TODO while (linearOpMode.opModeIsActive() && nextEvent != DecodeTeleOpEvent.EXIT) {}

        // Methodology: guard condition methods call update[button]() and
        // return true or false for presence of the condition of interest.

        DecodeTeleOpEvent nextEvent = DecodeTeleOpEvent.GET_NEXT_EVENT;
        while (nextEvent != DecodeTeleOpEvent.EXIT) {
            if (iterationCount++ >= 20) //**TODO stop infinite loops
                break;

            // Updating all button states here is safer even though
            // not all buttons are queried at each step of the process.
            if (nextEvent == DecodeTeleOpEvent.GET_NEXT_EVENT) {
                intakeToggleButton.update();
                greenSelectionButton.update();
                purpleSelectionButton.update();
                patternCancellationButton.update();
                aimButton.update();
            }

            RobotLogCommon.d(TAG, "FSM current state " + FSM6.getCurrentState() + ", event " +
                    nextEvent);
            nextEvent = moveTeleOpFSM(nextEvent);
            RobotLogCommon.d(TAG, "FSM new current state " + FSM6.getCurrentState() + ", next event " + nextEvent);

            // Since DOUBLE_TAP_INTERVAL_MS = 250, sleep for 300ms here
            // to emulate driver normal driver single tap.
            sleep(300);
        }

        RobotLogCommon.d(TAG, "Done traversing the state machine at state " + FSM6.getCurrentState());
        RobotLogCommon.d(TAG, "Artifacts in the Revolver " + artifactsInRevolver);
        RobotLogCommon.d(TAG, "Artifact pattern " + artifactPattern);
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
            } else {
                nextEvent = processEventOutput.second;
                RobotLogCommon.d(TAG, "Internal event " + processEventOutput.second + " supplied by an action routine");
            }
        }

        return nextEvent;
    }

    private GenericFSM6.Transition<DecodeTeleOpState, DecodeTeleOpEvent> getPatternSelectionTransition(DecodeTeleOpState pNextState,
                                                                                                       FTCButton pColorButton, RobotConstantsDecode.ArtifactColor pColor) {
        return new GenericFSM6.Transition<>(pNextState,
                // Guard condition
                () -> pColorButton.is(FTCButton.State.TAP) && artifactPattern.size() < RobotConstantsDecode.MAX_ARTIFACTS_IN_PATTERN,
                // Action
                () -> {
                    artifactPattern.add(pColor);
                    RobotLogCommon.d(TAG, pColor + " added to artifact pattern");
                    return null;
                }
        );
    }

    private GenericFSM6.Transition<DecodeTeleOpState, DecodeTeleOpEvent> getPatternCancellationTransition(DecodeTeleOpState pNextState) {
        return new GenericFSM6.Transition<>(DecodeTeleOpState.START,
                // Guard condition
                () -> patternCancellationButton.is(FTCButton.State.TAP),
                // Action
                () -> {
                    RobotLogCommon.d(TAG,"Artifact pattern cancelled");
                    artifactPattern.clear();
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
        //**TODO turn off intake servos, set intake toggle OFF, etc.
    }

    private void patternCancellationButtonAction() {
        //**TODO patternCancellationButtonAction
    }

}
