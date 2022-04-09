package de.isuret.polos.AetherOnePi.processing.config;


import de.isuret.polos.AetherOnePi.domain.Settings;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

public class AetherOnePiProcessingConfigurationTest {

    @BeforeAll
    public static void init() {
        File targetFolder = new File("target/");
        if (!targetFolder.exists()) {
            targetFolder.mkdir();
        }
        AetherOnePiProcessingConfiguration.setConfigPath("target/");
    }

    @Test
    public void testMissingConfigs() {
        // This settings don't exist, because I read one from a UUID random identifier
        Settings settings = AetherOnePiProcessingConfiguration.loadSettings(UUID.randomUUID().toString());

        System.out.println(settings);

        Boolean testValue = settings.getBoolean("test", true);
        Assertions.assertTrue(testValue);

        AetherOnePiProcessingConfiguration.saveAllSettings();
    }

}
