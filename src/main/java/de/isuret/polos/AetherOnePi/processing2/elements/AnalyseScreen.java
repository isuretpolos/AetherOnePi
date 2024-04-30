package de.isuret.polos.AetherOnePi.processing2.elements;

import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.Session;
import de.isuret.polos.AetherOnePi.domain.StickPad;
import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyseScreen implements IDrawableElement, MouseClickObserver {

    private AetherOneUI p;
    private StickPad stickPad = new StickPad();
    private boolean browserSupported = false;
    private boolean mouseClickOccurred = false;
    private Long lastMouseClick = null;

    private int page = 1;
    public final static int MAX_ENTRIES = 21;
    public final static int MAX_ENTRIES_INTERNAL = 105;

    public AnalyseScreen(AetherOneUI p) {
        this.p = p;

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            browserSupported = true;
        }
    }

    @Override
    public void draw() {

        if (p.getStickPadMode() || p.getStickPadGeneralVitalityMode()) {
            analyzeStickPad();
        }

        p.textFont(p.getGuiElements().getFonts().get("default"), 15);

        if (p.getSelectedDatabase() != null) {
            p.fill(198, 220, 255);
            p.text("SELECTED DATABASE: " + p.getSelectedDatabase(), 35, 88);

            if (p.getGeneralVitality() > 0) {
                if (p.getGvCounter() == 0) {
                    p.fill(0, 255, 0);
                    p.text(">> CHECK GENERAL VITALITY <<", 35, 510);
                } else {
                    String trials = "";
                    if (p.getAnalysisResult().getNumberOfTrials() != null) {
                        trials = " (runs = " + p.getAnalysisResult().getNumberOfTrials() + ")";
                    }
                    p.text("GENERAL VITALITY: " + p.getGeneralVitality() + trials, 35, 510);
                }
            } else if (p.getAnalysisResult() != null){
                p.fill(0, 255, 0);
                p.text(">> CHECK GENERAL VITALITY <<", 35, 510);
            }
        }

        if (p.getCaseObject() != null && p.getCaseObject().getSessionList() != null) {

            if (p.getCaseObject().getSessionList().size() > 0 && p.getAnalysisPointer() != null) {
                Session session = p.getCaseObject().getSessionList().get(p.getAnalysisPointer());
                if (session != null && session.getCreated() != null) {
                    DateFormat formatter = new SimpleDateFormat();
                    p.fill(255);
                    p.text(formatter.format(session.getCreated().getTime()), 400, 110);
                }
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
            p.text("POTENCY", 600, y);
            p.text("LVL", 724, y);
            p.text("HIT", 764, y);
            p.text("GV", 804, y);
            p.text("GV RE", 844, y);
            p.text("REC", 890, y);
            p.text("ACTIONS", 925, y);
            p.line(35, y + 2, 1200, y + 2);
            y += 18;

            Integer highestGV = null;
            Integer highestY = null;

            drawAnalysisBroadband();

            final Map<String, Integer> words = new HashMap<>();

            // collect words for later use in marking reoccurrences
            for (RateObject rate : p.getAnalysisResult().getRateObjects()) {

                String name = rate.getNameOrRate().trim();

                if (name.contains(" ")) {

                    String parts[] = name.split(" ");

                    for (String part : parts) {

                        part = part.replaceAll("-","").replaceAll(">","").trim();

                        if (part.length() < 3) continue;

                        if (words.get(part) != null) {
                            words.put(part, words.get(part) + 1);
                        } else {
                            words.put(part, 1);
                        }
                    }
                } else {
                    if (words.get(name) != null) {
                        words.put(name, words.get(name) + 1);
                    } else {
                        words.put(name, 1);
                    }
                }
            }

            // Calculate start and end index for the sublist
            int startIndex = (page -1) * MAX_ENTRIES;
            int endIndex = Math.min(startIndex + MAX_ENTRIES, p.getAnalysisResult().getRateObjects().size());

            // Get the sublist based on the paging parameters
            List<RateObject> pageList = p.getAnalysisResult().getRateObjects().subList(startIndex, endIndex);

            // draw the rate table
            for (RateObject rate : pageList) {

                // LEVEL
                p.fill(255);

                if (rate.getPotency() != null) {
                    p.text(rate.getPotency(), 600, y - 2);
                }

                p.text(rate.getLevel(), 724, y - 2);

                // ACTION BUTTONS
                p.fill(0, 255, 0);
                p.rect(930, y - 15, 100, 15);
                p.fill(0);
                p.stroke(255);
                p.text("BROADCAST", 935, y - 2);

                if (browserSupported && rate.getUrl() != null) {
                    p.fill(3, 177, 252);
                    p.rect(1035, y - 15, 40, 15);
                    p.fill(0);
                    p.stroke(255);
                    p.text("URL", 1040, y - 2);
                }

                if (browserSupported) {
                    p.fill(3, 170, 252);
                    p.rect(1080, y - 15, 65, 15);
                    p.fill(0);
                    p.stroke(255);
                    p.text("GOOGLE", 1085, y - 2);
                }

                if (p.getSelectedDatabase().toLowerCase().contains("clarke")) {
                    p.fill(120, 170, 252);
                    p.rect(1150, y - 15, 60, 15);
                    p.fill(0);
                    p.stroke(255);
                    p.text("CLARKE", 1155, y - 2);
                }

                //MOUSELINE
                if (p.mouseY >= y - 18 && p.mouseY < y && p.mouseX < 1200) {
                    p.noStroke();
                    p.fill(10, 255, 10, 40);
                    p.rect(33, y - 16, 1190, 18);

                    if (mouseClickOccurred) {
                        mouseClickOccurred = false;
                        this.lastMouseClick = Calendar.getInstance().getTimeInMillis();
                        // Prevent multiple clicks
                        performClickEvent(rate);
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

                p.text(count + ((page - 1) * MAX_ENTRIES), 35, y);
                p.text(rate.getEnergeticValue(), 80, y);
                p.text(rate.getNameOrRate(), 125, y);
                p.text(rate.getGv(), 804, y);

                // Mark reoccurrence of word parts in the list
                String name = rate.getNameOrRate();

                if (name.contains(" ")) {

                    String parts[] = name.split(" ");

                    for (String part : parts) {

                        part = part.replaceAll("-","").replaceAll(">","").trim();

                        if (part.length() < 3) continue;

                        if (words.get(part) != null && words.get(part) > 1) {
                            p.noStroke();
                            p.fill(200,0,0,50f);
                            p.rect(105, y - 15, 700, 15);
                            break;
                        }
                    }
                } else if (words.get(name) != null && words.get(name) > 1) {
                    p.noStroke();
                    p.fill(200,0,0,50f);
                    p.rect(105, y - 15, 65, 15);
                }

                p.stroke(160);

                if (highestGV == null || highestGV < rate.getGv()) {
                    highestGV = rate.getGv();
                    highestY = y;
                }

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
            p.line(590, 100, 590, y - 15);
            p.line(720, 100, 720, y - 15);
            p.line(760, 100, 760, y - 15);
            p.line(800, 100, 800, y - 15);
            p.line(840, 100, 840, y - 15);
            p.line(885, 100, 885, y - 15);
            p.line(920, 100, 920, y - 15);

            p.stroke(255,0,0);
            p.fill(255);
            if (highestGV != null && highestY != null) {
                p.text(highestGV, 764, highestY);
            }

            // page selection
            if (p.getAnalysisResult().getRateObjects().size() > MAX_ENTRIES) {
                p.noStroke();
                for (int i=0; i < p.getAnalysisResult().getRateObjects().size() / MAX_ENTRIES; i++) {

                    if (p.mouseX > 930 + ((i) * 22) && p.mouseX < 930 + ((i) * 22) + 22 && p.mouseY > 75 && p.mouseY < 95) {
                        p.stroke(0,255,0);
                        if (p.mousePressed) {
                            page = i + 1;
                            System.out.println(i);
                        }
                    } else {
                        p.stroke(255);
                    }

                    if (page == i + 1) {
                        p.fill(0, 200, 0);
                    } else {
                        p.fill(50, 0, 200);
                    }
                    p.rect(930 + (i * 22), 75, 20, 20);

                    p.fill(255);
                    p.text(String.valueOf(i+1), 937 + (i * 22), 90);
                }
            }
        }

        if (mouseClickOccurred) {
            p.fill(255);
            p.text("CLICK",10,20);

            if (this.lastMouseClick + 500 > Calendar.getInstance().getTimeInMillis()) {
                this.mouseClickOccurred = false;
            }
        }

        if (p.getTrainingSignature() != null) {
            p.fill(255);
            p.text("TRAINING MODE ...",450,510);
        }

        if (p.getTrainingSignature() != null && !p.getTrainingSignatureCovered()) {
            p.fill(255);
            p.text(p.getTrainingSignature(),600,510);
        }

        if (p.getGvCounter() > 0 && p.getGvCounter() <= AnalyseScreen.MAX_ENTRIES) {
            p.fill(0, 255, 0);
            p.noStroke();
            p.rect(0, 94 + (p.getGvCounter() * 18), 10, 18);
            p.stroke(0,255,0);
            p.line(0, 94 + (p.getGvCounter() * 18),900, 94 + (p.getGvCounter() * 18));
            p.line(0, 112 + (p.getGvCounter() * 18),900, 112 + (p.getGvCounter() * 18));
        }
    }

    private void drawAnalysisBroadband() {

        final float bandWidth = 850;
        final float maxGV = getMaxGV();

        if (maxGV == 0) return;

        float relation = 0f;

            relation = bandWidth / maxGV;

        p.fill(0,255,0,70f);
        p.noStroke();
        p.rect(300,500,bandWidth,40);

        // -------------------------------------------------------
        // --- Scale relatively to the bandWidth and max value ---
        p.pushMatrix();
        p.scale(relation,1f); // 2D Scaling only on X-axis
        // General Vitality
        p.fill(0,255,255);
        p.rect(300 + p.getGeneralVitality(),500,4,40);

        // Max General Vitality of all rates
        p.fill(255,0,0);
        p.rect(300 + maxGV,500,4,40);

        // All values on the bandWith
        for (RateObject rate : p.getAnalysisResult().getRateObjects()) {
            if (rate.getGv() == 0) continue;
            p.fill(255);
            p.rect(300 + rate.getGv(),500,1,40);
        }

        p.popMatrix();
        // --- End of scaling --
        // -------------------------------------------------------
    }

    private int getMaxGV() {

        int count = 0;
        int maxGV = 0;

        for (RateObject rate : p.getAnalysisResult().getRateObjects()) {

            if (rate.getGv() > maxGV) {
                maxGV = rate.getGv();
            }

            count++;
            if (count >= MAX_ENTRIES) break;
        }

        if (p.getGeneralVitality() > maxGV) {
            maxGV = p.getGeneralVitality();
        }

        return maxGV;
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    public void performClickEvent(RateObject rate) {
        ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SIGNATURE)).setText(rate.getNameOrRate());
        if (rate.getGv() > 0) {
            ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SECONDS)).setText(String.valueOf(rate.getGv()));
        } else {
            ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SECONDS)).setText("60");
        }

        if (p.mouseButton == p.RIGHT || (p.mouseX >= 935 && p.mouseX < 1030)) {
            p.getAetherOneEventHandler().broadcastNow();
        }

        if (browserSupported && rate.getUrl() != null && (p.mouseButton == p.RIGHT || (p.mouseX >= 1040 && p.mouseX < 1080))) {
            openUrl(rate.getUrl());
        }

        if (browserSupported && (p.mouseButton == p.RIGHT || (p.mouseX >= 1080 && p.mouseX < 1145))) {
            openUrl("https://www.google.com/search?q=" + rate.getNameOrRate().replaceAll(" ", "+"));
        }

        if (p.mouseButton == p.RIGHT || (p.mouseX >= 1150 && p.mouseX < 1210)) {
            openFile(p.getAnalyseService().analyzeClarke(rate.getNameOrRate(), p.getAnalysisResult()));
        }
    }

    public void openFile(File file) {
        try {
            Desktop.getDesktop().browse(file.toURI());
        } catch (IOException e) {
            e.printStackTrace();
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

        if (!stickPad.getGeneralVitalityChecking()) {
            stickPad.setGeneralVitalityChecking(p.getStickPadGeneralVitalityMode());
        }

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
        return AetherOneConstants.ANALYZE;
    }

    @Override
    public void mouseClicked() {

        if (this.lastMouseClick == null) {
            this.lastMouseClick = Calendar.getInstance().getTimeInMillis();
            this.mouseClickOccurred = true;
            return;
        }

        if (this.lastMouseClick + 500 < Calendar.getInstance().getTimeInMillis()) {
            this.mouseClickOccurred = true;
        } else {
            this.mouseClickOccurred = false;
        }

        this.lastMouseClick = Calendar.getInstance().getTimeInMillis();
    }
}
