package de.isuret.polos.aopi.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;

/** Renders fresh glyphs at the chosen size instead of enlarging a small bitmap. */
public class UiFontManager {

    private final FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSans.ttf"));
    private int currentSize;
    private float currentDensity;

    public boolean update(Skin skin, int size, Actor... roots) {
        float density = Math.max(1f, (float) Gdx.graphics.getBackBufferWidth() / Gdx.graphics.getWidth());
        if (size == currentSize && Math.abs(density - currentDensity) < 0.01f) {
            return false;
        }

        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Math.round(size * density);
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u2013\u2014\u2018\u2019\u201c\u201d";
        BitmapFont next = generator.generateFont(parameter);
        next.getData().setScale(1f / density);
        next.setUseIntegerPositions(false);
        BitmapFont previous = skin.getFont("default-font");

        skin.add("default-font", next);
        for (Label.LabelStyle style : skin.getAll(Label.LabelStyle.class).values()) {
            style.font = next;
        }
        for (TextButton.TextButtonStyle style : skin.getAll(TextButton.TextButtonStyle.class).values()) {
            style.font = next;
        }
        skin.get(Window.WindowStyle.class).titleFont = next;

        // Replace label caches but keep the actors themselves, including a slider being dragged.
        for (Actor root : roots) {
            refreshLabels(root, next);
        }
        previous.dispose();
        currentSize = size;
        currentDensity = density;
        return true;
    }

    private void refreshLabels(Actor actor, BitmapFont font) {
        if (actor instanceof Label label) {
            Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
            style.font = font;
            label.setStyle(style);
        }
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                refreshLabels(child, font);
            }
        }
        if (actor instanceof Layout layout) {
            layout.invalidateHierarchy();
        }
    }

    public void refresh(Actor root, Skin skin) {
        refreshLabels(root, skin.getFont("default-font"));
    }

    public void dispose() {
        generator.dispose();
    }
}
