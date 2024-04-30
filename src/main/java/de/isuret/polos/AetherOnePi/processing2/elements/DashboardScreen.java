package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

import java.io.File;

public class DashboardScreen implements IDrawableElement {

    private static final String DASHBOARD_NEWS_TXT = "https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/dashboardNews.txt";
    private static final String TEMPORARY_DASHBOARD_TEXT_TXT = "temporaryDashboardText.txt";
    private AetherOneUI p;
    private String dashboardNews [];
    private float blink = 256;
    private float blinkRate = 0.7f;
    private int click = 0;

    private boolean showPatreonSection = false;

    public DashboardScreen(AetherOneUI p) {
        this.p = p;
        p.getDataService().init();

        (new Thread() {
            public void run() {
                try {
                    dashboardNews = p.loadStrings(DASHBOARD_NEWS_TXT);
                    p.saveStrings(TEMPORARY_DASHBOARD_TEXT_TXT, dashboardNews);
                    if (p.getHotbitsClient().getInteger(0,1000) >= 777) {
                        showPatreonSection = true;
                    }
                } catch (Exception e) {
                    dashboardNews = p.loadStrings(TEMPORARY_DASHBOARD_TEXT_TXT);
                }
            }
        }).start();
    }

    @Override
    public void draw() {

        int y = 90;

        p.color(255);

        if (dashboardNews != null) {
            for (String line : dashboardNews) {
                p.text(line, 40, y);
                y += 15;
            }
        }

        if (blink > 256 || blink < 0) {
            blinkRate = blinkRate * -1;
        }

        if (click > 0) {
            click -= 1;
        }

        if (showPatreonSection) {
            blink = blink - blinkRate;
            p.stroke(60,179,34,blink);
            p.noFill();
            p.rect(40,y - 10,400,70);
            y += 20;
            p.textFont(p.getGuiElements().getFonts().get("default"), 32);
            p.text("Support me on PATREON", 50, y);
            y += 28;
            p.textFont(p.getGuiElements().getFonts().get("default"), 16);
            p.text("and get FREE courses on radionics and homeopathy!", 50, y);
            p.fill(60, 179, 34, blink);
            p.text("and get FREE courses on radionics and homeopathy!", 50, y);
            y += 20;
            if (p.mouseX >= 40 && p.mouseX < 400 && p.mouseY < y && p.mouseY >= y - 70) {
                p.noStroke();
                p.fill(0, 255, 0, 40f);
                p.rect(40, y - 78, 400, 70);

                if (p.mousePressed && click == 0) {
                    click = 100;
                    p.getAetherOneEventHandler().openWebsiteInDefaultBrowser("https://patreon.com/aetherone");
                }
            }
        }

        int maxCount = 0;
        p.color(255);
        if (p.getDataService().getDashboardInformations().getRecentlyLoadedCases().size() > 0) {
            p.fill(255);
            p.text("-----------------", 50, y);
            y += 20;
            p.text("=== LAST CASES ===", 50, y);
            y += 20;
        }

        for (String dataBaseName : p.getDataService().getDashboardInformations().getRecentlyLoadedCases()) {
            if (p.mouseX >= 50 && p.mouseX < 300 && p.mouseY < y && p.mouseY >= y - 20) {
                p.noStroke();
                p.fill(0,255,0,50f);
                p.rect(50, y - 13, 300, 18);

                if (p.mousePressed) {
                    p.getAetherOneEventHandler().loadCaseFile(new File("cases/" + dataBaseName + ".json"));
                }
            }
            p.fill(255);
            p.text(dataBaseName, 50, y);
            y += 20;

            maxCount++;
            if (maxCount > 10) {
                break;
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
