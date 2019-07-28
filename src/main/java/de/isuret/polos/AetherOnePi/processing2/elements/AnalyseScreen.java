package de.isuret.polos.AetherOnePi.processing2.elements;

import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.StickPad;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

public class AnalyseScreen implements IDrawableElement {

    private AetherOneUI p;
    private StickPad stickPad = new StickPad();
    private boolean browserSupported = false;
    private Date lastClick = Calendar.getInstance().getTime();

    public final static int MAX_ENTRIES = 21;

    public AnalyseScreen(AetherOneUI p) {
        this.p = p;

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            browserSupported = true;
        }
    }

    @Override
    public void draw() {

        if (p.getStickPadMode()) {
            analyzeStickPad();
        }

        p.textFont(p.getGuiElements().getFonts().get("default"), 15);

        if (p.getSelectedDatabase() != null) {
            p.fill(198, 220, 255);
            p.text("SELECTED DATABASE: " + p.getSelectedDatabase(), 35, 88);

            if (p.getGeneralVitality() > 0) {
                p.text("GENERAL VITALITY: " + p.getGeneralVitality(), 35, 510);
            }
        }


        if (p.getAnalysisResult() != null) {

            int y = 110;
            int count = 0;

            p.stroke(180);
            p.fill(255);
            p.text("NO", 35, y);
            p.text("EV", 80, y);
            p.text("RATE / SIGNATURE", 125, y);
            p.text("GV", 804, y);
            p.text("GV RE", 844, y);
            p.text("REC", 890, y);
            p.text("ACTIONS", 925, y);
            p.line(35, y + 2, 1200, y + 2);
            y += 18;

            for (RateObject rate : p.getAnalysisResult().getRateObjects()) {

                // ACTION BUTTONS
                p.fill(0,255,0);
                p.rect(930,y-15,100,15);
                p.fill(0);
                p.stroke(255);
                p.text("BROADCAST",935,y-2);

                if (browserSupported && rate.getUrl() != null) {
                    p.fill(3, 177, 252);
                    p.rect(1035,y-15,40,15);
                    p.fill(0);
                    p.stroke(255);
                    p.text("URL",1040,y-2);
                }

                if (browserSupported) {
                    p.fill(3, 170, 252);
                    p.rect(1080,y-15,65,15);
                    p.fill(0);
                    p.stroke(255);
                    p.text("GOOGLE",1085,y-2);
                }

                //MOUSELINE
                if (p.mouseY >= y - 18 && p.mouseY < y && p.mouseX < 1200) { //de breedde van de muis bij klikken
                    p.noStroke();
                    p.fill(10, 255, 10, 40);
                    p.rect(33, y - 16, 1160, 18);

                    if (p.mousePressed) {

                        // Prevent multiple clicks
                        if (Calendar.getInstance().getTime().getTime() - 1000 >= lastClick.getTime()) {
                            performClickEvent(rate);
                            lastClick = Calendar.getInstance().getTime();
                        }
                    }
                }

                p.fill(255);
                p.stroke(160);

                if (rate.getGv() == p.getGeneralVitality()) {
                    p.fill(130, 219, 255);
                } else if (rate.getGv() > p.getGeneralVitality() && p.getGeneralVitality() >= 1000) {
                    p.fill(39, 255, 0);
                } else if (rate.getGv() > p.getGeneralVitality() && rate.getGv() > 1000) {
                    p.fill(29, 255, 0);
                } else if (rate.getGv() > (p.getGeneralVitality() + 100)) {
                    p.fill(26, 237, 0);
                } else if (rate.getGv() > p.getGeneralVitality()) {
                    p.fill(23, 211, 0);
                } else if (rate.getGv() > 0) {
                    p.fill(200);
                }

                p.text(count, 35, y);
                p.text(rate.getEnergeticValue(), 80, y);
                p.text(rate.getNameOrRate(), 125, y);
                p.text(rate.getGv(), 804, y);

                if (rate.getRecurringGeneralVitality() > 1) {
                    p.text(rate.getRecurringGeneralVitality(), 854, y);
                }

                if (rate.getRecurring() > 0) {
                    p.text(rate.getRecurring(), 900, y);
                }

                p.line(35, y + 2, 1200, y + 2);
                y += 18;
                count++;
                if (count >= MAX_ENTRIES) break;
            }

            p.line(70, 100, 70, y - 15);
            p.line(105, 100, 105, y - 15);
            p.line(800, 100, 800, y - 15);
            p.line(840, 100, 840, y - 15);
            p.line(885, 100, 885, y - 15);
            p.line(920, 100, 920, y - 15);
        }
    }

    public void performClickEvent(RateObject rate) {
        ((Textfield) p.getGuiElements().getCp5().get("SIGNATURE")).setText(rate.getNameOrRate());
        if (rate.getGv() > 0) {
            ((Textfield) p.getGuiElements().getCp5().get("SECONDS")).setText(String.valueOf(rate.getGv()));
        } else {
            ((Textfield) p.getGuiElements().getCp5().get("SECONDS")).setText("60");
        }

        if (p.mouseButton == p.RIGHT || (p.mouseX >= 935 && p.mouseX < 1030)) {
            p.getAetherOneEventHandler().broadcastNow();
        }

        if (browserSupported && rate.getUrl() != null && (p.mouseButton == p.RIGHT || (p.mouseX >= 1040 && p.mouseX < 1080))) {
            openUrl(rate.getUrl());
        }

        if (browserSupported && (p.mouseButton == p.RIGHT || (p.mouseX >= 1080 && p.mouseX < 1145))) {
            openUrl("https://www.google.com/search?q=" + rate.getNameOrRate().replaceAll(" ","+"));
        }
    }

    public void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void analyzeStickPad() {

        if (stickPad.getGeneralVitalityChecking()) {
            p.fill(156, 255, 99);
        } else {
            p.fill(255, 127, 127);
        }

        p.text("STICK PAD ON " + p.mouseX + "," + p.mouseY + " (" + stickPad.getPositions().size() + ")", 850, 88);

        stickPad.addStickPadPosition(p.mouseX, p.mouseY);

        if (stickPad.getPositions().size() >= 500 && !stickPad.getGeneralVitalityChecking()) {
            p.setAnalysisResult(stickPad.analyze(p.getSelectedDatabase()));
            stickPad.setGeneralVitalityChecking(true);
            p.setGvCounter(0);
        } else if (stickPad.getPositions().size() >= 10 && stickPad.getGeneralVitalityChecking()) {
            stickPad.checkGeneralVitality(p);
        }
    }

    @Override
    public String getAssignedTabName() {
        return "ANALYZE";
    }
}
