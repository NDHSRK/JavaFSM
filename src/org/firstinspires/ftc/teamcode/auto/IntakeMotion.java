package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.ftcdevcommon.AutoWorker;
import org.firstinspires.ftc.ftcdevcommon.Threading;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

// 4/10/2026 The starting point is a class of the same name copied
// in to this project from commit a3c55d0 (3/6/2026) of project
// FtcDecode_11.1.0_RR_4348.

// Class that starts up a CompletableFuture whose Callable runs
// until it fills the revolver by taking in 3 artifacts or until
// it is interrupted by the driver.
public class IntakeMotion {

    private static final String TAG = IntakeMotion.class.getSimpleName();

    // Thread-related.
    private IntakeCallable intakeCallable;
    private CompletableFuture<Integer> intakeFuture;

    private final AtomicBoolean stopIntake = new AtomicBoolean();
    private volatile boolean pauseIntake = false;
    int artifactsToIntake;
    private int artifactsInRevolver = 0;

    public IntakeMotion() {
    }

    // Start up intake as a CompletableFuture.
    // Return the Future so that the caller can manage
    // its completion. Make sure that this can't be called
    // while an intakeFuture is already running.
    public CompletableFuture<Integer> startIntake(int pArtifactsToIntake) {
        if (intakeFuture != null && !intakeFuture.isDone())
            return null;

        RobotLogCommon.d(TAG, "Starting a CompletableFuture for intake");

        artifactsToIntake = pArtifactsToIntake;
        intakeCallable = new IntakeCallable();
        intakeFuture = Threading.launchAsync(intakeCallable);

        stopIntake.set(false);
        pauseIntake = false;
        return intakeFuture;
    }

    // Pause intake until resume() is called.
    // For use during TeleOp reverse intake.
    public void pause() {
        pauseIntake = true;
    }

    public void resume() {
        pauseIntake = false;
    }

    // This will cause the CompletableFuture to exit after
    // it has completed the current operation.
    public void stopIntake() {
        stopIntake.set(true);
    }

    // To be called from the finally block of FTCAuto and TeleOp.
    public void stopIntakeThread() {
        if (intakeFuture == null || intakeFuture.isDone()) // already stopped?
            return;

        intakeCallable.stopThread(); // Force stop now.
        intakeFuture.complete(0);
    }

    // CallableFuture that simulates the intake of Decode
    // artifacts and exits when the revolver is full (by
    // the expiration of a 3-second timer, i.e. 1 second
    // per artifact) or the driver requests a stop.
    // Returns the number of artifacts in the revolver at
    // the time of completion.
    private class IntakeCallable extends AutoWorker<Integer> {
        IntakeCallable() {
            super();
        }

        public Integer call() {
            RobotLogCommon.d(TAG, "Intake future is running");

            Instant intakeTimer = Instant.now();  // start timer for total elapsed time including pauses

            boolean transitionToPause = true;
            boolean intakePaused = false;
            Instant pauseTimer = Instant.now(); // timer for a single pause
            long totalTimePaused = 0;
            int boundary = 1; // 1 second per artifact
            while (!stopThreadRequested() && !Thread.interrupted() && !stopIntake.get()) {
                // Check for a requested pause in intake while outtake is in progress.
                // Start a pause timer here to get the time spent in this pause.
                if (pauseIntake) {
                    if (transitionToPause) {
                        transitionToPause = false;
                        intakePaused = true;
                        pauseTimer = Instant.now();
                    }
                    continue;
                }

                // If we've just come out of a pause, add the time spent in the
                // pause to the total.
                if (intakePaused) {
                    intakePaused = false;
                    transitionToPause = true; // for the next time around
                    totalTimePaused += Math.abs(Duration.between(intakeTimer, pauseTimer).toMillis());
                }

                // Check to see if the intake timer (minus time spent in all pauses)
                // has crossed a boundary, e.g. zero to one seconds.
                long totalElapsedTime = Duration.between(intakeTimer, Instant.now()).toMillis();
                if ((totalElapsedTime - totalTimePaused) / 1000 == boundary) {
                    artifactsInRevolver++; // increment on a 1-second boundary
                    if (++boundary > artifactsToIntake)
                        break;
                }
            }

            return artifactsInRevolver;
        }
    }

}

