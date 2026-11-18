package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** The exact original AOPi icon, bundled independently with this application. */
public final class DesktopIcons {

    public static final String APPLICATION_ICON = "icons/aetherOnePi.png";

    private DesktopIcons() {
    }

    public static void configure(Lwjgl3ApplicationConfiguration configuration) {
        configuration.setWindowIcon(APPLICATION_ICON);
    }

    public static BufferedImage load() throws IOException {
        try (var input = DesktopIcons.class.getResourceAsStream("/" + APPLICATION_ICON)) {
            if (input == null) {
                throw new IOException("Missing bundled AOPi application icon.");
            }
            return ImageIO.read(input);
        }
    }
}
