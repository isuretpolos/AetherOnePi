package de.isuret.polos.aopi.image;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Scaled preview: mouse coordinates always map back to original image pixels without changing canvas resolution. */
public class SigilCanvas extends Widget implements com.badlogic.gdx.utils.Disposable {
    private final SigilMaker editor;
    private final Drawable pixel;
    private Texture texture;
    private long renderedRevision = -1;
    private boolean enabled = true;
    private final SigilPlacement placement;
    private long placementRevision = -1;
    private boolean resizing;
    private int dragX;
    private int dragY;
    private int dragWidth;

    public SigilCanvas(SigilMaker editor, Skin skin) {
        this(editor, skin, null);
    }

    public SigilCanvas(SigilMaker editor, Skin skin, SigilPlacement placement) {
        this.editor = editor;
        this.placement = placement;
        pixel = skin.getDrawable("white");
        setName("sigil-canvas");
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!enabled || button != Input.Buttons.LEFT || !imageBounds().contains(x, y)) {
                    return false;
                }
                if (placement != null) {
                    int pointerX = imageX(x);
                    int pointerY = imageY(y);
                    float tolerance = 12 * editor.collection().getLayers().getFirst().getImage().getWidth() / imageBounds().width;
                    resizing = Math.abs(pointerX - placement.x() - placement.width()) <= Math.min(tolerance, placement.width() / 3f)
                            && Math.abs(pointerY - placement.y() - placement.height()) <= Math.min(tolerance, placement.height() / 3f);
                    if (!resizing && !placement.contains(pointerX, pointerY)) { return false; }
                    dragX = resizing ? pointerX : pointerX - placement.x();
                    dragY = pointerY - placement.y();
                    dragWidth = placement.width();
                    return true;
                }
                return editor.beginGesture(imageX(x), imageY(y));
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (enabled) {
                    if (placement == null) {
                        editor.dragGesture(imageX(x), imageY(y));
                    } else if (resizing) {
                        placement.resize(dragWidth + imageX(x) - dragX);
                    } else {
                        placement.move(imageX(x) - dragX, imageY(y) - dragY);
                    }
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (placement == null) { editor.endGesture(); }
            }
        });
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Rectangle imageBounds() {
        var base = editor.collection().getLayers().getFirst().getImage();
        float scale = Math.min(getWidth() / base.getWidth(), getHeight() / base.getHeight());
        float width = base.getWidth() * scale;
        float height = base.getHeight() * scale;
        return new Rectangle((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
    }

    private int imageX(float x) {
        var bounds = imageBounds();
        return Math.round((x - bounds.x) * editor.collection().getLayers().getFirst().getImage().getWidth() / bounds.width);
    }

    private int imageY(float y) {
        var bounds = imageBounds();
        return Math.round((bounds.y + bounds.height - y) * editor.collection().getLayers().getFirst().getImage().getHeight() / bounds.height);
    }

    @Override
    public float getPrefWidth() { return 720; }

    @Override
    public float getPrefHeight() { return 405; }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (renderedRevision != editor.revision() || (placement != null && placementRevision != placement.revision())) {
            var preview = editor.preview(placement);
            if (placement != null) {
                placementRevision = placement.revision();
            }
            Pixmap image = new Pixmap(preview.getWidth(), preview.getHeight(), Pixmap.Format.RGBA8888);
            var bytes = image.getPixels();
            int[] argb = preview.getRGB(0, 0, preview.getWidth(), preview.getHeight(), null, 0, preview.getWidth());
            for (int value : argb) {
                bytes.put((byte) (value >>> 16)).put((byte) (value >>> 8)).put((byte) value).put((byte) (value >>> 24));
            }
            bytes.flip();
            if (texture == null || texture.getWidth() != image.getWidth() || texture.getHeight() != image.getHeight()) {
                if (texture != null) { texture.dispose(); }
                texture = new Texture(image);
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } else {
                texture.draw(image, 0, 0);
            }
            image.dispose();
            renderedRevision = editor.revision();
        }
        Rectangle bounds = imageBounds();
        Color previous = new Color(batch.getColor());
        float size = bounds.width / editor.collection().getLayers().getFirst().getImage().getWidth() * 20;
        if (size > 0) {
            for (float y = 0; y < bounds.height; y += size) {
                for (float x = 0; x < bounds.width; x += size) {
                    float gray = (((int) (x / size) + (int) (y / size)) & 1) == 0 ? 0.96f : 0.86f;
                    batch.setColor(gray, gray, gray, parentAlpha);
                    pixel.draw(batch, getX() + bounds.x + x, getY() + bounds.y + y,
                            Math.min(size, bounds.width - x), Math.min(size, bounds.height - y));
                }
            }
        }
        batch.setColor(1, 1, 1, parentAlpha);
        batch.draw(texture, getX() + bounds.x, getY() + bounds.y, bounds.width, bounds.height);
        if (placement != null) {
            float scale = bounds.width / editor.collection().getLayers().getFirst().getImage().getWidth();
            float left = getX() + bounds.x + placement.x() * scale;
            float bottom = getY() + bounds.y + bounds.height - (placement.y() + placement.height()) * scale;
            float width = placement.width() * scale;
            float height = placement.height() * scale;
            batch.setColor(0.3f, 1, 0.4f, parentAlpha);
            pixel.draw(batch, left, bottom, width, 2);
            pixel.draw(batch, left, bottom + height - 2, width, 2);
            pixel.draw(batch, left, bottom, 2, height);
            pixel.draw(batch, left + width - 2, bottom, 2, height);
            pixel.draw(batch, left + width - 8, bottom, 8, 8);
        }
        batch.setColor(previous);
    }

    @Override
    public void dispose() {
        if (texture != null) { texture.dispose(); }
    }
}
