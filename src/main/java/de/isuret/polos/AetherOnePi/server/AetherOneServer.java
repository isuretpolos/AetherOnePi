package de.isuret.polos.AetherOnePi.server;

import de.isuret.polos.AetherOnePi.domain.Settings;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class AetherOneServer {

    /**
     * For tests only
     * @param args
     */
    public static void main(String [] args) {
        new AetherOneServer();
    }

    /**
     * Main Server application which runs parallel to AetherOnePi Processing App
     */
    public AetherOneServer() {
        // init
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("html", Location.EXTERNAL);
        }).start(7070);

        app.get("/settings", ctx -> {
            Settings settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);
            ctx.json(settings);
        });
    }
}
