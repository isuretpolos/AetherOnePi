package de.isuret.polos.AetherOnePi.utils;

import de.isuret.polos.AetherOnePi.domain.RateObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class RateMetadataFrequency {

    private final Map<String, Integer> counts = new HashMap<>();
    private final int minimum;
    private final int maximum;

    public RateMetadataFrequency(List<RateObject> rates, Function<RateObject, String> field) {
        for (RateObject rate : rates) {
            String key = normalize(field.apply(rate));
            if (!key.isEmpty()) {
                counts.merge(key, 1, Integer::sum);
            }
        }
        minimum = counts.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        maximum = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public int getCount(String value) {
        return counts.getOrDefault(normalize(value), 0);
    }

    public int getBrightness(String value) {
        int count = getCount(value);
        if (count == 0) {
            return 100;
        }
        if (maximum == minimum) {
            return 255;
        }
        return 100 + Math.round(155f * (count - minimum) / (maximum - minimum));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
