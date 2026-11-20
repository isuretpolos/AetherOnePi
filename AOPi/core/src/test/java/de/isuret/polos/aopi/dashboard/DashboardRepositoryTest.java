package de.isuret.polos.aopi.dashboard;

import com.badlogic.gdx.files.FileHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void missingDataStartsEmptyWithoutCreatingFiles() {
        DashboardState state = repository().load();

        assertTrue(state.recentCases().isEmpty());
        assertTrue(state.watchlist().isEmpty());
        assertEquals(0, directory.toFile().list().length);
    }

    @Test
    void recentCasesAreDistinctAndCannotEscapeTheCasesDirectory() {
        writeDashboard("""
                {
                  "recentCases": ["Example", "Example", "../outside", "C:\\\\outside", "..", "Second"],
                  "watchlist": []
                }
                """);

        assertEquals(List.of("Example", "Second"), repository().load().recentCases());
        assertThrows(IllegalArgumentException.class, () -> repository().readCaseSummary("../outside"));
    }

    @Test
    void watchlistOnlyIncludesValidResultsBelowTheOriginalAttentionThreshold() {
        writeDashboard("""
                {
                  "watchlist": [
                    {"name": "Needs attention", "generalVitality": 499},
                    {"name": "At threshold", "generalVitality": 500},
                    {"name": "Missing result"},
                    {"name": "", "generalVitality": 100},
                    {"name": "Zero", "generalVitality": 0}
                  ]
                }
                """);

        assertEquals(List.of(new DashboardState.WatchlistEntry("Needs attention", 499),
                new DashboardState.WatchlistEntry("Zero", 0)), repository().load().watchlist());
    }

    @Test
    void invalidDashboardShapeIsReportedRatherThanInventingAnEmptyState() {
        writeDashboard("[]");

        assertThrows(IllegalArgumentException.class, () -> repository().load());
    }

    @Test
    void previewReadsOnlyTheIndependentCaseFile() {
        FileHandle caseFile = new FileHandle(directory.resolve("cases/Example.json").toFile());
        caseFile.parent().mkdirs();
        caseFile.writeString("{\"name\":\"Example\",\"description\":\"A local case\"}", false, "UTF-8");

        assertTrue(repository().readCaseSummary("Example").contains("A local case"));
    }

    private DashboardRepository repository() {
        return new DashboardRepository(new FileHandle(directory.toFile()));
    }

    private void writeDashboard(String json) {
        new FileHandle(directory.resolve("dashboard.json").toFile()).writeString(json, false, "UTF-8");
    }
}
