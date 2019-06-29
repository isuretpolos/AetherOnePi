package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

public class DashboardScreen implements IDrawableElement {

    private AetherOneUI p;

    public DashboardScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {
        String dashboardNews [] = p.loadStrings("https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/dashboardNews.txt");
        int y = 90;

        p.color(255);

        for (String line : dashboardNews) {
            p.text(line, 40, y);
            y += 15;
        }
    }

    @Override
    public String getAssignedTabName() {
        return "default";
    }
}
