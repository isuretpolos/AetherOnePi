package de.isuret.polos.aopi.analysis;

import java.time.Instant;
import java.util.List;

/** Immutable result snapshots can be retained in case history without later GV edits changing them. */
public record AnalysisResult(String id, Instant created, String database, int hitLimit, int trials,
                             Integer generalVitality, List<Row> rows) {

    public AnalysisResult {
        rows = List.copyOf(rows);
    }

    public record Row(String name, String url, String classification, String subfamily, int energeticValue,
                      String potency, int level, Integer generalVitality, int recurring) {

        public Row withVitality(int value) {
            return new Row(name, url, classification, subfamily, energeticValue, potency, level, value, recurring);
        }
    }

    public String asText() {
        StringBuilder text = new StringBuilder("AOPi analysis | ").append(database).append("\n")
                .append(created).append(" | Trials: ").append(trials).append(" | Hit limit: ").append(hitLimit)
                .append("\nGeneral vitality: ").append(generalVitality == null ? "not checked" : generalVitality)
                .append("\n\n");
        int rank = 0;
        for (Row row : rows) {
            text.append(++rank).append(". ").append(row.name()).append(" | EV ").append(row.energeticValue())
                    .append(" | ").append(row.potency()).append(" | Level ").append(row.level())
                    .append(" | GV ").append(row.generalVitality() == null ? "--" : row.generalVitality())
                    .append(" | Recurrence ").append(row.recurring()).append('\n');
        }
        return text.toString();
    }
}
