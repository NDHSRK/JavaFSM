package org.firstinspires.ftc.teamcode.auto.fsm;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class FSM6Container {

    private enum State {
        START, INTAKE_IN_PROGRESS, INTAKE_COMPLETE
    }

    private enum Event {
        GET_NEXT_EVENT,
        INTAKE_STARTED, INTAKE_DONE,
        ALL_OTHER //**TODO -> DEFAULT
    }

    private final GenericFSM6<State, Event> FSM6 =
            new GenericFSM6<>(State.START, State.class, Event.class);

    enum InProgress {START, INTAKE, SHOOTING, AIMING}

    // Simulate button values and other conditions.
    private enum IntakeToggle {OFF, ON}

    private IntakeToggle intakeToggle = IntakeToggle.OFF;
    private boolean intakeToggleOn = true;
    private boolean revolverFull = false;

    // Pattern selection
    private boolean patternSelectionGreenOn = false;
    private boolean patternSelectionPurpleOn = false;
    private boolean patternConfirmationOn = false;
    private boolean patternCancellationOn = false;

    enum ArtifactColor {GREEN, PURPLE}

    private final List<ArtifactColor> artifactPattern = new ArrayList<>();
    public static final int MAX_ARTIFACTS_IN_REVOLVER = 3;
    private boolean customPatternTimerStarted = false;

    //**TODO You're going to need a Monitor state machine that
    // checks button presses and conditions and generates events
    // for the DecodeTeleOpFSM.

    public FSM6Container() {
    }

    public void testFSM6() {
        FSM6.defineTransition(State.START, Event.GET_NEXT_EVENT,
                FSM6.new Transition(State.START,
                        // Guard condition
                        null,
                        // Action
                        //**TODO Do you want to look at the buttons
                        // and conditions here or get the Event from
                        // the Monitor?
                        () -> {
                            if (updateIntakeOn()) {
                                return Event.INTAKE_STARTED;
                            }
                            if (updatePatternSelectionGreen()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            if (updatePatternSelectionPurple()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            if (updatePatternConfirmation()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            if (updatePatternCancellation()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            return Event.GET_NEXT_EVENT;
                        }));

        FSM6.defineTransition(State.START, Event.INTAKE_STARTED,
                FSM6.new Transition(State.INTAKE_IN_PROGRESS,
                        // Guard condition
                        null,
                        // Action
                        () -> {
                            return Event.GET_NEXT_EVENT;
                        }
                ));

        // Catch-all for all other events applied to this state.
        FSM6.defineTransition(State.START, FSM6Container.Event.ALL_OTHER, FSM6Container.State.START);

        FSM6.defineTransition(State.INTAKE_IN_PROGRESS, Event.GET_NEXT_EVENT,
                FSM6.new Transition(State.INTAKE_IN_PROGRESS,
                        // Guard condition
                        null,
                        // Action
                        () -> {
                            if (updateIntakeOff()) {
                                return Event.INTAKE_DONE;
                            }
                            if (updatePatternSelectionGreen()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            if (updatePatternSelectionPurple()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            if (updatePatternConfirmation()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            if (updatePatternCancellation()) {
                                return Event.GET_NEXT_EVENT;
                            }
                            return Event.GET_NEXT_EVENT;
                        }));

        //**TODO TEMP
        FSM6.defineTransition(State.INTAKE_IN_PROGRESS, FSM6Container.Event.INTAKE_DONE, FSM6Container.State.START);


        // ***** STATE MACHINE DEFINITIONS ARE COMPLETE *****

        System.out.println("Starting the state machine");
        FSM6.processEvent(Event.GET_NEXT_EVENT);
        State newCurrentState = FSM6.getCurrentState();
        if (newCurrentState == null)
            System.out.println("New current state not supplied by action routine");
        else
            System.out.println("New current state " + newCurrentState);
    }

    private boolean updateIntakeOn() {
        return intakeToggleOn;
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
