package de.isuret.polos.aopi.analysis;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Pure analysis rules observed in AOPi, with cancellation, finite limits, and immutable output. */
public class AnalysisEngine {

    public static final int RESULT_LIMIT = 20;
    public static final int GV_RATE_LIMIT = RESULT_LIMIT;

    @FunctionalInterface
    public interface Draws {
        int nextInt(int bound) throws IOException;
    }

    private final Draws draws;
    private final Consumer<String> progress;

    public AnalysisEngine(Draws draws, Consumer<String> progress) {
        this.draws = draws;
        this.progress = progress;
    }

    public AnalysisResult analyze(String database, List<RateRepository.Rate> rates, boolean highLimit,
                                  AnalysisResult previous) throws IOException {
        if (rates.isEmpty()) {
            throw new IOException("Choose a nonempty rate list.");
        }
        List<RateRepository.Rate> shuffled = new ArrayList<>(rates);
        for (int index = shuffled.size() - 1; index > 0; index--) {
            int other = draw(index + 1);
            var value = shuffled.get(index);
            shuffled.set(index, shuffled.get(other));
            shuffled.set(other, value);
        }
        int count = Math.min(rates.size(), Math.max(120, Math.min(5000, rates.size() / 10)));
        List<RateRepository.Rate> candidates = shuffled.subList(0, count);
        int[] scores = new int[count];
        int limit = highLimit ? 1000 : 100;
        int trials = 0;
        boolean complete = false;
        while (!complete) {
            for (int index = 0; index < count; index++) {
                scores[index] += draw(11);
                trials++;
                if (scores[index] >= limit) {
                    complete = true;
                    break;
                }
                if (trials >= 20000000) {
                    throw new IOException("Analysis did not reach its hit limit. Check the hotbits source.");
                }
            }
            progress.accept("Scoring " + count + " candidates | Trials: " + trials);
        }
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            order.add(index);
        }
        order.sort(Comparator.<Integer>comparingInt(index -> scores[index]).reversed()
                .thenComparing(index -> candidates.get(index).name()));
        Map<String, Integer> recurrence = new HashMap<>();
        if (previous != null) {
            previous.rows().forEach(row -> recurrence.put(row.name(), row.recurring() + 1));
        }
        List<AnalysisResult.Row> rows = new ArrayList<>();
        for (int rank = 0; rank < Math.min(RESULT_LIMIT, order.size()); rank++) {
            int index = order.get(rank);
            RateRepository.Rate rate = candidates.get(index);
            progress.accept("Potency and level: " + (rank + 1) + " / " + Math.min(RESULT_LIMIT, order.size()));
            rows.add(new AnalysisResult.Row(rate.name(), rate.url(), rate.classification(), rate.subfamily(),
                    scores[index], potency(), level(), null, recurrence.getOrDefault(rate.name(), 0)));
        }
        return new AnalysisResult(UUID.randomUUID().toString(), Instant.now(), database, limit, trials, null, rows);
    }

    public AnalysisResult checkVitality(AnalysisResult result, boolean all) throws IOException {
        List<AnalysisResult.Row> rows = new ArrayList<>(result.rows());
        int checkedCount = Math.min(GV_RATE_LIMIT, rows.size());
        Integer target = result.generalVitality();
        int nextRate = nextVitalityRate(result);
        if (all) {
            target = vitality();
            for (int index = 0; index < checkedCount; index++) {
                progress.accept("Checking GV: " + (index + 1) + " / " + checkedCount);
                rows.set(index, rows.get(index).withVitality(vitality()));
            }
        } else if (nextRate < 0) {
            target = vitality();
            for (int index = 0; index < checkedCount; index++) {
                var row = rows.get(index);
                rows.set(index, new AnalysisResult.Row(row.name(), row.url(), row.classification(), row.subfamily(),
                        row.energeticValue(), row.potency(), row.level(), null, row.recurring()));
            }
        } else {
            rows.set(nextRate, rows.get(nextRate).withVitality(vitality()));
        }
        return new AnalysisResult(UUID.randomUUID().toString(), result.created(), result.database(), result.hitLimit(),
                result.trials(), target, rows);
    }

    /** -1 means the next check is the target GV, rather than a rate. Filters never alter this order. */
    public static int nextVitalityRate(AnalysisResult result) {
        if (result == null || result.generalVitality() == null) {
            return -1;
        }
        for (int index = 0; index < Math.min(GV_RATE_LIMIT, result.rows().size()); index++) {
            if (result.rows().get(index).generalVitality() == null) {
                return index;
            }
        }
        return -1;
    }

    private String potency() throws IOException {
        String[] types = {"D", "C", "LM", "Q", "FLUX"};
        int[] decimal = {0, 1, 3, 4, 6, 12, 30, 200};
        int[] centesimal = {0, 1, 3, 6, 12, 30, 200, 1000, 10000, 50000, 100000, 1000000};
        // The old hit race treats every candidate equally and never examines its potency.
        // Its winning candidate therefore has the same distribution as one fresh candidate.
        String type = types[draw(types.length)];
        int strength = switch (type) {
            case "D" -> decimal[draw(decimal.length)];
            case "C", "FLUX" -> centesimal[draw(centesimal.length)];
            default -> draw(30) + 1;
        };
        return type + " " + strength;
    }

    private int level() throws IOException {
        int[] scores = new int[12];
        for (int trial = 0; trial < 100; trial++) {
            int level = draw(12);
            if (draw(2) == 1) {
                scores[level]++;
            }
        }
        int best = 0;
        int maximum = 0;
        for (int index = 0; index < scores.length; index++) {
            if (scores[index] > maximum) {
                maximum = scores[index];
                best = index + 1;
            }
        }
        return best;
    }

    private int vitality() throws IOException {
        int value = Math.max(draw(1001), Math.max(draw(1001), draw(1001)));
        if (value > 950) {
            for (int trial = 0; trial < 256; trial++) {
                int dice = draw(101);
                if (dice < 50) {
                    return value;
                }
                value += dice;
            }
            throw new IOException("GV extension did not finish. Check the hotbits source.");
        }
        return value;
    }

    private int draw(int bound) throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Analysis cancelled.");
        }
        return draws.nextInt(bound);
    }
}
