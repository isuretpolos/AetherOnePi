package de.isuret.polos.aopi.broadcast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Displays actual BroadcastElement paint output in independently accumulated 320 x 180 surfaces. */
public class BroadcastVisual extends Widget implements Disposable {

    private final BroadcastService service;
    private final BroadcastContext context;
    private final Skin skin;
    private final Map<String, Surface> surfaces = new HashMap<>();
    private int previousCount;
    private int resonancePointer;

    private class Surface implements Disposable {
        private final FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 320, 180, false);
        private final BroadcastElement element;
        private double renderedSeconds;

        private Surface(BroadcastService.Unit unit) {
            context.setNowMillis(unit.started().toEpochMilli());
            element = new BroadcastElement(context, "BROADCAST", unit.request().seconds(),
                    unit.request().signature(), unit.request().intensity());
            element.setOffsetX(0);
            element.setOffsetY(0);
            element.setStart(unit.started().toEpochMilli());
            buffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            buffer.begin();
            Gdx.gl.glClearColor(0.03f, 0.02f, 0.06f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            buffer.end();
        }

        @Override
        public void dispose() {
            element.dispose();
            buffer.dispose();
        }
    }

    public BroadcastVisual(BroadcastService service, BroadcastContext context, Skin skin) {
        this.service = service;
        this.context = context;
        this.skin = skin;
        setName("broadcast-visual");
    }

    /** Called before Stage.draw, including while the Broadcast tab is detached. */
    public void update() {
        var ids = new HashSet<String>();
        for (var unit : service.active()) {
            ids.add(unit.id());
            Surface surface = surfaces.computeIfAbsent(unit.id(), ignored -> new Surface(unit));
            double elapsed = service.elapsedSeconds(unit);
            surface.buffer.begin();
            try {
                // Original Processing runs at 60 fps. Limit catch-up after a suspended desktop.
                int frames = Math.min(4, Math.max(1, (int) ((elapsed - surface.renderedSeconds) * 60)));
                for (int frame = 0; frame < frames; frame++) {
                    context.setNowMillis(unit.started().toEpochMilli() + (long) (elapsed * 1000));
                    surface.element.draw();
                }
                surface.renderedSeconds = elapsed;
            } finally {
                surface.buffer.end();
            }
        }
        for (String id : new HashSet<>(surfaces.keySet())) {
            if (!ids.contains(id)) {
                surfaces.remove(id).dispose();
            }
        }
        var events = context.getResonanceList();
        while (resonancePointer < events.size()) {
            service.resonate(events.get(resonancePointer++).getRateObject().getNameOrRate());
        }
        if (previousCount != service.active().size()) {
            previousCount = service.active().size();
            invalidateHierarchy();
        }
    }

    private int columns() {
        return Math.max(1, Math.min(4, (int) (getWidth() / 320)));
    }

    private float captionHeight() {
        return skin.getFont("default-font").getLineHeight() + 6;
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        invalidateHierarchy();
    }

    @Override
    public float getPrefHeight() {
        int rows = Math.max(1, (service.active().size() + columns() - 1) / columns());
        return rows * (Math.max(180, getWidth() / columns() * 180 / 320) + captionHeight());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        var font = skin.getFont("default-font");
        var oldFontColor = new com.badlogic.gdx.graphics.Color(font.getColor());
        var oldBatchColor = new com.badlogic.gdx.graphics.Color(batch.getColor());
        float tileWidth = getWidth() / columns();
        float imageHeight = tileWidth * 180 / 320;
        float rowHeight = imageHeight + captionHeight();
        font.setColor(0.85f, 0.8f, 0.95f, parentAlpha);
        batch.setColor(1, 1, 1, parentAlpha);
        int index = 0;
        for (var unit : service.active()) {
            Surface surface = surfaces.get(unit.id());
            if (surface == null) {
                continue;
            }
            float x = getX() + index % columns() * tileWidth;
            float top = getY() + getHeight() - index / columns() * rowHeight;
            int remaining = (int) Math.ceil(Math.max(0, unit.request().seconds() - service.elapsedSeconds(unit)));
            String title = unit.request().signature().replace('\n', ' ');
            if (title.length() > 24) {
                title = title.substring(0, 21) + "...";
            }
            String caption = title + " | " + remaining + " s";
            font.draw(batch, caption, x + 3, top - 2, 0, caption.length(),
                    tileWidth - 8, com.badlogic.gdx.utils.Align.left, false, "...");
            // This Batch overload takes the lower-left UV first. Framebuffers
            // have their bottom row at v=0; reversing it turns all artwork upside down.
            batch.draw(surface.buffer.getColorBufferTexture(), x, top - captionHeight() - imageHeight,
                    tileWidth - 3, imageHeight, 0, 0, 1, 1);
            index++;
        }
        font.setColor(oldFontColor);
        batch.setColor(oldBatchColor);
    }

    public BroadcastContext context() {
        return context;
    }

    @Override
    public void dispose() {
        surfaces.values().forEach(Surface::dispose);
        surfaces.clear();
        context.dispose();
    }
}
