package de.isuret.polos.AetherOnePi.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public class SearchResult {

    Map<String, String> values = new LinkedHashMap<>();

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}
