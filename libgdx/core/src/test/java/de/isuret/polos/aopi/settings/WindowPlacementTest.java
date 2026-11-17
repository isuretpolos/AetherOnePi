package de.isuret.polos.aopi.settings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowPlacementTest {

    private static final WindowPlacement.DisplayArea PRIMARY = new WindowPlacement.DisplayArea(0, 0, 1920, 1040);

    @Test
    void normalBoundsAndDisplayModesArePreserved() {
        WindowPlacement saved = new WindowPlacement(120, 100, 1000, 700, true, true);

        assertEquals(saved, saved.fitToDisplays(List.of(PRIMARY)));
    }

    @Test
    void negativeCoordinatesOnAConnectedSecondaryMonitorArePreserved() {
        WindowPlacement.DisplayArea secondary = new WindowPlacement.DisplayArea(-1920, 0, 1920, 1080);
        WindowPlacement saved = new WindowPlacement(-1800, 100, 1000, 700, false, false);

        assertEquals(saved, saved.fitToDisplays(List.of(PRIMARY, secondary)));
    }

    @Test
    void disconnectedMonitorReturnsTheWindowToThePrimaryDisplay() {
        WindowPlacement saved = new WindowPlacement(-3000, -2000, 1000, 700, false, false);
        WindowPlacement restored = saved.fitToDisplays(List.of(PRIMARY));

        assertEquals((1920 - 1000) / 2, restored.x());
        assertEquals((1040 - 700) / 2, restored.y());
    }

    @Test
    void hugeDimensionsAreFittedWithoutIntegerOverflow() {
        WindowPlacement restored = new WindowPlacement(Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, false, false).fitToDisplays(List.of(PRIMARY));

        assertEquals(1856, restored.width());
        assertEquals(976, restored.height());
        assertTrue(restored.x() >= 0);
        assertTrue(restored.y() >= 0);
    }

    @Test
    void smallDisplayTakesPriorityOverUsualMinimumWindowSize() {
        WindowPlacement restored = new WindowPlacement(10, 10, 1285, 721, false, false)
                .fitToDisplays(List.of(new WindowPlacement.DisplayArea(0, 0, 640, 480)));

        assertEquals(576, restored.width());
        assertEquals(416, restored.height());
    }
}
