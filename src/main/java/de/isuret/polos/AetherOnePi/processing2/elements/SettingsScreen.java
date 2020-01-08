package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

/**
 * Manage settings for the Processing2 Gui
 */
public class SettingsScreen implements IDrawableElement {

    private AetherOneUI p;

    public SettingsScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {

        Settings settings = p.getSettings();

        p.color(255);
        int y = 90;
        p.text("=== SETTINGS ===",40,y);
        p.stroke(100);
        p.line(290,y,290,500);

        for (String key : settings.getBooleans().keySet()) {
            y = drawSettingsTableRow(y, key);
            p.text(settings.getBoolean(key,false).toString(), 300, y);
        }

        for (String key : settings.getIntegers().keySet()) {
            y = drawSettingsTableRow(y, key);
            p.text(settings.getInteger(key,0).toString(), 300, y);
        }

        for (String key : settings.getStrings().keySet()) {
            y = drawSettingsTableRow(y, key);
            p.text(settings.getString(key,"").toString(), 300, y);
        }
    }

    public int drawSettingsTableRow(int y, String key) {
        y += 16;
        p.color(255);
        p.text(key, 40, y);
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
}
