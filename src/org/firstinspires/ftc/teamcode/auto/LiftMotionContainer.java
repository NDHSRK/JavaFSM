package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.ftcdevcommon.AutoWorker;
import org.firstinspires.ftc.ftcdevcommon.Threading;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// Class that starts up a CompletableFuture whose Callable simulates
// the raising of the Decode lift.
public class LiftMotionContainer {

    private static final String TAG = LiftMotionContainer.class.getSimpleName();

    // Thread-related.
    private LiftMotionCallable liftMotionCallable;
    private CompletableFuture<Void> liftFuture;

    private final AtomicBoolean stopLift = new AtomicBoolean();

    public LiftMotionContainer() {
    }

    // Start up the lift as a CompletableFuture.
    // Return the Future so that the caller can manage
    // its completion. Make sure that this can't be called
    // while a liftFuture is already running.
    public CompletableFuture<Void> startLift() {
        if (liftFuture != null && !liftFuture.isDone())
            return null;

        RobotLogCommon.d(TAG, "Starting a CompletableFuture for the lift");

        liftMotionCallable = new LiftMotionCallable();
        liftFuture = Threading.launchAsync(liftMotionCallable);

        stopLift.set(false);

        return liftFuture;
    }

    // This will cause the CompletableFuture to exit after
    // it has completed the current operation.
    public void stopLift() {
        stopLift.set(true);
    }

    // To be called from the finally block of FTCAuto and TeleOp.
    public void stopLiftThread() {
        if (liftFuture == null || liftFuture.isDone()) // already stopped?
            return;

        liftMotionCallable.stopThread(); // Force stop now.
        liftFuture.complete(null);
    }

    // CallableFuture that simulates the raising of the lift
    // by time.
    private class LiftMotionCallable extends AutoWorker<Void> {
        LiftMotionCallable() {
            super();
        }

        //**TODO BUT note how we do similar tasks in IntoTheDeep -
        // we just create and launch a Callable ...
        //**TODO to make the lift interruptible in TeleOp you need
        // to put this code into a Callable
               /*
        FTCRobot.MotorTarget<Lifter.LifterPosition, Integer> target = robot.lifter.getClicksToPosition(Lifter.LifterPosition.RAISE);
        Objects.requireNonNull(lifterMotion, TAG + " The Lifter is not in the configuration")
                .moveLifter(target, robot.lifter.velocity, FTCRobot.MotorAction.MOVE_AND_STOP);
         */
        // or, for the simulation, put the 2-second timer into a Callable,
        // launch it as a Future, then test isDone() in the loop below.
        public Void call() {
            RobotLogCommon.d(TAG, "Lift motion future is running");

            Instant start = Instant.now();
            Instant check;
            long secondsElapsed;
            int liftDone = 2; // simulate a two-second lift
            while (!stopThreadRequested() && !Thread.interrupted() && !stopLift.get()) {
                check = Instant.now();
                secondsElapsed = Duration.between(start, check).toSeconds();
                if (secondsElapsed == liftDone)
                    break;

                try {
                    TimeUnit.MILLISECONDS.sleep(25);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            return null;
        }
    }

}


