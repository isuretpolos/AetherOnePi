package de.isuret.polos.aopi.broadcast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import de.isuret.polos.aopi.session.CachedHotbits;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.Setting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntSupplier;

/** Processing drawing/API adapter. The original BroadcastElement owns the paint decisions. */
public class BroadcastContext implements Disposable {

    public static final int BLEND = 0;
    public static final int DIFFERENCE = 1;
    public static final int CORNER = 0;
    public final float HALF_PI = (float) Math.PI / 2;
    public final float PI = (float) Math.PI;
    public final float QUARTER_PI = (float) Math.PI / 4;
    public final float TWO_PI = (float) Math.PI * 2;
    public int[] pixels;
    private final Skin skin;
    private final ApplicationSettings settings;
    private final CachedHotbits hotbits;
    private final IntSupplier cacheSize;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Color fill = new Color(Color.WHITE);
    private final Color stroke = new Color(Color.WHITE);
    private boolean filled = true;
    private boolean stroked = true;
    private int fontSize = 12;
    private int blend = BLEND;
    private final List<Float> vertices = new ArrayList<>();
    private final List<BroadcastImage> clipboardImages = new ArrayList<>();
    private final List<BroadcastImage> layerImages = new ArrayList<>();
    private ImageLayersAnalysis imageLayers;
    private boolean separateImageLayerPass;
    private final List<RateObject> resonated = new ArrayList<>();
    private final List<ResonanceObject> resonances = new ArrayList<>();
    private final java.security.SecureRandom fallback = new java.security.SecureRandom();
    private long retryHotbits;
    private long now;
    private String broadcastId = "";
    private final ExecutorService audio = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "aopi-broadcast-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final ArrayDeque<Style> styles = new ArrayDeque<>();
    private record Style(Color fill, Color stroke, boolean filled, boolean stroked, int font, int blend) { }

    public BroadcastContext(Skin skin, ApplicationSettings settings, CachedHotbits hotbits, IntSupplier cacheSize) {
        this.skin = skin;
        this.settings = settings;
        this.hotbits = hotbits;
        this.cacheSize = cacheSize;
        Matrix4 projection = new Matrix4().setToOrtho2D(0, 0, 320, 180);
        batch.setProjectionMatrix(projection);
        shapes.setProjectionMatrix(projection);
    }

    public long nowMillis() { return now; }
    public String broadcastId() { return broadcastId; }
    public void setBroadcastId(String id) { broadcastId = id; }
    public void retainImageLayerBroadcasts(java.util.Set<String> activeIds) {
        if (imageLayers != null) { imageLayers.retainBroadcasts(activeIds); }
    }
    public void setNowMillis(long now) { this.now = now; }
    public int getHotbitsCacheSize() { return cacheSize.getAsInt(); }
    public BroadcastContext getSettings() { return this; }
    public boolean getBoolean(Setting setting, boolean ignored) { return settings.isEnabled(setting); }
    public BroadcastContext getHotbitsClient() { return this; }
    public int getInteger(int maximum) { return getInteger(0, maximum); }
    public int getInteger(int minimum, int maximum) {
        // BroadcastElement originally uses PRNGs and HotbitsHandler's SecureRandom fallback.
        // The shared reader also exposes SecureRandom fallback through the footer warning LED.
        if (hotbits != null && System.nanoTime() >= retryHotbits) {
            try {
                return hotbits.nextInt(maximum - minimum + 1) + minimum;
            } catch (java.io.IOException exception) {
                retryHotbits = System.nanoTime() + 1_000_000_000L;
            }
        }
        return fallback.nextInt(maximum - minimum + 1) + minimum;
    }
    public List<BroadcastImage> getClipBoardImages() { return clipboardImages; }
    public List<RateObject> getResonatedList() { return resonated; }
    public List<ResonanceObject> getResonanceList() { return resonances; }
    public ImageLayersAnalysis getImageLayers() {
        return !separateImageLayerPass && settings.isEnabled(Setting.BROADCAST_IMAGES) ? imageLayers : null;
    }
    public void setSeparateImageLayerPass(boolean separate) { separateImageLayerPass = separate; }
    public void playAudio(Runnable task) { audio.execute(task); }
    public void setImages(List<BroadcastImage> images, boolean layers) {
        List<BroadcastImage> destination = layers ? layerImages : clipboardImages;
        destination.forEach(BroadcastImage::dispose);
        destination.clear();
        destination.addAll(images);
        if (layers) { imageLayers = new ImageLayersAnalysis(destination); }
    }

    public void noFill() { filled = false; }
    public void noStroke() { stroked = false; }
    public void fill(int gray) { fill(gray, gray, gray, 255); }
    public void fill(int r, int g, int b) { fill(r, g, b, 255); }
    public void fill(int r, int g, int b, int a) { fill.set(r / 255f, g / 255f, b / 255f, a / 255f); filled = true; }
    public void stroke(int r, int g, int b) { stroke(r, g, b, 255); }
    public void stroke(int r, int g, int b, int a) { stroke.set(r / 255f, g / 255f, b / 255f, a / 255f); stroked = true; }
    private void begin(ShapeRenderer.ShapeType type, Color color) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(type);
        shapes.setColor(color);
    }
    public void point(float x, float y) {
        if (stroked) { begin(ShapeRenderer.ShapeType.Filled, stroke); shapes.rect(x, 179 - y, 1, 1); shapes.end(); }
    }
    public void rect(float x, float y, float w, float h) {
        if (filled) { begin(ShapeRenderer.ShapeType.Filled, fill); shapes.rect(x, 180 - y - h, w, h); shapes.end(); }
        if (stroked) { begin(ShapeRenderer.ShapeType.Line, stroke); shapes.rect(x, 180 - y - h, w, h); shapes.end(); }
    }
    public void ellipse(float x, float y, float w, float h) {
        w = Math.abs(w);
        h = Math.abs(h);
        if (w == 0 || h == 0) { return; }
        if (filled) { begin(ShapeRenderer.ShapeType.Filled, fill); shapes.ellipse(x - w / 2, 180 - y - h / 2, w, h, 64); shapes.end(); }
        if (stroked) { begin(ShapeRenderer.ShapeType.Line, stroke); shapes.ellipse(x - w / 2, 180 - y - h / 2, w, h, 64); shapes.end(); }
    }
    public void line(float x1, float y1, float x2, float y2) {
        if (stroked) { begin(ShapeRenderer.ShapeType.Line, stroke); shapes.line(x1, 180 - y1, x2, 180 - y2); shapes.end(); }
    }
    public void bezier(float x1, float y1, float cx1, float cy1, float cx2, float cy2, float x2, float y2) {
        if (stroked) { begin(ShapeRenderer.ShapeType.Line, stroke); shapes.curve(x1, 180 - y1, cx1, 180 - cy1, cx2, 180 - cy2, x2, 180 - y2, 40); shapes.end(); }
    }
    public void arc(float x, float y, float w, float h, float start, float end) {
        if (!stroked) { return; }
        begin(ShapeRenderer.ShapeType.Line, stroke);
        for (int index = 0; index < 40; index++) {
            double a = start + (end - start) * index / 40;
            double b = start + (end - start) * (index + 1) / 40;
            shapes.line(x + (float) Math.cos(a) * w / 2, 180 - y - (float) Math.sin(a) * h / 2,
                    x + (float) Math.cos(b) * w / 2, 180 - y - (float) Math.sin(b) * h / 2);
        }
        shapes.end();
    }
    public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
        if (filled) { begin(ShapeRenderer.ShapeType.Filled, fill); shapes.triangle(x1, 180 - y1, x2, 180 - y2, x3, 180 - y3); shapes.end(); }
        if (stroked) { begin(ShapeRenderer.ShapeType.Line, stroke); shapes.triangle(x1, 180 - y1, x2, 180 - y2, x3, 180 - y3); shapes.end(); }
    }
    public void beginShape() { vertices.clear(); }
    public void vertex(float x, float y) { vertices.add(x); vertices.add(y); }
    public void endShape() {
        if (filled && vertices.size() >= 6) {
            float[] polygon = new float[vertices.size()];
            for (int index = 0; index < polygon.length; index += 2) {
                polygon[index] = vertices.get(index);
                polygon[index + 1] = 180 - vertices.get(index + 1);
            }
            var triangles = new com.badlogic.gdx.math.EarClippingTriangulator().computeTriangles(polygon);
            for (int index = 0; index + 2 < triangles.size; index += 3) {
                int a = triangles.get(index) * 2;
                int b = triangles.get(index + 1) * 2;
                int c = triangles.get(index + 2) * 2;
                begin(ShapeRenderer.ShapeType.Filled, fill);
                shapes.triangle(polygon[a], polygon[a + 1], polygon[b], polygon[b + 1], polygon[c], polygon[c + 1]);
                shapes.end();
            }
        }
        for (int index = 0; index + 3 < vertices.size(); index += 2) {
            line(vertices.get(index), vertices.get(index + 1), vertices.get(index + 2), vertices.get(index + 3));
        }
    }
    public float cos(float value) { return (float) Math.cos(value); }
    public float sin(float value) { return (float) Math.sin(value); }
    public void textSize(int size) { fontSize = size; }
    public void text(Object text, float x, float y) {
        var font = skin.getFont("default-font");
        float sx = font.getData().scaleX;
        float sy = font.getData().scaleY;
        Color previous = new Color(font.getColor());
        try {
            font.getData().setScale(sx * fontSize / Math.max(1, font.getLineHeight()));
            font.setColor(fill);
            batch.begin();
            font.draw(batch, String.valueOf(text), x, 180 - y);
            batch.end();
        } finally {
            font.getData().setScale(sx, sy);
            font.setColor(previous);
        }
    }
    public void pushStyle() { styles.push(new Style(new Color(fill), new Color(stroke), filled, stroked, fontSize, blend)); }
    public void popStyle() {
        Style state = styles.pop(); fill.set(state.fill()); stroke.set(state.stroke()); filled = state.filled();
        stroked = state.stroked(); fontSize = state.font(); blend = state.blend();
    }
    public void blendMode(int mode) { blend = mode; }
    public void imageMode(int ignored) {
        // CORNER is the only image mode requested by BroadcastElement; image() always uses it.
    }
    public void noTint() {
        // image() sets its batch color to white before every draw, so no tint state can leak in.
    }
    public void image(BroadcastImage image, float x, float y, float w, float h) {
        image(image, x, y, w, h, 1);
    }

    /** Opacity multiplies the PNG's own alpha; it is local to this draw and cannot tint later images. */
    public void image(BroadcastImage image, float x, float y, float w, float h, float opacity) {
        opacity = Math.max(0, Math.min(1, opacity));
        if (opacity == 0) { return; }
        if (blend == DIFFERENCE) {
            // Exact Processing DIFFERENCE for arbitrary RGBA images, not an approximate blend equation.
            differenceImage(image, x, y, w, h, opacity);
            return;
        }
        batch.setColor(1, 1, 1, opacity);
        // Keep accumulated framebuffer alpha intact when blending a translucent overlay over its base.
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin(); batch.draw(image.texture(), x, 180 - y - h, w, h); batch.end();
        batch.setColor(Color.WHITE);
    }
    private void differenceImage(BroadcastImage image, float x, float y, float w, float h, float opacity) {
        loadPixels();
        Pixmap source = image.pixels();
            for (int yy = Math.max(0, (int) y); yy < Math.min(180, y + h); yy++) {
                for (int xx = Math.max(0, (int) x); xx < Math.min(320, x + w); xx++) {
                    int color = source.getPixel(Math.min(source.getWidth() - 1, (int) ((xx - x) * source.getWidth() / w)),
                            Math.min(source.getHeight() - 1, (int) ((yy - y) * source.getHeight() / h)));
                    int old = pixels[yy * 320 + xx];
                    float alpha = (color & 255) / 255f * opacity;
                    int r = Math.round(red(old) * (1 - alpha) + Math.abs(red(old) - red(color)) * alpha);
                    int g = Math.round(green(old) * (1 - alpha) + Math.abs(green(old) - green(color)) * alpha);
                    int b = Math.round(blue(old) * (1 - alpha) + Math.abs(blue(old) - blue(color)) * alpha);
                    pixels[yy * 320 + xx] = color(r, g, b);
                }
            }
            updatePixels();
    }
    public void loadPixels() {
        Pixmap image = Pixmap.createFromFrameBuffer(0, 0, 320, 180);
        pixels = new int[320 * 180];
        for (int y = 0; y < 180; y++) {
            for (int x = 0; x < 320; x++) { pixels[y * 320 + x] = image.getPixel(x, 179 - y); }
        }
        image.dispose();
    }
    public void updatePixels() {
        Pixmap image = new Pixmap(320, 180, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 180; y++) {
            for (int x = 0; x < 320; x++) { image.drawPixel(x, y, pixels[y * 320 + x]); }
        }
        Texture texture = new Texture(image);
        batch.setColor(Color.WHITE);
        batch.disableBlending(); batch.begin(); batch.draw(texture, 0, 0, 320, 180); batch.end(); batch.enableBlending();
        texture.dispose(); image.dispose();
    }
    public float red(int color) { return color >>> 24 & 255; }
    public float green(int color) { return color >>> 16 & 255; }
    public float blue(int color) { return color >>> 8 & 255; }
    public int color(float r, float g, float b) { return (int) r << 24 | (int) g << 16 | (int) b << 8 | 255; }

    @Override
    public void dispose() {
        audio.shutdownNow();
        clipboardImages.forEach(BroadcastImage::dispose);
        layerImages.forEach(BroadcastImage::dispose);
        batch.dispose(); shapes.dispose();
    }
}
