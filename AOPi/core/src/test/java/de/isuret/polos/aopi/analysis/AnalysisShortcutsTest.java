package de.isuret.polos.aopi.analysis;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisShortcutsTest {

    @Test
    void eitherControlTriggersOnlyOnBareRelease() {
        AtomicInteger calls = new AtomicInteger();
        var keys = new AnalysisShortcuts(() -> true, calls::incrementAndGet, () -> { });
        for (int control : new int[]{Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT}) {
            keys.keyDown(control);
            keys.keyDown(control);
            assertEquals(control == Input.Keys.CONTROL_LEFT ? 0 : 1, calls.get());
            assertTrue(keys.keyUp(control));
            assertFalse(keys.keyUp(control));
        }
        assertEquals(2, calls.get());
    }

    @Test
    void controlCombinationsNeverTriggerAnalysis() {
        var keys = new AnalysisShortcuts(() -> true, () -> fail("Ctrl combination triggered analysis."), () -> { });
        keys.keyDown(Input.Keys.CONTROL_LEFT);
        keys.keyDown(Input.Keys.C);
        keys.keyUp(Input.Keys.C);
        keys.keyDown(Input.Keys.CONTROL_LEFT);
        assertFalse(keys.keyUp(Input.Keys.CONTROL_LEFT));
        keys.keyDown(Input.Keys.SHIFT_LEFT);
        keys.keyDown(Input.Keys.CONTROL_RIGHT);
        keys.keyUp(Input.Keys.SHIFT_LEFT);
        assertFalse(keys.keyUp(Input.Keys.CONTROL_RIGHT));
    }

    @Test
    void returnTriggersOncePerPressAndModifiersDoNotTriggerVitality() {
        AtomicInteger calls = new AtomicInteger();
        var keys = new AnalysisShortcuts(() -> true, () -> { }, calls::incrementAndGet);
        keys.keyDown(Input.Keys.ENTER);
        keys.keyDown(Input.Keys.ENTER);
        assertEquals(1, calls.get());
        keys.keyUp(Input.Keys.ENTER);
        keys.keyDown(Input.Keys.NUMPAD_ENTER);
        keys.keyUp(Input.Keys.NUMPAD_ENTER);
        keys.keyDown(Input.Keys.CONTROL_LEFT);
        keys.keyDown(Input.Keys.ENTER);
        keys.keyUp(Input.Keys.ENTER);
        keys.keyUp(Input.Keys.CONTROL_LEFT);
        assertEquals(2, calls.get());
    }

    @Test
    void inactiveTabsAndDialogsCannotTriggerShortcuts() {
        AtomicBoolean enabled = new AtomicBoolean(false);
        var keys = new AnalysisShortcuts(enabled::get, () -> fail("Inactive analysis shortcut."),
                () -> fail("Inactive GV shortcut."));
        keys.keyDown(Input.Keys.CONTROL_LEFT);
        enabled.set(true);
        assertFalse(keys.keyUp(Input.Keys.CONTROL_LEFT));
        enabled.set(false);
        assertFalse(keys.keyDown(Input.Keys.ENTER));
    }
}
