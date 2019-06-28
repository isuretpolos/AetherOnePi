package de.isuret.polos.AetherOnePi.processing.utils;

import processing.core.PApplet;

import java.io.File;

public class DataUtils {

    public static final String UTF_8 = "UTF-8";
    public static final String HOTBITS_TXT = "/hotbits.txt";

    private DataUtils() {
    }

    public static File getHomeDirectory() {

        String aetherOneHomeDirectory = System.getProperty("user.home") + "/AetherOne";
        File homeDir = new File(aetherOneHomeDirectory);

        if (!homeDir.exists()) {
            homeDir.mkdir();
        }

        return homeDir;
    }

    public static String getRatesDirectoryPath(PApplet p) {
        return p.sketchPath() + "/../../../../radionics-database";
    }
}
