package de.isuret.polos.aopi.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import de.isuret.polos.aopi.settings.ApplicationSettings;

/** Translucent panel fill without a border or an opaque backing beneath it. */
public class PanelBackground extends BaseDrawable {

    private final Drawable pixel;
    private final ApplicationSettings settings;
    private final Color fill = new Color();
    private String previousColor;

    public PanelBackground(Drawable pixel, ApplicationSettings settings) {
        this.pixel = pixel;
        this.settings = settings;
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        if (!settings.getPanelColor().equals(previousColor)) {
            previousColor = settings.getPanelColor();
            fill.set(Color.valueOf(previousColor));
        }
        Color previous = new Color(batch.getColor());
        float opacity = 1 - settings.getPanelTransparency() / 100f;
        batch.setColor(fill.r, fill.g, fill.b, previous.a * opacity);
        pixel.draw(batch, x, y, width, height);
        batch.setColor(previous);
    }
}
