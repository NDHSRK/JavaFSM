package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.ftcdevcommon.AutoWorker;
import org.firstinspires.ftc.ftcdevcommon.Threading;
import org.firstinspires.ftc.ftcdevcommon.platform.intellij.RobotLogCommon;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// 4/10/2026 Copied in to this project from commit a3c55d0
// (3/6/2026) of project FtcDecode_11.1.0_RR_4348.

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
    // time) or the driver requests a stop. Returns the
    // number of artifacts in the revolver at the time of
    // completion.
    private class IntakeCallable extends AutoWorker<Integer> {
        IntakeCallable() {
            super();
        }

        public Integer call() {
            RobotLogCommon.d(TAG, "Intake future is running");

            Instant start = Instant.now();
            Instant check;
            long secondsElapsed;
            int boundary = 1;
            while (!stopThreadRequested() && !Thread.interrupted() && !stopIntake.get()) {
                // Check for a requested pause in intake while outtake is in progress.
                if (pauseIntake)
                    continue;

                // Simulate the intake of one artifact per second up to the target
                check = Instant.now();
                secondsElapsed = Duration.between(start, check).toSeconds();
                if (secondsElapsed == boundary) {
                    artifactsInRevolver++; // increment on a 1-second boundary
                    if (++boundary > artifactsToIntake)
                        break;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(25);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            return artifactsInRevolver;
        }
    }

}

