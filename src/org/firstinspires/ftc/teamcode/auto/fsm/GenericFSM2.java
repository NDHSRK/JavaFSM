// *************************************************************************
//
//  Filename:     GenericFSM2.java
//
//  Purpose:      See below
//
//  Modification History:
//  Date          Name        Change
//
//  22-Feb-2021   PYoung      Upgrade Java version to support lambda action
//                            routines.
//
// ***************************************************************************

// The starting point was an FSM implementation described in an issue of the magazine
// C/C++ Users Journal: Object-Oriented Finite State Machines by Frantisek Kaduch,
// Damian Jan, and Purificacion Vidal. The source code was obtained from the CUJ
// freeware download library.

// The first change to the C++ original was to parameterize the class over the types
// S (state), E (event), and A (arguments to an action routine).

// The current Java version uses instances of the functional Supplier<T> and
// stores the lambda implementations in the FSM itself.

package org.firstinspires.ftc.teamcode.auto.fsm;

// import org.firstinspires.ftc.ftcdevcommon.RobotLogCommon;

import java.util.*;
import java.util.function.Function;

public class GenericFSM2<S extends Enum<S>, E extends Enum<E>, T extends GenericFSM2.ActionRoutineArgument> {

    private static final String TAG = "GenericFSM";

    //** 12/18/2021
    // "next event" is a misnomer. What you really mean is "source of
    // the next event", internal or external. and, if internal, the
    // actual event so that the state machinery can be moved right
    // away.Also, you need to find a way to store action routines
    // as Function<T,R>.

    //**TODO correct comments ...
    // The FSM is a Map whose key is the current state (S); the associated
    // value is a second Map, Map<R, Transition<S, R, T>>. The key of the
    // second (inner) map is an event (R), the value is an instance of the
    // class Transition, which contains two fields: 1. The next state (S)
    // and 2. A Function<T, R> that takes a parameter T and returns the next
    // event R.
    private final Map<S, Map<E, Transition<S, E, T>>> FSM = new HashMap<>();
    private S currentState;
    private final E nextExternalEvent;

    // The next external event value signifies to the processEvent() overloads
    // below that they should stop making state transitions and return to the
    // caller.
    public GenericFSM2(S pInitialState, E pNextExternalEvent) {
        currentState = pInitialState;
        nextExternalEvent = pNextExternalEvent;
    }

    // Call either of the overloads (or mix and match) multiple times to set up
    // the state machine.
    // Overload that sets the state machine to expect the next external event.
    // See processEvent() below.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState) {
        defineTransition(pSourceState, pEvent, pDestinationState,
                (arg -> nextExternalEvent));
    }

    // Typically called multiple times to set up the state machine.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState, Function<T, E> pActionRoutine) {
        // If the source state is already in the collection as a key then add to the map
        // that must already exist as its value.
        if (FSM.containsKey(pSourceState)) {
            // Get the collection of events that can occur in this state.
            Map<E, Transition<S, E, T>> eventMap = FSM.get(pSourceState);
            if (eventMap == null)
                throw new IllegalStateException(
                        "FSM: no events associated with " + pSourceState.toString());

            // The event must not already be in the Map.
            if (eventMap.containsKey(pEvent))
                throw new IllegalStateException("FSM: event "
                        + pEvent.toString() + " already present for state " + currentState);
            eventMap.put(pEvent, new Transition<>(pDestinationState, pActionRoutine));
        } else {
            // Insert a new source state along with its associated event and
            // transition into the collection.
            Map<E, Transition<S, E, T>> newEventMap = new HashMap<>();
            newEventMap.put(pEvent, new Transition<>(pDestinationState, pActionRoutine));
            FSM.put(pSourceState, newEventMap);
        }
    }

    // For debugging.
    public S getCurrentState() {
        return currentState;
    }

    // Allow the current state to be reset by force. All defined transitions remain
    // intact. This method can be used for a "hard reset" of the state machine is.
    public void setState(S pNewState) {
        currentState = pNewState;
    }

    // Move the state machinery in response to an event.
    //**TODO STUCK here
    // Look at this code from GenericFSM, which actually has the same problem ...
    /*

        // If the next event in the stored in the state machine is an internal
        // event, process it now by calling myself.
        if (nextEvent != nextExternalEvent)
            processEvent(nextEvent);
     */
    // ONLY the first call receives the event argument, subsequent internal
    // calls do not.
    //**TODO LOOK at GenericFSM, line 129 - should this call itself with the
    // original arguments??
    public E processEvent(E pEvent, T pEventArg) {
        // Use the current state as a key into the FSM.
        // The value of the first lookup is a map of events and state
        // transitions.
        if (!FSM.containsKey(currentState)) {
            throw new IllegalStateException("FSM: current state is not in the FSM: "
                    + currentState.toString());
        }

        // Use the current state as a key into the map of state transitions to
        // find the destination state and action routine.
        Map<E, Transition<S, E, T>> eventMap = FSM.get(currentState);
        if (!eventMap.containsKey(pEvent)) {
            throw new IllegalStateException("FSM: event "
                    + pEvent.toString() + " not present for state " + currentState);
        }

        // Execute the action routine.
        Transition<S, E, T> transition = eventMap.get(pEvent);
        E retval = transition.getNextEvent(pEventArg);

        // Move the state machinery by changing the current state.
        S newCurrentState = transition.getNextState();
        //**RobotLogCommon.d(TAG, "FSM current state " + currentState.toString() + ", event " +
        //**		pEvent + ", next state " + newCurrentState.toString());

        currentState = newCurrentState;
        return retval;
    }

    // A Transition consists of a state and a Function<T, R> which
    // returns the next event.
    static class Transition<S, E, U extends ActionRoutineArgument> {
        private final S nextState;
        private final Function<U, E> actionRoutine;

        public Transition(S pNextState, Function<U, E> pActionRoutine) {
            nextState = pNextState;
            actionRoutine = pActionRoutine;
        }

        // Executes the lambda previously stored in the statemachine.
        public E getNextEvent(U t) {
            return actionRoutine.apply(t);
        }

        public S getNextState() {
            return nextState;
        }
    }

    public static abstract class ActionRoutineArgument {

    }
}