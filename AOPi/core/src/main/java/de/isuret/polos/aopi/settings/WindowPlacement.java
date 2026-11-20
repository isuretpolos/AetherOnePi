package de.isuret.polos.aopi.settings;

import java.util.List;

/** Normal window bounds are kept separately from fullscreen and maximized state. */
public record WindowPlacement(int x, int y, int width, int height, boolean maximized, boolean fullscreen) {

    public static final int DEFAULT_WIDTH = 1285;
    public static final int DEFAULT_HEIGHT = 721;
    public static final int MIN_WIDTH = 800;
    public static final int MIN_HEIGHT = 600;

    /** Monitor work area in desktop coordinates; negative coordinates are valid. */
    public record DisplayArea(int x, int y, int width, int height) {
    }

    public WindowPlacement fitToDisplays(List<DisplayArea> displays) {
        if (displays.isEmpty()) {
            throw new IllegalArgumentException("At least one display is required.");
        }
        DisplayArea display = displays.getFirst();
        long largestIntersection = 0;
        for (DisplayArea candidate : displays) {
            long overlapWidth = Math.max(0, Math.min((long) x + width, (long) candidate.x + candidate.width)
                    - Math.max(x, candidate.x));
            long overlapHeight = Math.max(0, Math.min((long) y + height, (long) candidate.y + candidate.height)
                    - Math.max(y, candidate.y));
            long intersection = overlapWidth * overlapHeight;
            if (intersection > largestIntersection) {
                largestIntersection = intersection;
                display = candidate;
            }
        }

        // Leave space for decorations so the title bar stays reachable after monitor changes.
        int margin = Math.min(32, Math.max(0, Math.min(display.width, display.height) / 4));
        int availableWidth = Math.max(1, display.width - margin * 2);
        int availableHeight = Math.max(1, display.height - margin * 2);
        int restoredWidth = Math.min(Math.max(MIN_WIDTH, width), availableWidth);
        int restoredHeight = Math.min(Math.max(MIN_HEIGHT, height), availableHeight);
        int restoredX = (int) Math.clamp((long) x, (long) display.x + margin,
                (long) display.x + display.width - margin - restoredWidth);
        int restoredY = (int) Math.clamp((long) y, (long) display.y + margin,
                (long) display.y + display.height - margin - restoredHeight);
        if (largestIntersection == 0) {
            restoredX = display.x + (display.width - restoredWidth) / 2;
            restoredY = display.y + (display.height - restoredHeight) / 2;
        }
        return new WindowPlacement(restoredX, restoredY, restoredWidth, restoredHeight, maximized, fullscreen);
    }
}
