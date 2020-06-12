package de.isuret.polos.AetherOnePi.processing.config;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

public class AetherOnePiProcessingConfigurationTest {

    @BeforeClass
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
        Assert.assertTrue(testValue);

        AetherOnePiProcessingConfiguration.saveAllSettings();
    }

}
