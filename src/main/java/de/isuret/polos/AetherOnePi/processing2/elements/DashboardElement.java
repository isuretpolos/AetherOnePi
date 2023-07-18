package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.domain.RateObject;
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

        if (p.watchlistRequiresAttention) {

            p.stroke(255);
            y = 100;
            p.text("WATCHLIST REQUIRES ATTENTION", 600, y);
            p.line(600,y+2,1000,y+2);

            for (RateObject rate : p.watchlistAnalysis.getRateObjects()) {
                if (rate.getGv() < 500) {
                    y += 20;
                    p.text(rate.getNameOrRate(), 660, y);
                    p.text(rate.getGv(), 600, y);
                }
            }
        }
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return "default";
    }
}
