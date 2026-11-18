package de.isuret.polos.aopi;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.SettingsRepository;
import de.isuret.polos.aopi.ui.AopiScreen;
import de.isuret.polos.aopi.ui.WindowModeController;
import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.analysis.RateFilePicker;

/**
 * Entry point for the new, independent AOPi application.
 * Desktop window configuration belongs to the launcher, not the application.
 */
public class AopiApplication extends Game {

    private final ApplicationSettings settings;
    private final SettingsRepository repository;
    private final WindowModeController windowMode;
    private final boolean settingsLoadFailed;
    private final HotbitsBackend hotbitsBackend;
    private final RateFilePicker rateFilePicker;
    private final de.isuret.polos.aopi.broadcast.BroadcastImagePicker broadcastImagePicker;

    public AopiApplication(ApplicationSettings settings, SettingsRepository repository,
                          WindowModeController windowMode, boolean settingsLoadFailed) {
        this(settings, repository, windowMode, settingsLoadFailed, null);
    }

    public AopiApplication(ApplicationSettings settings, SettingsRepository repository,
                          WindowModeController windowMode, boolean settingsLoadFailed, HotbitsBackend hotbitsBackend) {
        this(settings, repository, windowMode, settingsLoadFailed, hotbitsBackend, null);
    }

    public AopiApplication(ApplicationSettings settings, SettingsRepository repository,
                          WindowModeController windowMode, boolean settingsLoadFailed, HotbitsBackend hotbitsBackend,
                          RateFilePicker rateFilePicker) {
        this(settings, repository, windowMode, settingsLoadFailed, hotbitsBackend, rateFilePicker, null);
    }

    public AopiApplication(ApplicationSettings settings, SettingsRepository repository,
                          WindowModeController windowMode, boolean settingsLoadFailed, HotbitsBackend hotbitsBackend,
                          RateFilePicker rateFilePicker, de.isuret.polos.aopi.broadcast.BroadcastImagePicker broadcastImagePicker) {
        this.settings = settings;
        this.repository = repository;
        this.windowMode = windowMode;
        this.settingsLoadFailed = settingsLoadFailed;
        this.hotbitsBackend = hotbitsBackend;
        this.rateFilePicker = rateFilePicker;
        this.broadcastImagePicker = broadcastImagePicker;
    }

    @Override
    public void create() {
        setScreen(new AopiScreen(settings, repository, windowMode, settingsLoadFailed, hotbitsBackend, rateFilePicker, broadcastImagePicker));
    }

    @Override
    public void render() {
        windowMode.capture();
        super.render();
    }

    @Override
    public void dispose() {
        windowMode.capture();
        // Game does not dispose its screens automatically.
        if (getScreen() != null) {
            getScreen().dispose();
        }
        try {
            // Share the Settings page's model so shutdown retains the latest font and boolean edits.
            repository.save(settings);
        } catch (Exception exception) {
            Gdx.app.error("Settings", "Cannot save the final window position and size.", exception);
        }
    }
}
