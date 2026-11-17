package de.isuret.polos.aopi.settings;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** Settings content shares the application's stage and theme, so preview changes are global. */
public class SettingsPage extends Table {

    private final ApplicationSettings settings;
    private final Skin skin;
    private final Slider fontSize;
    private final Label fontSizeValue;
    private final Label saveStatus;
    private boolean dirty;
    private float saveDelay;

    public SettingsPage(ApplicationSettings settings, Skin skin) {
        this.settings = settings;
        this.skin = skin;
        top().left();
        defaults().growX().spaceBottom(12);

        add(new Label("SETTINGS", skin, "accent")).row();
        add(paragraph("Choose a comfortable text size. Changes appear immediately throughout the app."))
                .row();

        Table fontHeading = new Table();
        fontHeading.add(new Label("Font size", skin)).expandX().left();
        fontSizeValue = new Label(settings.getFontSize() + " px", skin);
        fontHeading.add(fontSizeValue).right();
        // Reserve enough height for the largest font, keeping the slider stable while dragging.
        add(fontHeading).height(48).row();
        fontSize = new Slider(ApplicationSettings.MIN_FONT_SIZE, ApplicationSettings.MAX_FONT_SIZE, 1, false, skin);
        fontSize.setValue(settings.getFontSize());
        fontSize.setName("font-size-slider");
        add(fontSize).height(48).row();

        fontSize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setFontSize(Math.round(fontSize.getValue()));
                fontSizeValue.setText(settings.getFontSize() + " px");
                changedSettings();
            }
        });
        addSliderKeyboardSupport();

        add(paragraph("Preview: AetherOnePi - The quick brown fox jumps over the lazy dog. 0123456789"))
                .padTop(8).row();
        TextButton reset = new TextButton("RESET FONT SIZE", skin);
        reset.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                fontSize.setValue(ApplicationSettings.DEFAULT_FONT_SIZE);
            }
        });
        add(reset).growX().left().padBottom(16).row();
        saveStatus = paragraph("Settings are saved automatically after changes.");
        add(saveStatus).row();
        add(paragraph("The options below preserve the original AOPi settings. They are saved now; their effects "
                + "become available as analysis, broadcasting, sound, and hardware support are implemented."))
                .padBottom(16).row();

        String previousGroup = "";
        int rowIndex = 0;
        for (Setting setting : Setting.values()) {
            if (!setting.group().equals(previousGroup)) {
                add(new Label(setting.group().toUpperCase(java.util.Locale.ROOT), skin, "accent"))
                        .padTop(16).row();
                previousGroup = setting.group();
            }
            addSetting(setting, rowIndex++);
        }
    }

    private void addSetting(Setting setting, int rowIndex) {
        Table row = new Table();
        // Shade the entire setting so its control, explanation, and key read as one row.
        String backgroundColor = rowIndex % 2 == 0 ? "292238" : "211C2D";
        row.setBackground(skin.newDrawable("white", Color.valueOf(backgroundColor)));
        row.pad(12, 14, 12, 14);
        Table description = new Table();
        description.add(paragraph(setting.title())).growX().row();
        Label detail = new Label(setting.description() + "\n" + setting.key(), skin, "muted");
        detail.setWrap(true);
        description.add(detail).growX().padTop(4);
        TextButton toggle = new TextButton(settings.isEnabled(setting) ? "ON" : "OFF", skin, "toggle");
        toggle.setChecked(settings.isEnabled(setting));
        toggle.pad(10);
        toggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setEnabled(setting, toggle.isChecked());
                toggle.setText(toggle.isChecked() ? "ON" : "OFF");
                changedSettings();
            }
        });
        row.add(toggle).minWidth(82).top().padRight(18);
        row.add(description).growX().top();
        add(row).spaceBottom(4).row();
    }

    private void addSliderKeyboardSupport() {
        fontSize.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                getStage().setKeyboardFocus(fontSize);
                return false;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                switch (keycode) {
                    case Input.Keys.LEFT, Input.Keys.DOWN -> fontSize.setValue(fontSize.getValue() - 1);
                    case Input.Keys.RIGHT, Input.Keys.UP -> fontSize.setValue(fontSize.getValue() + 1);
                    case Input.Keys.HOME -> fontSize.setValue(ApplicationSettings.MIN_FONT_SIZE);
                    case Input.Keys.END -> fontSize.setValue(ApplicationSettings.MAX_FONT_SIZE);
                    default -> { return false; }
                }
                return true;
            }
        });
    }

    private void changedSettings() {
        dirty = true;
        saveDelay = 0.4f;
        saveStatus.setText("Preview applied. Saving changes...");
    }

    public void update(float delta, SettingsRepository repository) {
        if (!dirty || fontSize.isDragging()) {
            return;
        }
        saveDelay -= delta;
        if (saveDelay <= 0) {
            save(repository);
        }
    }

    public void save(SettingsRepository repository) {
        if (!dirty) {
            return;
        }
        try {
            repository.save(settings);
            dirty = false;
            saveStatus.setText("Settings saved.");
        } catch (Exception exception) {
            dirty = false;
            saveStatus.setText("Settings could not be saved. Your preview is active; change a setting to retry.");
            com.badlogic.gdx.Gdx.app.error("Settings", "Cannot save settings.", exception);
        }
    }

    public void showLoadError() {
        saveStatus.setText("Saved settings could not be read. Defaults are shown; settings.json will be replaced when the app closes.");
    }

    private Label paragraph(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        return label;
    }
}
