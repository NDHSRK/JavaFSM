package org.firstinspires.ftc.teamcode.auto.fsm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;

public class FSM6Container {

    private enum State {
        START, INTAKE_IN_PROGRESS, INTAKE_COMPLETE
    }

    private enum Event {
        GENERATE_NEXT_EVENT,
        INTAKE_TOGGLE_BUTTON_PRESS, INTAKE_DONE,
        PATTERN_SELECTION_GREEN_BUTTON_PRESS, PATTERN_SELECTION_PURPLE_BUTTON_PRESS,
        PATTERN_CONFIRMATION_BUTTON_PRESS, PATTERN_CANCELLATION_BUTTON_PRESS,
        ALL_OTHER //**TODO or DEFAULT
    }

    private final GenericFSM6<State, Event> FSM6 =
            new GenericFSM6<>(State.START, State.class, Event.class);

    // Simulate button values and other conditions.
    private enum IntakeToggle {OFF, ON}

    private IntakeToggle intakeToggle = IntakeToggle.OFF;
    private boolean intakeToggleTap = true;
    private boolean revolverFull = false;

    // Pattern selection
    private boolean patternSelectionGreenTap = false;
    private boolean patternSelectionPurpleTap = false;
    private boolean patternConfirmationTap = false;
    private boolean patternCancellationTap = false;

    enum ArtifactColor {GREEN, PURPLE}
    private final List<ArtifactColor> artifactPattern = new ArrayList<>();
    public static final int MAX_ARTIFACTS_IN_REVOLVER = 3;
    private boolean customPatternTimerStarted = false;

    //**TODO You're going to need a Monitor state machine that
    // checks button presses and conditions and generates events
    // for the DecodeTeleOpFSM.

    public FSM6Container() {}

    public void testFSM6() {
        FSM6.defineTransition(State.START, Event.GENERATE_NEXT_EVENT,
                new ArrayList<>(Arrays.asList(FSM6.new Transition(State.START,
                        // Guard condition
                        null,
                        // Action
                        //**TODO Do you want to look at the buttons
                        // and conditions here or get the Event from
                        // the Monitor?
                        () -> {
                            if (intakeToggleTap) {
                                return Event.INTAKE_TOGGLE_BUTTON_PRESS;
                            }
                            if (patternSelectionGreenTap) {
                                return Event.PATTERN_SELECTION_GREEN_BUTTON_PRESS;
                            }
                            if (patternSelectionPurpleTap) {
                                return Event.PATTERN_SELECTION_PURPLE_BUTTON_PRESS;
                            }
                            if (patternConfirmationTap) {
                                return Event.PATTERN_CONFIRMATION_BUTTON_PRESS;
                            }
                            if (patternCancellationTap) {
                                return Event.PATTERN_CANCELLATION_BUTTON_PRESS;
                            }
                            return Event.ALL_OTHER; // default
                        }))));

        // Test case to exercise the Finite State Machine FSM6.
        FSM6.defineTransition(State.START, Event.INTAKE_TOGGLE_BUTTON_PRESS,
                new ArrayList<>(Arrays.asList(FSM6.new Transition(State.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> {
                            System.out.println("Intake toggle " + intakeToggle + " revolverFull " + revolverFull);
                            //**TODO Here you're getting the current toggle state
                            // from the NWayToggle itself.
                            return intakeToggle == IntakeToggle.OFF && !revolverFull;
                        },
                        // Action
                        () -> {
                            System.out.println("Set intake toggle to ON");
                            intakeToggle = IntakeToggle.ON;
                            System.out.println("Turn on intake servos; start IntakeMotion thread");
                            //**TODO Need GENERATE_EVENT for State INTAKE_IN_PROGRESS
                            System.out.println("Generate internal event CHECK_INTAKE_DONE");
                            return Event.INTAKE_DONE;
                        }))));

        FSM6.defineTransition(State.START, Event.PATTERN_SELECTION_GREEN_BUTTON_PRESS,
                new ArrayList<>(Arrays.asList(FSM6.new Transition(State.START,
                // Guard condition
                () -> {
                    System.out.println("Artifacts in pattern " + artifactPattern.size());
                    return artifactPattern.size() < MAX_ARTIFACTS_IN_REVOLVER;
                },
                // Action
                () -> {
                    if (!customPatternTimerStarted) {
                        customPatternTimerStarted = true;
                        System.out.println("Start custom pattern timer");
                    }
                    return Event.GENERATE_NEXT_EVENT;
                }))));

        /*
        B.	Event  PATTERN_SELECTION_GREEN Button.update();
1.	Transition START
2.	Action pattern selection not full, add GREEN to pattern; if (!customPatternTimerStarted) start timer
C.	 Event  PATTERN_SELECTION_PURPLE Button.update();
1.	Transition START
2.	Action pattern selection not full, add PURPLE to pattern; if (!customPatternTimerStarted) start timer
D.	Event PATTERN_CONFIRMATION
1.	Transition START
2.	Action cancel timer
E.	 Event PATTERN_CANCELLATION
1.	Transition START
2.	Action cancel timer, clear pattern

         */

        // Catch-all for all other events applied to this state.
        FSM6.defineTransition(State.START, FSM6Container.Event.ALL_OTHER, FSM6Container.State.START);

        //**TODO TEMP
        FSM6.defineTransition(State.INTAKE_IN_PROGRESS, FSM6Container.Event.INTAKE_DONE, FSM6Container.State.START);


        // ***** STATE MACHINE DEFINITIONS ARE COMPLETE *****

        System.out.println("Starting the state machine");
        FSM6.processEvent(Event.INTAKE_TOGGLE_BUTTON_PRESS);
        State newCurrentState = FSM6.getCurrentState();
        if (newCurrentState == null)
            System.out.println("New current state not supplied by action routine");
        else
        System.out.println("New current state " + newCurrentState);
    }

}
