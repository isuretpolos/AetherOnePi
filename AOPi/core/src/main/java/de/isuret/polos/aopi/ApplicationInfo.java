package de.isuret.polos.aopi;

import java.io.IOException;
import java.util.Properties;

/** Shared application identity; Gradle supplies the version when packaging resources. */
public final class ApplicationInfo {
    public static final String VERSION = loadVersion();
    public static final String WINDOW_TITLE = "AetherOnePi - AOPi - " + VERSION;

    private ApplicationInfo() {
    }

    public static String windowTitle(String caseName) {
        return caseName == null || caseName.isBlank() ? WINDOW_TITLE : WINDOW_TITLE + " - " + caseName;
    }

    private static String loadVersion() {
        Properties properties = new Properties();
        try (var input = ApplicationInfo.class.getResourceAsStream("/application.properties")) {
            if (input == null) { throw new IllegalStateException("Application version resource is missing."); }
            properties.load(input);
            return properties.getProperty("version");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read the application version.", exception);
        }
    }
}
