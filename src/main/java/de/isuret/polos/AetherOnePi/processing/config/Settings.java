package de.isuret.polos.AetherOnePi.processing.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Settings {

    private String name;
    private Map<String,Boolean> booleans = new HashMap<>();
    private Map<String,Integer> integers = new HashMap<>();
    private Map<String,String> strings = new HashMap<>();

    public Boolean getBoolean(String key, Boolean defaultValue) {

        Boolean value = booleans.get(key);

        if (value != null) {
            return value;
        }

        booleans.put(key, defaultValue);

        return defaultValue;
    }

    public Integer getInteger(String key, Integer defaultValue) {

        Integer value = integers.get(key);

        if (value != null) {
            return value;
        }

        integers.put(key, defaultValue);

        return defaultValue;
    }

    public String getString(String key, String defaultValue) {

        String value = strings.get(key);

        if (value != null) {
            return value;
        }

        strings.put(key, defaultValue);

        return defaultValue;
    }
}
