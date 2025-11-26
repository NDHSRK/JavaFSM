package org.firstinspires.ftc.teamcode.auto.fsm;

import org.firstinspires.ftc.teamcode.auto.FSM5Container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static java.lang.Thread.sleep;

public class FSM6Container {

    private enum State {
        START, INTAKE_IN_PROGRESS, INTAKE_COMPLETE
    }

    private enum Event {
        INTAKE_TOGGLE_BUTTON_PRESS, CHECK_INTAKE_DONE,
        PATTERN_SELECTION_GREEN_BUTTON_PRESS, PATTERN_SELECTION_PURPLE_BUTTON_PRESS,
        PATTERN_CONFIRMATION_BUTTON_PRESS, PATTERN_CANCELLATION_BUTTON_PRESS,
        ALL_OTHER
    }

    private final GenericFSM6<State, Event> FSM6 =
            new GenericFSM6<>(State.START, State.class, Event.class);

    public FSM6Container() {}

    public void testFSM6() throws InterruptedException {

        // Test case to exercise the Finite State Machine FSM6.
        FSM6.defineTransition(State.START, Event.INTAKE_TOGGLE_BUTTON_PRESS,
                new ArrayList<>(Arrays.asList(FSM6.new Transition(State.INTAKE_IN_PROGRESS,
                        // Guard condition
                        () -> {
                            System.out.println("If current toggle state is OFF && Revolver is not full");
                            return true;
                        },
                        // Action
                        () -> {
                            System.out.println("Set intake toggle to ON");
                            System.out.println("Turn on intake servos; start IntakeMotion thread");
                            System.out.println("Generate internal event CHECK_INTAKE_DONE");
                            return Event.CHECK_INTAKE_DONE;
                        }))));

        FSM6.defineTransition(State.INTAKE_IN_PROGRESS, FSM6Container.Event.CHECK_INTAKE_DONE, FSM6Container.State.START);

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
