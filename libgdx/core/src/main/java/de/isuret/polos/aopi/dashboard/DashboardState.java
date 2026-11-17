package de.isuret.polos.aopi.dashboard;

import java.util.List;

/** Dashboard values belong to this application, never to the Processing UI. */
public record DashboardState(List<String> recentCases, List<WatchlistEntry> watchlist) {

    public DashboardState {
        recentCases = List.copyOf(recentCases);
        watchlist = List.copyOf(watchlist);
    }

    public record WatchlistEntry(String name, int generalVitality) {
    }
}
