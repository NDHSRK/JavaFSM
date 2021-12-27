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
import java.util.function.Supplier;
import java.util.stream.Collectors;

// 12/26/2021 Based on a combination of FSM3 and FSM4, FSM5 provides
// for the storing of a Supplier in the FSM itself but eliminates
// support for a Function as an argument to processEvent. The reason
// is that the Function has access to the event in processEvent but
// would need additional logic based on the current state.

// Generic types: state (S), internal event enumeration (E).
public class GenericFSM5<S extends Enum<S>, E extends Enum<E>> {

    private static final String TAG = GenericFSM5.class.getSimpleName();
    public static final String ALL_OTHER = "ALL_OTHER";

    // The FSM is a Map whose key is the current state (S); the associated
    // value is a second Map, Map<E, Transition>. The key is an event and
    // the value is an instance of the class Transition, which contains
    // two fields: 1. The next state and 2. An Optional Supplier that
    // returns an Optional next event which, if present, will cause the
    // state machine to make a transition immediately based on the next
    // state and the next event. That is, the state machine makes an
    // internal transition without an external event.
    private final Map<S, Map<E, Transition>> FSM = new HashMap<>();
    private S currentState;
    private final Class<E> eventClass;

    public GenericFSM5(S pInitialState, Class<E> pEventClass) {
        currentState = pInitialState;
        eventClass = pEventClass;
    }

    // Define the transitions in the state machine by calling either of
    // the overloads (or mix and match) multiple times to set up.

    // The simplest state transition without an action routine.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState) {
        defineTransition(pSourceState, pEvent, pDestinationState, null);
    }

    // Overload that stores a Supplier in the state machine itself. This
    // Supplier is an action routine that returns an Optional next event
    // as described above.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState, Supplier<Optional<E>> pActionRoutine) {
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
            eventMap.put(pEvent, new Transition(pDestinationState, pActionRoutine));
        } else {
            // Insert a new source state along with its associated event and
            // transition into the collection.
            Map<E, Transition> newEventMap = new HashMap<>();
            newEventMap.put(pEvent, new Transition(pDestinationState, pActionRoutine));
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
        Optional<E> nextEvent = transition.executeActionRoutine();
        //** RobotLogCommon.d(TAG, "Executing internal transition based on event " + nextEvent.get());
        // call self
        nextEvent.ifPresent(this::processEvent);
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
        Map<E, Transition> transitionMap = FSM.get(currentState);

        // If the current state has a transition for the current
        // event, return the Transition now.
        if (transitionMap.containsKey(pEvent))
            return transitionMap.get(pEvent);

        // Otherwise check if the special catch-all event is present
        // and, if so, use its transition.
        List<String> eventKeys = transitionMap.keySet().stream().
                map(Enum::name).collect(Collectors.toList());

        if (!eventKeys.contains(ALL_OTHER))
            throw new IllegalStateException("FSM: event "
                    + pEvent.toString() + " not present for state " + currentState);

        E allOtherEvent = E.valueOf(eventClass, ALL_OTHER);
        return transitionMap.get(allOtherEvent);
    }

    // A Transition consists of a next state (S) and an Optional
    // action routine that returns an Optional next event. A
    // non-empty Optional next internal event indicates that the
    // state machine should make an internal transition based on
    // this event.
    private class Transition {
        private final S nextState;
        private final Supplier<Optional<E>> actionRoutine;

        public Transition(S pNextState, Supplier<Optional<E>> pActionRoutine) {
            nextState = pNextState;
            actionRoutine = pActionRoutine;
        }

        public S getNextState() {
            return nextState;
        }

        public Optional<E> executeActionRoutine() {
            return actionRoutine == null ? Optional.empty() :
                    actionRoutine.get();
        }
    }

}
