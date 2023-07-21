package de.isuret.polos.AetherOnePi.processing2.elements;

import controlP5.Textfield;
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
            p.line(600,y+2,1200,y+2);

            for (RateObject rate : p.watchlistAnalysis.getRateObjects()) {
                if (rate.getGv() < 900) {
                    y += 20;
                    //p.noFill();
                    if (p.mouseX > 658 && p.mouseX < 1000 && p.mouseY > y-17 && p.mouseY < y) {
                        p.stroke(255,0,0);
                        //p.fill(255);
                        if (p.mousePressed) {
                            p.getAetherOneEventHandler().clearForNewCase();
                            ((Textfield) p.getGuiElements().getCp5().get("NAME")).setText(rate.getNameOrRate());
                            p.getGuiElements().selectCurrentTab("SESSION");
                            p.getGuiElements().cp5.controlWindow.activateTab("SESSION");
                        }
                    } else {
                        p.stroke(255);
                    }
                    p.noFill();
                    p.rect(658,y-17,542,18);
                    p.fill(255);
                    p.stroke(255);
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
