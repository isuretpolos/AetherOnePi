package de.isuret.polos.aopi.analysis;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalysisResultSortTest {
    private final AnalysisResult result = new AnalysisResult("id", Instant.EPOCH, "list", 1000, 20, 500, List.of(
            new AnalysisResult.Row("Zulu", "", "Zinc", "Z", 300, "C 10", 3, null, 3),
            new AnalysisResult.Row("alpha", "", "animal", "a", 100, "C 2", 1, 700, 1),
            new AnalysisResult.Row("Middle", "", "Mineral", "m", 200, "C 6", 2, 300, 2)));

    @Test
    void everyColumnSortsBothWaysWithoutChangingAnalysisRanks() {
        for (AnalysisResultSort column : AnalysisResultSort.values()) {
            List<Integer> ascending = switch (column) {
                case RANK -> List.of(0, 1, 2);
                case GV -> List.of(2, 1, 0);
                default -> List.of(1, 2, 0);
            };
            List<Integer> descending = switch (column) {
                case RANK -> List.of(2, 1, 0);
                case GV -> List.of(1, 2, 0);
                default -> List.of(0, 2, 1);
            };
            assertEquals(ascending, column.indices(result, false), column.name());
            assertEquals(descending, column.indices(result, true), column.name());
        }
        assertEquals("Zulu", result.rows().getFirst().name());
        assertEquals(300, result.rows().getFirst().energeticValue());
    }

    @Test
    void equalValuesKeepTheirOriginalOrderInBothDirections() {
        AnalysisResult equal = new AnalysisResult("id", Instant.EPOCH, "list", 1000, 20, null,
                List.of(result.rows().getFirst(), result.rows().getFirst(), result.rows().getFirst()));
        for (AnalysisResultSort column : AnalysisResultSort.values()) {
            if (column == AnalysisResultSort.RANK) { continue; }
            assertEquals(List.of(0, 1, 2), column.indices(equal, false));
            assertEquals(List.of(0, 1, 2), column.indices(equal, true));
        }
    }
}
