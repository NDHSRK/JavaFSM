package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.auto.fsm.GenericFSM5;

import java.util.Optional;

public class FSM5Container {

    private enum State {
        START, GAMEPAD_2_Y_PRESSED, GAMEPAD_2_DPAD_UP_PRESSED,
        ELEVATOR_SAFE, ELEVATOR_AT_REQUESTED_LEVEL, DELIVERY_ARM_EXTENDED,
        ELEVATOR_UP_AND_ARM_OUT, GAMEPAD_2_B_PRESSED, GAMEPAD_2_X_PRESSED,
        DELIVERY_ARM_RETRACTED, ELEVATOR_DOWN, ARM_RETRACTION_AND_ELEVATOR_DESCENT,
        ARM_RETRACTION_AND_ELEVATOR_DESCENT_STARTED, WAIT_FOR_ARM_RETRACTION_AND_ELEVATOR_DESCENT
    }

    private enum Event {
        GAMEPAD_2_Y, GAMEPAD_2_A, GAMEPAD_2_X, GAMEPAD_2_B,
        GAMEPAD_2_DPAD_UP, GAMEPAD_2_DPAD_DOWN, GAMEPAD_2_DPAD_LEFT,
        GAMEPAD_2_DPAD_RIGHT, ELEVATOR_LEVEL_NOT_REST, ELEVATOR_ASCENT_TO_SAFE_COMPLETE,
        ELEVATOR_ASCENT_COMPLETE, DELIVERY_ARM_EXTENSION_COMPLETE, START_ARM_RETRACTION_AND_ELEVATOR_DESCENT,
        ELEVATOR_DESCENT_COMPLETE, DELIVERY_ARM_RETRACTION_COMPLETE,
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

        From the rest position start to raise the elevator to the safe position, delay
        100ms, then tip the freight delivery arm up to the carry position.
        elevatorHeightSafe = new FTCButton(this, FTCButton.ButtonValue.GAMEPAD_2_Y);
        Current state must be ElevatorMotors.ElevatorLevel.REST
        asyncMoveElevatorUp = Threading.launchAsync(callableMoveElevatorUp);

        // Wait and then tilt the freight carrier up.
        sleep(100);
        Objects.requireNonNull(robot.freightCarrierServo).servo.setPosition(robot.freightCarrierServo.up);
        */
        FSM5.defineTransition(State.START, Event.GAMEPAD_2_Y, State.GAMEPAD_2_Y_PRESSED,
                () -> {
                    if (currentElevatorLevel != ElevatorLevel.REST)
                        return Optional.of(Event.ELEVATOR_LEVEL_NOT_REST);

                    System.out.println("Elevator is at REST");
                    System.out.println("Start asynchronous elevator motion to SAFE");
                    System.out.println("Wait then tilt the freight carrier up");
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

        // ELEVATOR_UP_TO_SAFE_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.GAMEPAD_2_Y_PRESSED, Event.ELEVATOR_ASCENT_TO_SAFE_COMPLETE, State.ELEVATOR_SAFE,
                () -> {
                    System.out.println("Elevator has completed its ascent to the SAFE level");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.GAMEPAD_2_Y_PRESSED, Event.ALL_OTHER, State.GAMEPAD_2_Y_PRESSED,
                () -> {
                    System.out.println("Unsupported event, remaining at GAMEPAD_2_Y_PRESSED");
                    return Optional.empty();
                });

        /*
        Simultaneously start to raise the elevator to level 3 and extend the freight delivery arm.
        elevatorHeightLevel3 = new FTCButton(this, FTCButton.ButtonValue.GAMEPAD_2_DPAD_UP);
        asyncMoveElevatorUp = Threading.launchAsync(callableMoveElevatorUp);
        asyncMoveArm = Threading.launchAsync(callableMoveArm);
        */
        FSM5.defineTransition(State.ELEVATOR_SAFE, Event.GAMEPAD_2_DPAD_UP, State.GAMEPAD_2_DPAD_UP_PRESSED,
                () -> {
                    System.out.println("Start asynchronous elevator ascent to LEVEL 3");
                    System.out.println("Start asynchronous extension of the freight delivery arm");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.START, Event.ALL_OTHER, State.ELEVATOR_SAFE,
                () -> {
                    System.out.println("Unsupported event, returning to ELEVATOR_SAFE");
                    return Optional.empty();
                });

        // Note that the elevator motion and the freight delivery arm motion may complete in either order.
        // ELEVATOR_UP_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.GAMEPAD_2_DPAD_UP_PRESSED, Event.ELEVATOR_ASCENT_COMPLETE, State.ELEVATOR_AT_REQUESTED_LEVEL,
                () -> {
                    System.out.println("Elevator has ascended to the requested level");
                    System.out.println("Wait for the freight delivery arm extension");
                    return Optional.empty();
                });

        // DELIVERY_ARM_EXTENSION_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.GAMEPAD_2_DPAD_UP_PRESSED, Event.DELIVERY_ARM_EXTENSION_COMPLETE, State.DELIVERY_ARM_EXTENDED,
                () -> {
                    System.out.println("Delivery arm extension complete");
                    System.out.println("Wait for the elevator to ascend to the requested level");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.GAMEPAD_2_DPAD_UP_PRESSED, Event.ALL_OTHER, State.GAMEPAD_2_DPAD_UP_PRESSED,
                () -> {
                    System.out.println("Unsupported event, returning to GAMEPAD_2_DPAD_UP_PRESSED");
                    return Optional.empty();
                });

        // DELIVERY_ARM_EXTENSION_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.ELEVATOR_AT_REQUESTED_LEVEL, Event.DELIVERY_ARM_EXTENSION_COMPLETE, State.ELEVATOR_UP_AND_ARM_OUT,
                () -> {
                    System.out.println("Delivery arm extension complete");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.ELEVATOR_AT_REQUESTED_LEVEL, Event.ALL_OTHER, State.ELEVATOR_AT_REQUESTED_LEVEL,
                () -> {
                    System.out.println("Unsupported event, remaining at ELEVATOR_AT_REQUESTED_LEVEL");
                    return Optional.empty();
                });

        // ELEVATOR_UP_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.DELIVERY_ARM_EXTENDED, Event.ELEVATOR_ASCENT_COMPLETE, State.ELEVATOR_UP_AND_ARM_OUT,
                () -> {
                    System.out.println("Elevator has reached the requested level");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.DELIVERY_ARM_EXTENDED, Event.ALL_OTHER, State.DELIVERY_ARM_EXTENDED,
                () -> {
                    System.out.println("Unsupported event, remaining at DELIVERY_ARM_EXTENDED");
                    return Optional.empty();
                });

        /*
        Tip the carrier down to deliver the block, wait, raise the carrier to the rest position
        freightCarrier = new FTCButton(this, FTCButton.ButtonValue.GAMEPAD_2_B);
        Objects.requireNonNull(robot.freightCarrierServo).servo.setPosition(robot.freightCarrierServo.down);
        sleep(1000);
        robot.freightCarrierServo.servo.setPosition(robot.freightCarrierServo.rest);
        */
        FSM5.defineTransition(State.ELEVATOR_UP_AND_ARM_OUT, Event.GAMEPAD_2_B, State.GAMEPAD_2_B_PRESSED,
                () -> {
                    System.out.println("Tilt the carrier to deliver freight");
                    System.out.println("Wait for the freight to be delivered");
                    System.out.println("Move the carrier to its rest position");
                    return Optional.empty();
                });

        /*
        Start the retraction, wait for the arm to clear the shipping hub, then start the descent
        of the elevator to the safe position.
        retractArmAndDescend = new FTCButton(this, FTCButton.ButtonValue.GAMEPAD_2_X);
        asyncMoveArm = Threading.launchAsync(callableMoveArm);
        sleep(200); // let the arm clear the shipping hub
        asyncMoveElevatorDown = Threading.launchAsync(callableMoveElevatorDown);
        */
        // Allow arm retraction and elevator descent even if the freight has not been delivered.
        FSM5.defineTransition(State.ELEVATOR_UP_AND_ARM_OUT, Event.GAMEPAD_2_X, State.ARM_RETRACTION_AND_ELEVATOR_DESCENT,
                () -> Optional.of(Event.START_ARM_RETRACTION_AND_ELEVATOR_DESCENT)); // internal transition to common state

        FSM5.defineTransition(State.ELEVATOR_UP_AND_ARM_OUT, Event.ALL_OTHER, State.ELEVATOR_UP_AND_ARM_OUT,
                () -> {
                    System.out.println("Unsupported event, remaining at ELEVATOR_UP_AND_ARM_OUT");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.GAMEPAD_2_B_PRESSED, Event.GAMEPAD_2_X, State.ARM_RETRACTION_AND_ELEVATOR_DESCENT,
                () -> Optional.of(Event.START_ARM_RETRACTION_AND_ELEVATOR_DESCENT)); // internal transition to common state

        FSM5.defineTransition(State.GAMEPAD_2_B_PRESSED, Event.ALL_OTHER, State.GAMEPAD_2_B_PRESSED,
                () -> {
                    System.out.println("Unsupported event, remaining at GAMEPAD_2_B_PRESSED");
                    return Optional.empty();
                });

        // Act on internal event START_ARM_RETRACTION_AND_ELEVATOR_DESCENT.
        FSM5.defineTransition(State.ARM_RETRACTION_AND_ELEVATOR_DESCENT, Event.START_ARM_RETRACTION_AND_ELEVATOR_DESCENT, State.WAIT_FOR_ARM_RETRACTION_AND_ELEVATOR_DESCENT,
                () -> {
                    System.out.println("Start asynchronous retraction of the freight delivery arm");
                    System.out.println("Wait before starting to elevator descent");
                    System.out.println("Start asynchronous elevator descent to SAFE");
                    return Optional.empty();
                });

        // Note that the freight delivery arm retraction and the elevator descent may complete in either order.
        // ELEVATOR_DESCENT_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.WAIT_FOR_ARM_RETRACTION_AND_ELEVATOR_DESCENT, Event.ELEVATOR_DESCENT_COMPLETE, State.ELEVATOR_DOWN,
                () -> {
                    System.out.println("Elevator is at the SAFE level");
                    System.out.println("Wait for the freight delivery arm retraction");
                    return Optional.empty();
                });

        // DELIVERY_ARM_EXTENSION_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.WAIT_FOR_ARM_RETRACTION_AND_ELEVATOR_DESCENT, Event.DELIVERY_ARM_RETRACTION_COMPLETE, State.DELIVERY_ARM_RETRACTED,
                () -> {
                    System.out.println("Delivery arm retraction complete");
                    System.out.println("Wait for the elevator to descend to the SAFE level");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.WAIT_FOR_ARM_RETRACTION_AND_ELEVATOR_DESCENT, Event.ALL_OTHER, State.WAIT_FOR_ARM_RETRACTION_AND_ELEVATOR_DESCENT,
                () -> {
                    System.out.println("Unsupported event, remaining at WAIT_FOR_ARM_RETRACTION_AND_ELEVATOR_DESCENT");
                    return Optional.empty();
                });

        // ELEVATOR_DESCENT_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.DELIVERY_ARM_RETRACTED, Event.ELEVATOR_DESCENT_COMPLETE, State.ELEVATOR_SAFE,
                () -> {
                    System.out.println("Elevator has reached the SAFE level");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.DELIVERY_ARM_RETRACTED, Event.ALL_OTHER, State.DELIVERY_ARM_RETRACTED,
                () -> {
                    System.out.println("Unsupported event, remaining at DELIVERY_ARM_RETRACTED");
                    return Optional.empty();
                });

        // DELIVERY_ARM_RETRACTION_COMPLETE is an external event triggered by thread completion.
        FSM5.defineTransition(State.ELEVATOR_DOWN, Event.DELIVERY_ARM_RETRACTION_COMPLETE, State.ELEVATOR_SAFE,
                () -> {
                    System.out.println("Delivery arm is retracted");
                    return Optional.empty();
                });

        FSM5.defineTransition(State.ELEVATOR_DOWN, Event.ALL_OTHER, State.ELEVATOR_DOWN,
                () -> {
                    System.out.println("Unsupported event, remaining at ELEVATOR_DOWN");
                    return Optional.empty();
                });

        // ***** STATE MACHINE DEFINITIONS ARE COMPLETE *****
        //**TODO STOPPED HERE 12/29/2021

        System.out.println("Starting the state machine");
        FSM5.processEvent(Event.GAMEPAD_2_Y);
        System.out.println("New current state " + FSM5.getCurrentState());

        FSM5.processEvent(Event.ELEVATOR_ASCENT_TO_SAFE_COMPLETE);
        System.out.println("New current state " + FSM5.getCurrentState());

        // Move the elevator up to level 3 and the freight delivery arm out.
        FSM5.processEvent(Event.GAMEPAD_2_DPAD_UP);
        System.out.println("New current state " + FSM5.getCurrentState());

        System.out.println("DONE");
    }

}
