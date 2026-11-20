package de.isuret.polos.aopi.analysis;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

import java.util.BitSet;
import java.util.function.BooleanSupplier;

/** A bare Ctrl release triggers analysis; Ctrl combinations retain their normal behavior. */
public class AnalysisShortcuts extends InputAdapter {

    private final BooleanSupplier enabled;
    private final Runnable analyze;
    private final Runnable vitality;
    private final BitSet pressed = new BitSet();
    private boolean bareControl;

    public AnalysisShortcuts(BooleanSupplier enabled, Runnable analyze, Runnable vitality) {
        this.enabled = enabled;
        this.analyze = analyze;
        this.vitality = vitality;
    }

    @Override
    public boolean keyDown(int keycode) {
        boolean repeated = pressed.get(keycode);
        pressed.set(keycode);
        if (isControl(keycode)) {
            if (!repeated) {
                bareControl = pressed.cardinality() == 1 && enabled.getAsBoolean();
            }
        } else {
            bareControl = false;
        }
        if ((keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER)
                && pressed.cardinality() == 1 && enabled.getAsBoolean()) {
            if (!repeated) {
                vitality.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        boolean wasPressed = pressed.get(keycode);
        pressed.clear(keycode);
        if (isControl(keycode)) {
            boolean trigger = wasPressed && bareControl && pressed.isEmpty() && enabled.getAsBoolean();
            bareControl = false;
            if (trigger) {
                analyze.run();
                return true;
            }
        }
        return false;
    }

    private boolean isControl(int keycode) {
        return keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.CONTROL_RIGHT;
    }
}
