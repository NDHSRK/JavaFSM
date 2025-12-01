package org.firstinspires.ftc.teamcode.auto;

import java.util.function.Supplier;

// Class for binding a binary button on an FTC Gamepad and for
// tracking its state.
public class FTCButton {

    public static final String TAG = FTCButton.class.getSimpleName();

    public enum State { // button states
        TAP,        // button pressed
        DOUBLE_TAP, // pressed down in quick succession
        HELD,       // button pressed for more than one cycle
        UP,         // moment of release
        OFF,        // button not pressed for more than once cycle
    }

    private static final int DOUBLE_TAP_INTERVAL_MS = 250;
    private final Supplier<Boolean> buttonValue;
    private State state = State.OFF;
    private long lastTapped = -1;

    private long timeHeld = 0;

    public FTCButton(Supplier<Boolean> pButtonValue) {
        buttonValue = pButtonValue;
    }

    //**TODO State.UP is not used; timeHeld not used either - revert to 10.1.1
    public void update() {
        if (buttonValue.get()) {
            if (state == State.OFF || state == State.UP) {
                if (System.currentTimeMillis() - lastTapped < DOUBLE_TAP_INTERVAL_MS) {
                    lastTapped = System.currentTimeMillis();
                    state = State.DOUBLE_TAP;
                    timeHeld = 0;
                } else {
                    lastTapped = System.currentTimeMillis();
                    state = State.TAP;
                    timeHeld = 0;
                }
            }
            else {
                state = State.HELD;
                timeHeld = System.currentTimeMillis() - lastTapped;
            }
        }
        else
        if (state == State.HELD || state == State.TAP || state == State.DOUBLE_TAP)
            state = State.UP;
        else
            state = State.OFF;
    }

    public boolean is(State pState) {
        return state == pState;
    }

    public long getTimeHeld(){
        return timeHeld;
    }
}
