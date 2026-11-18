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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
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
    private Slider panelTransparency;
    private TextField panelColor;
    private Label colorValidation;
    private Label transparencyValue;

    public SettingsPage(ApplicationSettings settings, Skin skin) {
        this.settings = settings;
        this.skin = skin;
        top().left();
        defaults().growX().spaceBottom(12);


        Table fontHeading = new Table();
        fontHeading.add(new Label("Choose a comfortable text size. Changes appear immediately throughout the app.", skin)).expandX().left();
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
        addSliderKeyboardSupport(fontSize, ApplicationSettings.MIN_FONT_SIZE, ApplicationSettings.MAX_FONT_SIZE);

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
        addBackgroundControls();
        add(paragraph("ESP32 TRNG and Infinite Noise TRNG enable their sources on the Hotbits tab. The other original AOPi "
                + "options are saved for use as analysis, broadcasting, sound, and further hardware support are implemented."))
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

    private void addBackgroundControls() {
        add(new Label("PANEL BACKGROUND", skin, "accent")).padTop(8).row();
        HorizontalGroup colors = new HorizontalGroup().wrap().left().rowLeft().space(8).wrapSpace(8);
        colors.addActor(new Label("Color", skin));
        panelColor = new TextField("#" + settings.getPanelColor(), skin) {
            @Override
            public float getPrefWidth() {
                return Math.max(150, getStyle().font.getCapHeight() * 8 + 24);
            }
        };
        panelColor.setName("panel-color");
        panelColor.setMaxLength(7);
        panelColor.setProgrammaticChangeEvents(true);
        panelColor.setTextFieldFilter((field, character) -> character == '#' || Character.digit(character, 16) >= 0);
        colors.addActor(panelColor);
        TextButton choose = new TextButton("CHOOSE COLOR", skin);
        choose.setName("panel-color-choose");
        choose.pad(7, 10, 7, 10);
        choose.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                choosePanelColor();
            }
        });
        colors.addActor(choose);
        TextButton reset = new TextButton("RESET BACKGROUND", skin);
        reset.setName("panel-background-reset");
        reset.pad(7, 10, 7, 10);
        reset.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                panelColor.setText("#" + ApplicationSettings.DEFAULT_PANEL_COLOR);
                panelTransparency.setValue(0);
            }
        });
        colors.addActor(reset);
        add(colors).row();
        colorValidation = new Label("RGB hex color (#RRGGBB). Changes apply to every tab immediately.", skin, "muted");
        colorValidation.setWrap(true);
        add(colorValidation).row();
        panelColor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    settings.setPanelColor(panelColor.getText());
                    colorValidation.setText("RGB hex color (#RRGGBB). Changes apply to every tab immediately.");
                    changedSettings();
                } catch (IllegalArgumentException exception) {
                    colorValidation.setText("Enter six hex digits. The previous valid color remains active.");
                }
            }
        });
        transparencyValue = new Label("Transparency: " + settings.getPanelTransparency() + "%", skin);
        transparencyValue.setName("panel-transparency-value");
        add(transparencyValue).left().row();
        panelTransparency = new Slider(0, 100, 1, false, skin);
        panelTransparency.setName("panel-transparency");
        panelTransparency.setValue(settings.getPanelTransparency());
        addSliderKeyboardSupport(panelTransparency, 0, 100);
        panelTransparency.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setPanelTransparency(Math.round(panelTransparency.getValue()));
                transparencyValue.setText("Transparency: " + settings.getPanelTransparency() + "%");
                changedSettings();
            }
        });
        add(panelTransparency).height(40).row();
        add(paragraph("0%: solid color. 100%: fully transparent, showing the galaxy background."))
                .padBottom(8).row();
    }

    private void choosePanelColor() {
        Dialog dialog = new Dialog("Panel color — live preview", skin);
        dialog.setName("panel-color-dialog");
        Color initial = Color.valueOf(settings.getPanelColor());
        Slider[] components = new Slider[3];
        String[] names = {"Red", "Green", "Blue"};
        int[] values = {Math.round(initial.r * 255), Math.round(initial.g * 255), Math.round(initial.b * 255)};
        for (int index = 0; index < components.length; index++) {
            final int channel = index;
            Slider slider = new Slider(0, 255, 1, false, skin);
            components[index] = slider;
            slider.setName("panel-color-" + names[index].toLowerCase(java.util.Locale.ROOT));
            slider.setValue(values[index]);
            addSliderKeyboardSupport(slider, 0, 255);
            Label value = new Label(names[index] + ": " + values[index], skin);
            dialog.getContentTable().add(value).left().pad(8).row();
            dialog.getContentTable().add(slider).width(Math.min(480, getStage().getWidth() - 100)).height(40).pad(8).row();
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    values[channel] = Math.round(slider.getValue());
                    value.setText(names[channel] + ": " + values[channel]);
                    panelColor.setText(String.format(java.util.Locale.ROOT, "#%02X%02X%02X", values[0], values[1], values[2]));
                }
            });
        }
        TextButton done = new TextButton("DONE", skin);
        done.setName("panel-color-done");
        done.pad(8, 12, 8, 12);
        dialog.button(done);
        dialog.key(Input.Keys.ESCAPE, null);
        dialog.show(getStage());
    }

    private void addSliderKeyboardSupport(Slider slider, int minimum, int maximum) {
        slider.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                slider.getStage().setKeyboardFocus(slider);
                return false;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                switch (keycode) {
                    case Input.Keys.LEFT, Input.Keys.DOWN -> slider.setValue(slider.getValue() - 1);
                    case Input.Keys.RIGHT, Input.Keys.UP -> slider.setValue(slider.getValue() + 1);
                    case Input.Keys.HOME -> slider.setValue(minimum);
                    case Input.Keys.END -> slider.setValue(maximum);
                    default -> { return false; }
                }
                return true;
            }
        });
    }

    public void changedSettings() {
        dirty = true;
        saveDelay = 0.4f;
        saveStatus.setText("Preview applied. Saving changes...");
    }

    public void update(float delta, SettingsRepository repository) {
        if (!dirty || fontSize.isDragging() || panelTransparency.isDragging()) {
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
