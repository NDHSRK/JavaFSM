// *************************************************************************
//
//  Filename:     GenericFSM.java
//
//  Purpose:      See below
//
//  Modification History:
//  Date          Name        Change
//
//  07-Mar-2021   PYoung      Upgrade Java version to support lambda action
//                            routines.
//
// ***************************************************************************

// The starting point was an FSM implementation described in an issue of the magazine
// C/C++ Users Journal: Object-Oriented Finite State Machines by Frantisek Kaduch,
// Damian Jan, and Purificacion Vidal. The source code was obtained from the CUJ
// freeware download library.

// The current Java version uses generic types for states and events and
// provides for an "action routine" in the form of a Function<T, E> that
// accept an argument T and returns the next event E. See the two overloads
// of processEvent below.

package org.firstinspires.ftc.teamcode.auto.fsm;

//** import org.firstinspires.ftc.ftcdevcommon.intellij.RobotLogCommon;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

// 12/22/2021 Based on FSM3 but, in the interest of simplicity,
// removes the option to store an action routine in the FSM itself.

// Generic types: state (S), internal event enumeration (E).
public class GenericFSM4<S extends Enum<S>, E extends Enum<E>> {

    private static final String TAG = GenericFSM4.class.getSimpleName();

    // The FSM is a Map whose key is the current state (S); the associated
    // value is a second Map, Map<E, Transition>. The key is an event
    // and the value is an instance of the class Transition, which
    // contains two fields: 1. The next state and 2. An Optional
    // internal next event.
    private final Map<S, Map<E, Transition>> FSM = new HashMap<>();
    private S currentState;

    public GenericFSM4(S pInitialState) {
        currentState = pInitialState;
    }

    // Define the transitions in the state machine by calling either of
    // the overloads (or mix and match) multiple times to set up.

    // Overload that sets the state machine to expect the event to come
    // from an external source.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState) {
        defineTransition(pSourceState, pEvent, pDestinationState, Optional.empty());
    }

    // Overload that explicitly supplies an internal next event. An internal
    // moves the state machinery without returning to the caller.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState, E pNextEvent) {
        defineTransition(pSourceState, pEvent, pDestinationState, Optional.of(pNextEvent));
    }

    private void defineTransition(S pSourceState, E pEvent, S pDestinationState, Optional<E> pNextEvent) {
        // If the source state is already in the collection as a key then add to the map
        // that must already exist as its value.
        if (FSM.containsKey(pSourceState)) {
            // Get the collection of events that can occur in this state.
            Map<E, Transition> eventMap = FSM.get(pSourceState);
            if (eventMap == null)
                throw new IllegalStateException(
                        "FSM: no events associated with " + pSourceState.toString());

            // The event must not already be in the Map.
            if (eventMap.containsKey(pEvent))
                throw new IllegalStateException("FSM: event "
                        + pEvent.toString() + " already present for state " + currentState);
            eventMap.put(pEvent, new Transition(pDestinationState, pNextEvent));
        } else {
            // Insert a new source state along with its associated event and
            // transition into the collection.
            Map<E, Transition> newEventMap = new HashMap<>();
            newEventMap.put(pEvent, new Transition(pDestinationState, pNextEvent));
            FSM.put(pSourceState, newEventMap);
        }
    }

    // For debugging.
    public S getCurrentState() {
        return currentState;
    }

    // Set the current state by force. This method can be used in
    // case a "hard reset" of the state machine is called for.
    public void setState(S pNewState) {
        currentState = pNewState;
    }

    // Move the state machinery in response to an event.
    public void processEvent(E pEvent) {
        // Move the state machinery by changing the current state.
        Transition transition = getTransition(pEvent);
        S newCurrentState = transition.getNextState();
        //** RobotLogCommon.d(TAG, "FSM current state " + currentState.toString() + ", event " +
        //**        pEvent + ", next state " + newCurrentState.toString());

        currentState = newCurrentState;
        Optional<E> nextEvent = transition.getNextEvent();
        //** RobotLogCommon.d(TAG, "Executing internal transition based on event " + nextEvent.get());
        // call self
        nextEvent.ifPresent(this::processEvent);
    }

    // Override the pre-stored internal next event, which may be an empty
    // Optional or a valid event, with a next event returned from a generic
    // Function. Also supply a generic argument to the Function.
    public <T> void processEvent(E pEvent, T pActionArg, Function<T, Optional<E>> pActionRoutine) {
        Transition transition = getTransition(pEvent);
        S newCurrentState = transition.getNextState();
        //** RobotLogCommon.d(TAG, "FSM current state " + currentState.toString() + ", event " +
        //**        pEvent + ", next state " + newCurrentState.toString());

        currentState = newCurrentState;
        Optional<E> nextEvent = pActionRoutine.apply(pActionArg);
        if (nextEvent.isPresent()) {
            //** RobotLogCommon.d(TAG, "Executing internal transition based on event " + internalNextEvent.get());
            processEvent(nextEvent.get());
        }
    }

    // Get the transition associated with an event.
    private Transition getTransition(E pEvent) {
        // Use the current state as a key into the FSM.
        // The value of the first lookup is a map of events and state
        // transitions.
        if (!FSM.containsKey(currentState)) {
            throw new IllegalStateException("FSM: current state is not in the FSM: "
                    + currentState.toString());
        }

        // Use the current state as a key into the map of state transitions to
        // find the destination state and action routine.
        Map<E, Transition> eventMap = FSM.get(currentState);
        if (!eventMap.containsKey(pEvent)) {
            throw new IllegalStateException("FSM: event "
                    + pEvent.toString() + " not present for state " + currentState);
        }

        // Move the state machinery by changing the current state.
        return eventMap.get(pEvent);
    }

    // A Transition consists of a next state (S) and an optional
    // next event. A non-empty Optional indicates that the state
    // machine should make an internal transition based on this
    // event.
    private class Transition {
        private final S nextState;
        private final Optional<E> nextEvent;

        public Transition(S pNextState, Optional<E> pNextEvent) {
            nextState = pNextState;
            nextEvent = pNextEvent;
        }

        public S getNextState() {
            return nextState;
        }

        public Optional<E> getNextEvent() {
                return nextEvent;
        }
    }

}
