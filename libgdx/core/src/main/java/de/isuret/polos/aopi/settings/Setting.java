package de.isuret.polos.aopi.settings;

/** Stable configuration keys and defaults observed in the original AOPi Settings page. */
public enum Setting {

    BROADCAST_DELTA_TIME("Broadcasting", "broadcast.delta.time", "Use delta time", false,
            "Adjust broadcast duration using the difference in general vitality."),
    BROADCAST_SINGLE_RATES("Broadcasting", "broadcast.single.rates.only", "One rate at a time", false,
            "Process individual rates sequentially instead of broadcasting several together."),
    BROADCAST_EMBEDDED("Broadcasting", "broadcast.embedded", "Embedded broadcast view", true,
            "Show broadcasting inside the application instead of a separate window."),
    BROADCAST_IMAGES("Broadcasting", "broadcast.image.layers", "Broadcast image layers", false,
            "Include the selected image layers when broadcasting."),
    DYNAMIC_ADJUSTMENTS("Broadcasting", "dynamic.adjustments", "Dynamic adjustments", false,
            "Recheck general vitality and adjust the broadcast queue."),
    POWER_SWITCH("Broadcasting", "power.switch", "Power switch", false,
            "Enable the original power-switch broadcast mode."),
    SAFETY_SWITCH("Broadcasting", "safety.switch", "Safety switch", true,
            "Enable the original broadcast safety-switch mode."),
    NOTIFICATIONS("Broadcasting", "notification.after.broadcast", "Completion notification", false,
            "Show a notification after a broadcast finishes."),
    HIGH_MAX_HIT("Analysis", "analysis.very.high.max.hit", "Higher analysis hit limit", false,
            "Use 1000 instead of 100 as the analysis hit limit."),
    WATCHLIST("Analysis", "watchlist.activate", "Activate watchlist", false,
            "Run watchlist analysis when the watchlist service is available."),
    BINAURAL_SOUND("Sound", "play.binaural.sound", "Binaural sound", false,
            "Play binaural sound during broadcast resonance."),
    ADVANCED_SOUND("Sound", "play.sound.advanced", "Advanced sound", false,
            "Enable the original advanced broadcast sound mode."),
    INFINITE_NOISE("Hardware", "trng.infinity", "Infinite Noise TRNG", false,
            "Allow manual Infinite Noise USB acquisition on the Hotbits tab."),
    ESP32("Hardware", "trng.esp32", "ESP32 TRNG", false,
            "Allow manual ESP32 serial acquisition on the Hotbits tab."),
    OVERLAY("Appearance", "use.overlay", "Broadcast overlay", true,
            "Enable the original broadcast visualization overlay.");

    private final String group;
    private final String key;
    private final String title;
    private final boolean defaultValue;
    private final String description;

    Setting(String group, String key, String title, boolean defaultValue, String description) {
        this.group = group;
        this.key = key;
        this.title = title;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String group() {
        return group;
    }

    public String key() {
        return key;
    }

    public String title() {
        return title;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }
}
