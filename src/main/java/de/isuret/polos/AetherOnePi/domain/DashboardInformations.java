package de.isuret.polos.AetherOnePi.domain;

import java.util.ArrayList;
import java.util.List;

public class DashboardInformations {

    private List<String> recentlyLoadedCases = new ArrayList<>();

    public List<String> getRecentlyLoadedCases() {
        return recentlyLoadedCases;
    }

    public void setRecentlyLoadedCases(List<String> recentlyLoadedCases) {
        this.recentlyLoadedCases = recentlyLoadedCases;
    }
}
