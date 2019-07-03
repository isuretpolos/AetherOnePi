package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

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
    }

    @Override
    public String getAssignedTabName() {
        return "default";
    }
}
