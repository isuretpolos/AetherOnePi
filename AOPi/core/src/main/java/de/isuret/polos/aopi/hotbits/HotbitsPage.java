package de.isuret.polos.aopi.hotbits;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.Setting;

import java.util.ArrayList;
import java.util.List;

/** Responsive controls; device work and package writing continue when this page is hidden. */
public class HotbitsPage extends Table {

    private final HotbitsAcquisition acquisition;
    private final ApplicationSettings settings;
    private final Skin skin;
    private final Label status;
    private final Label counts;
    private final Table devices = new Table();
    private final Image previewImage = new Image();
    private final Table previewContainer = new Table();
    private final TextButton webcam;
    private final TextButton esp32;
    private final TextButton infiniteNoise;
    private final TextButton scan;
    private final TextButton preview;
    private final TextButton start;
    private final TextButton stop;
    private final List<TextButton> deviceButtons = new ArrayList<>();
    private HotbitsBackend.Kind kind = HotbitsBackend.Kind.WEBCAM;
    private HotbitsBackend.Device selected;
    private List<HotbitsBackend.Device> displayedDevices = List.of();
    private byte[] displayedPreview;
    private Texture previewTexture;

    public HotbitsPage(HotbitsAcquisition acquisition, ApplicationSettings settings, Skin skin) {
        this.acquisition = acquisition;
        this.settings = settings;
        this.skin = skin;
        top().left();
        defaults().growX().spaceBottom(14);
        add(new Label("HOTBITS", skin, "accent")).row();
        add(paragraph("1. Choose a source and scan.  2. Select a device.  3. Start acquisition.\n"
                + "Acquisition continues in the background when you switch tabs. STOP releases the device."))
                .row();

        webcam = button("WEBCAM", () -> selectKind(HotbitsBackend.Kind.WEBCAM));
        esp32 = button("ESP32 SERIAL", () -> selectKind(HotbitsBackend.Kind.ESP32));
        infiniteNoise = button("INFINITE NOISE USB", () -> selectKind(HotbitsBackend.Kind.INFINITE_NOISE));
        scan = button("SCAN DEVICES", () -> {
            clearPreview();
            acquisition.scan(kind);
        });
        preview = button("PREVIEW CAMERA", () -> acquisition.preview(selected));
        start = button("START ACQUISITION", this::start);
        stop = button("STOP", acquisition::stop);
        webcam.setStyle(skin.get("toggle", TextButton.TextButtonStyle.class));
        esp32.setStyle(skin.get("toggle", TextButton.TextButtonStyle.class));
        infiniteNoise.setStyle(skin.get("toggle", TextButton.TextButtonStyle.class));
        webcam.setProgrammaticChangeEvents(false);
        esp32.setProgrammaticChangeEvents(false);
        infiniteNoise.setProgrammaticChangeEvents(false);
        webcam.setName("hotbits-webcam");
        esp32.setName("hotbits-esp32");
        infiniteNoise.setName("hotbits-infinite-noise");
        scan.setName("hotbits-scan");
        preview.setName("hotbits-preview");
        start.setName("hotbits-start");
        stop.setName("hotbits-stop");
        HorizontalGroup sources = controlRow();
        sources.addActor(webcam);
        sources.addActor(esp32);
        sources.addActor(infiniteNoise);
        sources.addActor(scan);
        add(sources).row();
        add(devices).row();

        HorizontalGroup actions = controlRow();
        actions.addActor(preview);
        actions.addActor(start);
        actions.addActor(stop);
        add(actions).row();
        status = paragraph("");
        counts = paragraph("");
        add(status).row();
        add(counts).row();
        previewImage.setScaling(Scaling.fit);
        previewContainer.add(previewImage).growX().height(0);
        add(previewContainer).row();
        add(paragraph("Camera preview is a single local image and is cleared before acquisition. "
                + "Images are not saved or uploaded. The preview does not run during acquisition."))
                .row();
        add(paragraph("ESP32: enable ESP32 TRNG in Settings, select the correct port, and use firmware that streams "
                + "raw random bytes at 921600 baud (8N1). Ports are never opened just to guess which device is connected."))
                .row();
        add(paragraph("Infinite Noise USB: enable Infinite Noise TRNG in Settings, install the FTDI D2XX driver, "
                + "scan, and select your stick by its serial number. Close other applications using the stick."))
                .row();
        add(paragraph("Webcam noise is experimental, not a certified random source. Extraction and hashing cannot "
                + "guarantee entropy. No software-generated fallback is used."))
                .row();
        selectKind(kind);
        acquisition.refreshCache();
        update();
    }

    private void selectKind(HotbitsBackend.Kind kind) {
        this.kind = kind;
        webcam.setChecked(kind == HotbitsBackend.Kind.WEBCAM);
        esp32.setChecked(kind == HotbitsBackend.Kind.ESP32);
        infiniteNoise.setChecked(kind == HotbitsBackend.Kind.INFINITE_NOISE);
        selected = null;
        displayedDevices = List.of();
        devices.clearChildren();
        deviceButtons.clear();
        devices.add(paragraph("Click SCAN DEVICES to list available "
                + switch (kind) {
                    case WEBCAM -> "cameras.";
                    case ESP32 -> "serial ports.";
                    case INFINITE_NOISE -> "Infinite Noise USB sticks.";
                })).growX();
        clearPreview();
    }

    private void start() {
        clearPreview();
        acquisition.start(selected);
    }

    public void update() {
        HotbitsAcquisition.Snapshot snapshot = acquisition.snapshot();
        boolean busy = acquisition.isBusy();
        if (snapshot.devices() != displayedDevices && !snapshot.devices().isEmpty()
                && snapshot.devices().getFirst().kind() == kind) {
            displayedDevices = snapshot.devices();
            selected = null;
            buildDevices();
        } else if (snapshot.devices().isEmpty() && snapshot.state() == HotbitsAcquisition.State.IDLE
                && !displayedDevices.isEmpty()) {
            displayedDevices = List.of();
            selected = null;
            buildDevices();
        }
        if (snapshot.state() == HotbitsAcquisition.State.ACQUIRING && !isSourceEnabled()) {
            acquisition.stop();
        }
        webcam.setDisabled(busy);
        esp32.setDisabled(busy);
        infiniteNoise.setDisabled(busy);
        scan.setDisabled(busy);
        preview.setDisabled(busy || selected == null || kind != HotbitsBackend.Kind.WEBCAM);
        start.setDisabled(busy || selected == null || !isSourceEnabled());
        stop.setDisabled(!busy || snapshot.state() == HotbitsAcquisition.State.STOPPING);
        for (TextButton button : deviceButtons) {
            button.setDisabled(busy);
        }
        status.setText(snapshot.message());
        counts.setText("Saved packages: " + snapshot.cachedPackages() + " / " + HotbitsAcquisition.MAX_PACKAGES
                + "\nThis session: " + snapshot.sessionPackages() + " packages | Current package: "
                + snapshot.pendingIntegers() + " / " + HotbitsAcquisition.INTEGERS_PER_PACKAGE
                + " integers | Samples: " + snapshot.sampleCount());
        if (snapshot.previewPng() != null && snapshot.previewPng() != displayedPreview && !busy) {
            try {
                showPreview(snapshot.previewPng());
            } catch (RuntimeException exception) {
                displayedPreview = snapshot.previewPng();
                status.setText("Camera preview could not be displayed. You can scan again or start acquisition.");
            }
        }
    }

    private boolean isSourceEnabled() {
        return switch (kind) {
            case WEBCAM -> true;
            case ESP32 -> settings.isEnabled(Setting.ESP32);
            case INFINITE_NOISE -> settings.isEnabled(Setting.INFINITE_NOISE);
        };
    }

    private void buildDevices() {
        devices.clearChildren();
        deviceButtons.clear();
        if (displayedDevices.isEmpty()) {
            devices.add(paragraph("No devices found. Check the connection and scan again.")).growX();
        }
        for (HotbitsBackend.Device device : displayedDevices) {
            TextButton button = new TextButton(device.name(), skin, "toggle");
            button.setName("hotbits-device-" + device.id());
            button.getLabel().setWrap(true);
            button.getLabel().setAlignment(Align.left);
            button.pad(12);
            button.setProgrammaticChangeEvents(false);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selected = device;
                    clearPreview();
                    for (TextButton candidate : deviceButtons) {
                        candidate.setChecked(candidate == button);
                    }
                }
            });
            deviceButtons.add(button);
            devices.add(button).growX().padBottom(6).row();
        }
    }

    private void showPreview(byte[] bytes) {
        clearPreview();
        Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
        try {
            previewTexture = new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }
        displayedPreview = bytes;
        previewImage.setDrawable(new TextureRegionDrawable(previewTexture));
        previewContainer.getCell(previewImage).height(240);
        previewContainer.invalidateHierarchy();
    }

    private void clearPreview() {
        previewImage.setDrawable(null);
        if (previewTexture != null) {
            previewTexture.dispose();
            previewTexture = null;
        }
        previewContainer.getCell(previewImage).height(0);
        previewContainer.invalidateHierarchy();
    }

    public void dispose() {
        clearPreview();
    }

    private TextButton button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.pad(10, 12, 10, 12);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private Label paragraph(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        return label;
    }

    private HorizontalGroup controlRow() {
        return new HorizontalGroup().wrap().left().rowLeft().space(8).wrapSpace(8);
    }
}
