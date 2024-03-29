package de.isuret.polos.AetherOnePi.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.isuret.polos.AetherOnePi.domain.Settings;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for loading and saving configurations and settings
 */
public class AetherOnePiProcessingConfiguration {

    public static final String SETTINGS = "Settings";
    public static final String ENVIRONMENT = "Environment";
    public static final String GUI = "Gui";
    private static String configPath = "config/";

    private AetherOnePiProcessingConfiguration() {
    }

    private static Map<String, Settings> settingsMap = new HashMap<>();

    /**
     * For testing purposes you can set another path (like target)
     *
     * @param newPath
     */
    public static void setConfigPath(String newPath) {
        configPath = newPath;
    }

    /**
     * Reload all settings from json files
     */
    public static void refresh() {
        for (String name : settingsMap.keySet()) {
            loadSettings(name);
        }
    }

    /**
     * Save all settings
     */
    public static void saveAllSettings() {

        for (String name : settingsMap.keySet()) {
            System.out.println(name);
            saveSettings(settingsMap.get(name));
        }
    }

    /**
     * Save specific settings
     * @param settings
     */
    public static void saveSettings(Settings settings) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

        try {
            File configFile = new File(configPath + settings.getName() + ".json");
            System.out.println(configFile.getAbsolutePath());

            if (makeFileIfNotExist(configFile)) return;

            writer.writeValue(configFile, settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean makeFileIfNotExist(File configFile) {
        if (configFile.getParentFile().exists() || configFile.getParentFile().mkdirs()){
            try
            {
                configFile.createNewFile();
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
                return true;
            }
        }
        return false;
    }

    /**
     * Load a settings
     * @param name
     * @param reload ... means that it will try to load it from harddisk if it exist
     * @return
     */
    public static Settings loadSettings(String name, boolean reload) {

        // return the existing map of configs if it exists
        if (!reload && settingsMap.get(name) != null) {
            return settingsMap.get(name);
        }

        // else create one
        ObjectMapper mapper = new ObjectMapper();

        Settings settings = null;
        try {
            settings = mapper.readValue(new File(configPath + name + ".json"), Settings.class);
        } catch (IOException e) {
            // everything is fine, this just means that the config file is created for the first time
        }

        if (settings == null) {
            settings = new Settings();
            settings.setName(name);
        }

        settingsMap.put(name,settings);

        return settings;
    }

    /**
     * Load a settings
     * @param name
     * @return
     */
    public static Settings loadSettings(String name) {

        return loadSettings(name, false);
    }
}
