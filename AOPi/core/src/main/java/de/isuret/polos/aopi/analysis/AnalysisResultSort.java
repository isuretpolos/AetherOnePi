package de.isuret.polos.aopi.analysis;

import java.util.ArrayList;
import java.util.List;

/** Sort display indices, preserving the immutable ranking and GV traversal order. */
public enum AnalysisResultSort {
    RANK("#"), EV("EV"), RATE("RATE"), POTENCY("POTENCY"), LEVEL("LEVEL"),
    GV("GV"), RECURRENCE("REC."), CLASSIFICATION("CLASSIFICATION"), SUBFAMILY("SUBFAMILY");

    private final String heading;

    AnalysisResultSort(String heading) {
        this.heading = heading;
    }

    public String heading() { return heading; }

    public List<Integer> indices(AnalysisResult result, boolean descending) {
        List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < result.rows().size(); index++) { indices.add(index); }
        indices.sort((left, right) -> {
            var a = result.rows().get(left);
            var b = result.rows().get(right);
            // Missing GV belongs at the end in either direction.
            if (this == GV && (a.generalVitality() == null || b.generalVitality() == null)) {
                if (a.generalVitality() == null && b.generalVitality() != null) { return 1; }
                if (b.generalVitality() == null && a.generalVitality() != null) { return -1; }
                return Integer.compare(left, right);
            }
            int compared = switch (this) {
                case RANK -> Integer.compare(left, right);
                case EV -> Integer.compare(a.energeticValue(), b.energeticValue());
                case RATE -> a.name().compareToIgnoreCase(b.name());
                case POTENCY -> comparePotency(a.potency(), b.potency());
                case LEVEL -> Integer.compare(a.level(), b.level());
                case GV -> Integer.compare(a.generalVitality(), b.generalVitality());
                case RECURRENCE -> Integer.compare(a.recurring(), b.recurring());
                case CLASSIFICATION -> a.classification().compareToIgnoreCase(b.classification());
                case SUBFAMILY -> a.subfamily().compareToIgnoreCase(b.subfamily());
            };
            if (compared == 0) { return Integer.compare(left, right); }
            return descending ? -Integer.signum(compared) : compared;
        });
        return indices;
    }

    private static int comparePotency(String left, String right) {
        // Compare the scale first, then its numeric strength (C 2 before C 10).
        String leftScale = left.replaceAll("[0-9]", "").strip();
        String rightScale = right.replaceAll("[0-9]", "").strip();
        int scale = leftScale.compareToIgnoreCase(rightScale);
        if (scale != 0) { return scale; }
        String a = left.replaceAll("[^0-9]", "");
        String b = right.replaceAll("[^0-9]", "");
        if (a.isEmpty() || b.isEmpty()) { return left.compareToIgnoreCase(right); }
        return new java.math.BigInteger(a).compareTo(new java.math.BigInteger(b));
    }
}
