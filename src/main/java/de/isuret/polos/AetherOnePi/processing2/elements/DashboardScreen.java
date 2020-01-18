package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

import java.io.File;

public class DashboardScreen implements IDrawableElement {

    private static final String DASHBOARD_NEWS_TXT = "https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/dashboardNews.txt";
    private static final String TEMPORARY_DASHBOARD_TEXT_TXT = "temporaryDashboardText.txt";
    private AetherOneUI p;
    private String dashboardNews [];

    public DashboardScreen(AetherOneUI p) {
        this.p = p;

        (new Thread() {
            public void run() {
                try {
                    dashboardNews = p.loadStrings(DASHBOARD_NEWS_TXT);
                    p.saveStrings(TEMPORARY_DASHBOARD_TEXT_TXT, dashboardNews);
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

        y += 20;

        int maxCount = 0;

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
