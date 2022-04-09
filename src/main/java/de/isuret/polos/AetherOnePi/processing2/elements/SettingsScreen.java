package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.domain.Settings;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;

import java.awt.*;

/**
 * Manage settings for the Processing2 Gui
 */
public class SettingsScreen implements IDrawableElement, MouseClickObserver {

    public static final String BROADCAST_DELTA_TIME = "broadcast.delta.time";
    public static final String BROADCAST_SINGLE_RATES_ONLY = "broadcast.single.rates.only";
    public static final String ANALYSIS_VERY_HIGH_MAX_HIT = "analysis.very.high.max.hit";
    public static final String PLAY_SOUND = "play.binaural.sound";
    public static final String DYNAMIC_ADJUSTMENTS = "dynamic.adjustments";
    private AetherOneUI p;
    private boolean mouseClicked = false;

    public SettingsScreen(AetherOneUI p) {
        this.p = p;

        Settings settings = p.getSettings();
        // init settings
        settings.getBoolean(BROADCAST_DELTA_TIME, false);
        settings.getBoolean(BROADCAST_SINGLE_RATES_ONLY, false);
        settings.getBoolean(ANALYSIS_VERY_HIGH_MAX_HIT, false);
        settings.getBoolean(PLAY_SOUND, false);
        settings.getBoolean(DYNAMIC_ADJUSTMENTS, false);
        AetherOnePiProcessingConfiguration.saveAllSettings();
    }

    @Override
    public void draw() {

        Settings settings = p.getSettings();

        p.color(255);
        int y = 90;
        int x2 = 400;
        p.text("=== SETTINGS ===",40,y);
        p.stroke(100);
        p.line(x2 - 6,y,x2 - 6,500);

        for (String key : settings.getBooleans().keySet()) {

            if (!settings.getBoolean(key,false)) {
                p.fill(255,0,0);
            } else {
                p.fill(0,255,0);
            }
            p.rect(32,y + 4,42,12);
            Rectangle rect = new Rectangle(32,y + 4,42,12);

            if (mouseClicked && rect.contains(p.getMousePoint())) {
                settings.setBoolean(key, !settings.getBoolean(key, false));
                AetherOnePiProcessingConfiguration.saveAllSettings();
            }

            p.fill(255);
            y = drawSettingsTableRow(y, key);
            p.text(settings.getBoolean(key,false).toString(), x2, y);
        }

        for (String key : settings.getIntegers().keySet()) {
            y = drawSettingsTableRow(y, key);
            p.text(settings.getInteger(key,0).toString(), x2, y);
        }

        for (String key : settings.getStrings().keySet()) {
            y = drawSettingsTableRow(y, key);
            p.text(settings.getString(key,"").toString(), x2, y);
        }

        if (mouseClicked) mouseClicked = false;
    }

    public int drawSettingsTableRow(int y, String key) {
        y += 16;
        p.color(255);
        p.text(key, 80, y);
        p.stroke(100);
        p.line(30, y + 2, 1000, y + 2);
        return y;
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return "SETTINGS";
    }

    @Override
    public void mouseClicked() {
        mouseClicked = true;
    }
}
