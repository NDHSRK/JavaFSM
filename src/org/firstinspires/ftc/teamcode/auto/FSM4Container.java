package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.auto.fsm.GenericFSM4;

import java.util.Optional;

public class FSM4Container {

    enum State {STATE_0, STATE_1, STATE_2, STATE_3, STATE_4, STATE_5, STATE_6, STATE_7}

    enum Event {E0, E1, E2, E3, E4, E5, E6}

    private final GenericFSM4<State, Event> FSM4 = new GenericFSM4<>(State.STATE_0);

    public FSM4Container() {}

    public void testFSM4() {

        // Use the overload that supplies an event E1 that the FSM will use to
        // make an internal transition.
        FSM4.defineTransition(State.STATE_0, Event.E0, State.STATE_1, Event.E1);

        // Define a simple transition from S1 to S2.
        FSM4.defineTransition(State.STATE_1, Event.E1, State.STATE_2);

        FSM4.defineTransition(State.STATE_2, Event.E2, State.STATE_3);

        FSM4.defineTransition(State.STATE_3, Event.E3, State.STATE_4);
        FSM4.defineTransition(State.STATE_4, Event.E4, State.STATE_5);

        FSM4.defineTransition(State.STATE_5, Event.E5, State.STATE_6);
        FSM4.defineTransition(State.STATE_6, Event.E6, State.STATE_7);

        System.out.println("Starting state state " + FSM4.getCurrentState());
        FSM4.processEvent(Event.E0);
        System.out.println("New current state after Event.E0 and internal Event.E1 " + FSM4.getCurrentState());

        FSM4.processEvent(Event.E2);
        System.out.println("New current state after Event.E2 " + FSM4.getCurrentState());

        FSM4.processEvent(Event.E3);
        System.out.println("New current state after Event.E3 " + FSM4.getCurrentState());

        FSM4.processEvent(Event.E4, 100,
                arg -> {
                    if (arg == 100)
                        return Optional.of(Event.E5);
                    else
                        return Optional.empty();
                });
        System.out.println("New current state after Event.E4 " + FSM4.getCurrentState());
    }

}
