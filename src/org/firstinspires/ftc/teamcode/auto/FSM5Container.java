package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.auto.fsm.GenericFSM5;

import java.util.Optional;

public class FSM5Container {

    private enum State {START, GAMEPAD_2_Y_PRESSED, ELEVATOR_SAFE}

    private enum Event {
        GAMEPAD_2_Y, GAMEPAD_2_A, GAMEPAD_2_X, GAMEPAD_2_B,
        GAMEPAD_2_DPAD_UP, GAMEPAD_2_DPAD_DOWN, GAMEPAD_2_DPAD_LEFT,
        GAMEPAD_2_DPAD_RIGHT, ELEVATOR_LEVEL_NOT_REST, ELEVATOR_MOTION_COMPLETE,
        ALL_OTHER
    }

    // Duplicates Freight Frenzy ElevatorMotors.ElevatorLevel
    private enum ElevatorLevel {REST, SAFE, SHIPPING_HUB_LEVEL_1, SHIPPING_HUB_LEVEL_2, SHIPPING_HUB_LEVEL_3, CAP}

    private ElevatorLevel currentElevatorLevel = ElevatorLevel.REST;

    private final GenericFSM5<State, Event> FSM5 =
            new GenericFSM5<>(State.START, State.class, Event.class);

    public FSM5Container() {
    }

    public void testFSM5() {

        /*
        Test case to exercise the Finite State Machine FSM5 using buttons and actions
        from Freight Frenzy TeleOp.

        From the rest position start to raise the elevator to the safe
        position, delay 750ms, then tip the freight delivery arm up to
        the carry position.
        elevatorHeightSafe = new FTCButton(this, FTCButton.ButtonValue.GAMEPAD_2_Y);
        Current state must be ElevatorMotors.ElevatorLevel.REST
        async_move_elevator_to_safe_position(robot.elevatorMotors.safe, elevatorVelocity);
        asyncActionInProgress = AsyncAction.ELEVATOR_UP_TO_SAFE;
        asyncMoveElevatorUp = Threading.launchAsync(callableMoveElevatorUp);

        // Wait and then tilt the freight carrier up.
        sleep(750);
        Objects.requireNonNull(robot.freightCarrierServo).servo.setPosition(robot.freightCarrierServo.up);
        */

        // Use the overload that supplies an event E1 that the FSM will use to
        // make an internal transition.
        FSM5.defineTransition(State.START, Event.GAMEPAD_2_Y, State.GAMEPAD_2_Y_PRESSED,
                () -> {
                    if (currentElevatorLevel != ElevatorLevel.REST)
                        return Optional.of(Event.ELEVATOR_LEVEL_NOT_REST);

                    System.out.println("Elevator is at REST");
                    System.out.println("Start asynchronous elevator motion to SAFE");
                    System.out.println("Wait 750ms then tilt the freight carrier up");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.START, Event.ALL_OTHER, State.START,
                () -> {
                   System.out.println("Unsupported event, returning to START");
                   return Optional.empty();
                });

        FSM5.defineTransition(State.GAMEPAD_2_Y_PRESSED, Event.ELEVATOR_LEVEL_NOT_REST, State.START,
                () -> {
                    System.out.println("Elevator was not at REST; cannot go up to SAFE");
                    return Optional.empty();
                });

        // ELEVATOR_MOTION_COMPLETE is an external event.
        FSM5.defineTransition(State.GAMEPAD_2_Y_PRESSED, Event.ELEVATOR_MOTION_COMPLETE, State.ELEVATOR_SAFE,
                () -> {
                    System.out.println("Elevator is at the SAFE level");
                    return Optional.empty();
                });


        System.out.println("Starting the state machine");
        FSM5.processEvent(Event.GAMEPAD_2_Y);
        System.out.println("New current state " + FSM5.getCurrentState());

        FSM5.processEvent(Event.ELEVATOR_MOTION_COMPLETE);
        System.out.println("New current state " + FSM5.getCurrentState());

        System.out.println("DONE");
    }

}
