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

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

// Generic types: state (S), event (E).
public class GenericFSM3<S extends Enum<S>, E extends Enum<E>> {

    private static final String TAG = "GenericFSM";

    // The FSM is a Map whose key is the current state (S); the associated
    // value is a second Map, Map<E, Transition<S, E>>. The key is an event
    // (T) and the value is an instance of the class Transition, which
    // contains two fields: 1. The next state (S) and 2. The next event (E).
    private final Map<S, Map<E, Transition<S, E>>> FSM = new HashMap<>();
    private S currentState;
    private final E nextExternalEvent;

    // The next external event value is the default value that the
    // the processEvent() overloads return to the caller. If the
    // processEvent overloads return any other value then the caller
    // can assume that it is an internal event and that the FSM
    // machinery should be moved without consuming any input.
    public GenericFSM3(S pInitialState, E pNextExternalEvent) {
        currentState = pInitialState;
        nextExternalEvent = pNextExternalEvent;
    }

    // Call any of the overloads (or mix and match) multiple times to set up
    // the state machine. See the processEvent() overloads below.
    // Overload that sets the state machine to expect the next external event.
    // No action routine is associated with the source state and current event.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState) {
        defineTransition(pSourceState, pEvent, pDestinationState, nextExternalEvent, Optional.empty());
    }

    public void defineTransition(S pSourceState, E pEvent, S pDestinationState,
                                 Optional<Supplier<E>> pActionRoutine) {
        defineTransition(pSourceState, pEvent, pDestinationState, nextExternalEvent, pActionRoutine);
    }

    // Overload that explicitly supplies the next event. The typical use of this
    // overload is for internal events, i.e. those that move the state machinery
    // internally without returning to the caller. No action routine is associated
    // with the source state and current event.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState, E pNextEvent) {
        defineTransition(pSourceState, pEvent, pDestinationState, pNextEvent, Optional.empty());
    }

    // Overload that explicitly supplies the next event. The typical use of this
    // overload is for internal events, i.e. those that move the state machinery
    // internally without returning to the caller. An action routine is associated
    // with the source state and current event.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState, E pNextEvent,
                                 Optional<Supplier<E>> pActionRoutine) {
        // If the source state is already in the collection as a key then add to the map
        // that must already exist as its value.
        if (FSM.containsKey(pSourceState)) {
            // Get the collection of events that can occur in this state.
            Map<E, Transition<S, E>> eventMap = FSM.get(pSourceState);
            if (eventMap == null)
                throw new IllegalStateException(
                        "FSM: no events associated with " + pSourceState.toString());

            // The event must not already be in the Map.
            if (eventMap.containsKey(pEvent))
                throw new IllegalStateException("FSM: event "
                        + pEvent.toString() + " already present for state " + currentState);
            eventMap.put(pEvent, new Transition<>(pDestinationState, pNextEvent, pActionRoutine));
        } else {
            // Insert a new source state along with its associated event and
            // transition into the collection.
            Map<E, Transition<S, E>> newEventMap = new HashMap<>();
            newEventMap.put(pEvent, new Transition<>(pDestinationState, pNextEvent, pActionRoutine));
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
    public E processEvent(E pEvent) {
        // Move the state machinery by changing the current state.
        Transition<S, E> transition = getTransition(pEvent);
        S newCurrentState = transition.getNextState();
        //** RobotLogCommon.d(TAG, "FSM current state " + currentState.toString() + ", event " +
        //**        pEvent + ", next state " + newCurrentState.toString());

        E nextEvent = transition.getNextEvent(); // and execute action routine if present
        currentState = newCurrentState;
        return nextEvent;
    }

    // Override the pre-stored next event with a next event returned from a
    // generic Function. Also supply a generic argument to the Function.
    public <T> E processEvent(E pEvent, T pActionArg, Function<T, E> pActionRoutine) {
        Transition<S, E> transition = getTransition(pEvent);
        S newCurrentState = transition.getNextState();
        //** RobotLogCommon.d(TAG, "FSM current state " + currentState.toString() + ", event " +
        //**        pEvent + ", next state " + newCurrentState.toString());

        // Note that the Function<T, E> is called instead of any action routine
        // stored in the FSM itself.
        E nextEvent = pActionRoutine.apply(pActionArg);
        currentState = newCurrentState;
        return nextEvent;
    }

    // Get the transition associated with an event.
    private Transition<S, E> getTransition(E pEvent) {
        // Use the current state as a key into the FSM.
        // The value of the first lookup is a map of events and state
        // transitions.
        if (!FSM.containsKey(currentState)) {
            throw new IllegalStateException("FSM: current state is not in the FSM: "
                    + currentState.toString());
        }

        // Use the current state as a key into the map of state transitions to
        // find the destination state and action routine.
        Map<E, Transition<S, E>> eventMap = FSM.get(currentState);
        if (!eventMap.containsKey(pEvent)) {
            throw new IllegalStateException("FSM: event "
                    + pEvent.toString() + " not present for state " + currentState);
        }

        // Move the state machinery by changing the current state.
        return eventMap.get(pEvent);
    }

    // A Transition consists of a next state (S) and a next event
    // (E).
    private static class Transition<S, E> {
        private final S nextState;
        private final E nextEvent;
        private final Optional<Supplier<E>> actionRoutine;

        public Transition(S pNextState, E pNextEvent, Optional<Supplier<E>> pActionRoutine) {
            nextState = pNextState;
            nextEvent = pNextEvent;
            actionRoutine = pActionRoutine;
        }

        public S getNextState() {
            return nextState;
        }

        public E getNextEvent() {
            if (actionRoutine.isEmpty())
                return nextEvent;
            else {
                return actionRoutine.get().get();
            }
        }
    }
}
