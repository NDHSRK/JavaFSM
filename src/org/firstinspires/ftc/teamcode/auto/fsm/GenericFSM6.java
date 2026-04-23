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
// and provides for an "action routine" in the form of a Callable<E> that
// returns an Optional next event E.

package org.firstinspires.ftc.teamcode.auto.fsm;

import org.firstinspires.ftc.ftcdevcommon.Pair;

import java.util.*;
import java.util.concurrent.Callable;

// Generic types: state (S), internal event enumeration (E).
public class GenericFSM6<S extends Enum<S>, E extends Enum<E>> {

    // The FSM is an EnumMap whose key is the current state (S); the
    // associated value is a second EnumMap<E, List<Transition>>.
    // The key is an event and the value is a List of instances of
    // the class Transition, which contains three fields: 1. The next
    // state, 2. A Callable<Boolean> pGuard which, if non-null, acts
    // as a “guard condition”: if pGuard is null or the non-null
    // guard condition returns true, on the next cycle the state
    // machine should make a transition to the state identified in
    // the Transition class, and 3. A Callable<E> pAction, which,
    // if non-null, can modify any accessible field or call any
    // accessible method, and returns the next event that the state
    // machine should feed to the next state. If pAction returns null,
    // the state machine will get the next event from its normal
    // sources such as button clicks; if the next event is a named
    // value, the state machine will make a transition without an
    // external event.

    // Why Callable<V> instead Supplier<T>? Because a Callable can
    // throw a checked exception, which allows the callers of the FSM
    // to manage their own error handling.
    // From the Java documentation:
    // V call() throws Exception;

    // A Supplier<T> cannot throw a checked exception; the signature
    // of its single method is:
    // T get();

    private final EnumMap<S, EnumMap<E, List<Transition<S, E>>>> FSM;
    private S currentState;
    private final Class<E> eventClass;

    public GenericFSM6(S pInitialState, Class<S> pStateClass, Class<E> pEventClass) {
        currentState = pInitialState;
        FSM = new EnumMap<>(pStateClass);
        eventClass = pEventClass;
    }

    // Define the transitions in the state machine by calling either of
    // the overloads (or mix and match) multiple times to set up.

    // The simplest state transition without any guard conditions/action routines.
    public void defineTransition(S pSourceState, E pEvent, S pDestinationState) {
        Objects.requireNonNull(pDestinationState, "FSM: destination state must not be null");
        defineTransition(pSourceState, pEvent, new ArrayList<>(Collections.singletonList(new Transition<>(pDestinationState, null, null))));
    }

    // State transition with a single guard condition/action routine.
    public void defineTransition(S pSourceState, E pEvent, Transition<S, E> pTransition) {
        Objects.requireNonNull(pTransition, "FSM: transition must not be null");
        defineTransition(pSourceState, pEvent, new ArrayList<>(Collections.singletonList(pTransition)));
    }

    // State transition with a collection of guard conditions/action routines.
    public void defineTransition(S pSourceState, E pEvent, List<Transition<S, E>> pTransitions) {
        // All arguments must be non-null and the collection of transitions must not be empty.
        Objects.requireNonNull(pSourceState, "FSM: source state must not be null");
        Objects.requireNonNull(pEvent, "FSM: event must not be null");
        Objects.requireNonNull(pTransitions, "FSM: collection of transitions must not be null");
        if (pTransitions.isEmpty())
            throw new IllegalStateException("FSM: collection of transitions must not be empty");

        // If the source state is already in the collection as a key then add to the map
        // that must already exist as its value.
        if (FSM.containsKey(pSourceState)) {
            // Get the collection of events that can occur in this state.
            EnumMap<E, List<Transition<S, E>>> eventMap = FSM.get(pSourceState);
            if (eventMap == null)
                throw new IllegalStateException(
                        "FSM: no events associated with " + pSourceState.toString());

            // The event must not already be in the map.
            if (eventMap.containsKey(pEvent))
                throw new IllegalStateException("FSM: event "
                        + pEvent.toString() + " already present for state " + pSourceState);
            eventMap.put(pEvent, pTransitions);
        } else {
            // Insert a new source state along with its associated event and
            // transition into the collection.
            EnumMap<E, List<Transition<S, E>>> newEventMap = new EnumMap<>(eventClass);
            newEventMap.put(pEvent, pTransitions);
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
    // a nullable guard condition and a nullable action routine.
    public E processEvent(E pEvent) throws Exception {
        List<Transition<S, E>> transitions = getTransitions(pEvent);
        Callable<Boolean> guard;
        E nextEvent = pEvent;
        for (Transition<S, E> oneTransition : transitions) {
            guard = oneTransition.guardCondition;
            if (guard != null && !guard.call()) {
                // failed the guard condition
                continue;
            }

            // Got a valid transition: the guard condition was null,
            // which indicates an automatic success, or the non-null
            // guard condition succeeded.
            S nextState = oneTransition.getNextState();
            if (nextState == null)
                throw new IllegalStateException("FSM: next state not defined for current state " + currentState +
                        " and transition " + oneTransition);

            currentState = nextState;

            // Get the action routine for the current transition.
            // if it is null, the next event will be the same as
            // the current event.
            Callable<E> actionRoutine = oneTransition.actionRoutine;
            if (actionRoutine == null) {
                break;
            }

            // A non-null action routine returns an event that may
            // or may not be different from the current event.
            nextEvent = oneTransition.executeActionRoutine();
            break;
        }

        return nextEvent;
    }

    // Get the transition(s) associated with an event.
    private List<Transition<S, E>> getTransitions(E pEvent) {
        // Use the current state as a key into the FSM.
        // The value of the first lookup is a map of events and state
        // transitions.
        if (!FSM.containsKey(currentState))
            throw new IllegalStateException("FSM: current state " + currentState.toString() + "is not in the FSM");

        // Use the current state as a key into the map of state transitions to
        // find the destination state and action routine.
        EnumMap<E, List<Transition<S, E>>> transitionMap = FSM.get(currentState);

        // If the current state has a transition for the current
        // event, return the Transition now.
        if (transitionMap != null && transitionMap.containsKey(pEvent))
            return transitionMap.get(pEvent);

        // Error out if the current state does not have a transition
        // for the current event.
        throw new IllegalStateException("FSM: current state " + currentState +
                 " has no transitions for event "+ pEvent.toString());
    }

    // A Transition consists of a next state (S), a nullable guard
    // condition, and a nullable action routine.
    public static class Transition<S, E> {
        private final S nextState;
        private final Callable<Boolean> guardCondition;
        private final Callable<E> actionRoutine;

        public Transition(S pNextState, Callable<Boolean> pGuard, Callable<E> pActionRoutine) {
            nextState = pNextState;
            guardCondition = pGuard;
            actionRoutine = pActionRoutine;
        }

        public S getNextState() {
            return nextState;
        }

        public E executeActionRoutine() throws Exception {
            return actionRoutine == null ? null :
                    actionRoutine.call();
        }
    }

}
