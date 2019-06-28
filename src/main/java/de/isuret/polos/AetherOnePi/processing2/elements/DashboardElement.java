package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

public class DashboardElement implements IDrawableElement {

    private AetherOneUI p;
    private String[] lines;

    public DashboardElement(AetherOneUI p) {
        this.p = p;

//        lines = p.loadStrings("https://raw.githubusercontent.com/radionics/AetherOnePi/master/AetherOnePi/documentation/dashboardNews.txt");
    }

    @Override
    public void draw() {

        int y = 100;

        p.fill(255);
        p.textFont(p.getGuiElements().getFonts().get("default"),16);

        if (lines != null) {
            for (int i = 0; i < lines.length; i++) {
                p.text(lines[i], 50, y);
                y += 20;
            }
        }
    }

    @Override
    public String getAssignedTabName() {
        return "default";
    }
}
