package de.isuret.polos.AetherOnePi.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MateriaMedica {

    private String remedyName;
    private String remedyAlternativeNames;
    private Map<String, List<String>> categories = new LinkedHashMap<>();

    public String getRemedyName() {
        return remedyName;
    }

    public void setRemedyName(String remedyName) {
        this.remedyName = remedyName;
    }

    public String getRemedyAlternativeNames() {
        return remedyAlternativeNames;
    }

    public void setRemedyAlternativeNames(String remedyAlternativeNames) {
        this.remedyAlternativeNames = remedyAlternativeNames;
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, List<String>> categories) {
        this.categories = categories;
    }

    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();

        for (String category : categories.keySet()) {
            str.append(category).append(": ");
            for (String symptoms : categories.get(category)) {
                str.append(symptoms);
            }
            str.append("\n");
        }

        return "MateriaMedica{" +
                "remedyName='" + remedyName + '\'' +
                ", remedyAlternativeNames='" + remedyAlternativeNames + '\'' +
                ", categories=" + str +
                '}';
    }
}
