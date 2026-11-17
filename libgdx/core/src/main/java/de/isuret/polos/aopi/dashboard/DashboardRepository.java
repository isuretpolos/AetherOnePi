package de.isuret.polos.aopi.dashboard;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/** Reads the new application's dashboard snapshot; no old AOPi files are accessed. */
public class DashboardRepository {

    private final FileHandle directory;

    public DashboardRepository(FileHandle directory) {
        this.directory = directory;
    }

    public DashboardState load() {
        FileHandle file = directory.child("dashboard.json");
        if (!file.exists()) {
            return new DashboardState(List.of(), List.of());
        }

        JsonValue root = new JsonReader().parse(file);
        if (!root.isObject()) {
            throw new IllegalArgumentException("Dashboard data must be a JSON object.");
        }
        List<String> recentCases = new ArrayList<>();
        JsonValue cases = root.get("recentCases");
        if (cases != null && cases.isArray()) {
            for (JsonValue item : cases) {
                if (item.isString() && isSafeCaseName(item.asString()) && !recentCases.contains(item.asString())) {
                    recentCases.add(item.asString());
                    if (recentCases.size() == 10) {
                        break;
                    }
                }
            }
        }

        List<DashboardState.WatchlistEntry> watchlist = new ArrayList<>();
        JsonValue entries = root.get("watchlist");
        if (entries != null && entries.isArray()) {
            for (JsonValue entry : entries) {
                String name = entry.getString("name", "").trim();
                int vitality = entry.getInt("generalVitality", -1);
                // Preserve the original dashboard's attention threshold.
                if (!name.isEmpty() && vitality >= 0 && vitality < 500) {
                    watchlist.add(new DashboardState.WatchlistEntry(name, vitality));
                }
            }
        }
        return new DashboardState(recentCases, watchlist);
    }

    public String readCaseSummary(String name) {
        if (!isSafeCaseName(name)) {
            throw new IllegalArgumentException("Invalid case name.");
        }
        FileHandle file = directory.child("cases").child(name + ".json");
        if (!file.exists()) {
            return "This case is listed in the dashboard, but its file is missing.";
        }
        JsonValue caseData = new JsonReader().parse(file);
        return caseData.getString("name", name) + "\n\n" + caseData.getString("description", "No description.")
                + "\n\nCase editing will be available when the Session screen is implemented.";
    }

    private boolean isSafeCaseName(String name) {
        // A displayed case name must never resolve outside this application's cases folder.
        return !name.isBlank() && !name.contains("/") && !name.contains("\\")
                && !name.contains(":") && !name.equals(".") && !name.equals("..");
    }
}
