package de.isuret.polos.AetherOnePi.domain;

import java.util.HashMap;
import java.util.Map;

public class Settings {

    private String name;
    private Map<String,Boolean> booleans = new HashMap<>();
    private Map<String,Integer> integers = new HashMap<>();
    private Map<String,String> strings = new HashMap<>();

    public Settings() {}

    public Settings(String name, Map<String, Boolean> booleans, Map<String, Integer> integers, Map<String, String> strings) {
        this.name = name;
        this.booleans = booleans;
        this.integers = integers;
        this.strings = strings;
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {

        Boolean value = booleans.get(key);

        if (value != null) {
            return value;
        }

        booleans.put(key, defaultValue);

        return defaultValue;
    }

    public void setBoolean(String key, Boolean value) {
        booleans.put(key, value);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Boolean> getBooleans() {
        return booleans;
    }

    public void setBooleans(Map<String, Boolean> booleans) {
        this.booleans = booleans;
    }

    public Map<String, Integer> getIntegers() {
        return integers;
    }

    public void setIntegers(Map<String, Integer> integers) {
        this.integers = integers;
    }

    public Map<String, String> getStrings() {
        return strings;
    }

    public void setStrings(Map<String, String> strings) {
        this.strings = strings;
    }
}
