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

// The current Java version uses generic enum types for states and events
// and provides for an "action routine" in the form of a Supplier<E> that
// returns an Optional next event E.

package org.firstinspires.ftc.teamcode.auto.fsm;

import java.util.*;
import java.util.function.Supplier;

// Generic types: state (S), internal event enumeration (E).
public class GenericFSM6<S extends Enum<S>, E extends Enum<E>> {

    private static final String TAG = GenericFSM6.class.getSimpleName();
    public static final String ALL_OTHER = "ALL_OTHER";

    // The FSM is an EnumMap whose key is the current state (S); the
    // associated value is a second EnumMap<E, List<Transition>>.
    // The key is an event and the value is a List of instances of
    // the class Transition, which contains three fields: 1. The next
    // state, 2. A Supplier<Boolean> pGuard which, if non-null, acts
    // as a “guard condition”: if pGuard is null or the non-null
    // guard condition returns true, on the next cycle the state
    // machine should make a transition to the state identified in
    // the Transition class, and 3. A Supplier<E> pAction, which,
    // if non-null, can modify any accessible field or call any
    // accessible method, and returns the next event that the state
    // machine should feed to the next state. If pAction returns null,
    // the state machine will get the next event from its normal
    // sources such as button clicks; if the next event is a named
    // value, the state machine will make a transition without an
    // external event.

    private final EnumMap<S, EnumMap<E, List<Transition>>> FSM;
    private S currentState;
    private final Class<S> stateClass;
    private final Class<E> eventClass;
    private final E allOtherEvent; // special catch-all event; see usage below

    public GenericFSM6(S pInitialState, Class<S> pStateClass, Class<E> pEventClass) {
        currentState = pInitialState;
        stateClass = pStateClass;
        FSM = new EnumMap<>(stateClass);
        eventClass = pEventClass;

        //**TODO Purpose? If the user has not included the catch-all ALL_OTHER as
        // an Event enum value, set it null here.
        E tempAllOtherEvent;
        try {
            tempAllOtherEvent = E.valueOf(eventClass, ALL_OTHER);
        }
        catch (IllegalArgumentException iex) {
            tempAllOtherEvent = null;
        }
        allOtherEvent = tempAllOtherEvent;
    }

    // Define the transitions in the state machine by calling either of
    // the overloads (or mix and match) multiple times to set up.

    // The simplest state transition without any guard conditions/
    // action routines.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState) {
        defineTransition(pSourceState, pEvent, new ArrayList<>(Arrays.asList(new Transition(pDestinationState, null, null))));
    }

    // State transition that includes non-null guard conditions/
    // action routines.
    public void defineTransition(S pSourceState, E pEvent, List<Transition> pTransitions) {
        // If the source state is already in the collection as a key then add to the map
        // that must already exist as its value.
        if (FSM.containsKey(pSourceState)) {
            // Get the collection of events that can occur in this state.
            EnumMap<E, List<Transition>> eventMap = FSM.get(pSourceState);
            if (eventMap == null)
                throw new IllegalStateException(
                        "FSM: no events associated with " + pSourceState.toString());

            // The event must not already be in the map.
            if (eventMap.containsKey(pEvent))
                throw new IllegalStateException("FSM: event "
                        + pEvent.toString() + " already present for state " + currentState);
            eventMap.put(pEvent, pTransitions);
        } else {
            // Insert a new source state along with its associated event and
            // transition into the collection.
            EnumMap<E, List<Transition>> newEventMap = new EnumMap<>(eventClass);
            newEventMap.put(pEvent,pTransitions);
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

    // Move the state machinery in response to an event. Look at
    // the list of possible transitions, each of which may have
    // a guard condition and/or and action routine.
    public void processEvent(E pEvent) {
        List<Transition> transitions = getTransitions(pEvent);
        S nextState;
        Supplier<Boolean> guard;
        E nextEvent;
        for (Transition oneTransition : transitions) {
            guard = oneTransition.guardCondition;
            if (guard != null && !guard.get()) {
                // failed the guard condition
                continue;
            }

            // Got a valid transition.
            nextState = oneTransition.getNextState();
            System.out.println(TAG + " " + "FSM current state " + currentState.toString() + ", event " +
                    pEvent + ", next state " + nextState.toString());

            currentState = nextState;
            Supplier<E> actionRoutine = oneTransition.actionRoutine;
            if (actionRoutine == null) {
                break;
            }

            //** RobotLogCommon.d(TAG, "Executing internal transition based on event " + nextEvent.get());
            // call self
            nextEvent = oneTransition.executeActionRoutine();
            if (nextEvent != null) {
                processEvent(nextEvent);
            }
        }
    }

    // Get the transition associated with an event.
    private List<Transition> getTransitions(E pEvent) {
        // Use the current state as a key into the FSM.
        // The value of the first lookup is a map of events and state
        // transitions.
        if (!FSM.containsKey(currentState)) {
            throw new IllegalStateException("FSM: current state is not in the FSM: "
                    + currentState.toString());
        }

        // Use the current state as a key into the map of state transitions to
        // find the destination state and action routine.
        EnumMap<E, List<Transition>> transitionMap = FSM.get(currentState);

        // If the current state has a transition for the current
        // event, return the Transition now.
        if (transitionMap.containsKey(pEvent))
            return transitionMap.get(pEvent);

        // Otherwise check if the special catch-all event is present
        // and, if so, use its transition.
        Set<E> eventKeySet = transitionMap.keySet();
        if (allOtherEvent == null || !eventKeySet.contains(allOtherEvent))
            throw new IllegalStateException("FSM: event "
                    + pEvent.toString() + " not present for state " + currentState);

        return transitionMap.get(allOtherEvent);
    }

    // A Transition consists of a next state (S) and an Optional
    // action routine that returns an Optional next event. A
    // non-empty Optional next internal event indicates that the
    // state machine should make an internal transition based on
    // this event.
    public class Transition {
        private final S nextState;
        private final Supplier<Boolean> guardCondition;
        private final Supplier<E> actionRoutine;

        public Transition(S pNextState, Supplier<Boolean> pGuard, Supplier<E> pActionRoutine) {
            nextState = pNextState;
            guardCondition = pGuard;
            actionRoutine = pActionRoutine;
        }

        public S getNextState() {
            return nextState;
        }

        public E executeActionRoutine() {
            return actionRoutine == null ? null :
                    actionRoutine.get();
        }
    }

}
