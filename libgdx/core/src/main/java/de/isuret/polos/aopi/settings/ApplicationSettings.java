package de.isuret.polos.aopi.settings;

import java.util.EnumMap;
import java.util.Map;

/** In-memory settings shared by every page. No rendering or file access happens here. */
public class ApplicationSettings {

    public static final int MIN_FONT_SIZE = 14;
    public static final int MAX_FONT_SIZE = 32;
    public static final int DEFAULT_FONT_SIZE = 16;
    public static final String DEFAULT_PANEL_COLOR = "1C1625";

    private final Map<Setting, Boolean> values = new EnumMap<>(Setting.class);
    private int fontSize = DEFAULT_FONT_SIZE;
    private WindowPlacement windowPlacement;
    private String panelColor = DEFAULT_PANEL_COLOR;
    private int panelTransparency;

    public ApplicationSettings() {
        for (Setting setting : Setting.values()) {
            values.put(setting, setting.defaultValue());
        }
    }

    public boolean isEnabled(Setting setting) {
        return values.get(setting);
    }

    public void setEnabled(Setting setting, boolean enabled) {
        values.put(setting, enabled);
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
    }

    public WindowPlacement getWindowPlacement() {
        return windowPlacement;
    }

    public void setWindowPlacement(WindowPlacement windowPlacement) {
        this.windowPlacement = windowPlacement;
    }

    public String getPanelColor() {
        return panelColor;
    }

    public void setPanelColor(String color) {
        String normalized = color.strip().replaceFirst("^#", "").toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[0-9A-F]{6}")) {
            throw new IllegalArgumentException("Use a six-digit RGB color, for example #1C1625.");
        }
        panelColor = normalized;
    }

    public int getPanelTransparency() {
        return panelTransparency;
    }

    public void setPanelTransparency(int percent) {
        panelTransparency = Math.max(0, Math.min(100, percent));
    }
}
