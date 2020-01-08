package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

/**
 * Manage settings for the Processing2 Gui
 */
public class SettingsScreen implements IDrawableElement {

    public SettingsScreen(AetherOneUI p) {
        Settings settings = p.getSettings();

        for (String key : settings.getBooleans().keySet()) {
            System.out.println(key);
        }
    }

    @Override
    public void draw() {

    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return "SETTINGS";
    }
}
