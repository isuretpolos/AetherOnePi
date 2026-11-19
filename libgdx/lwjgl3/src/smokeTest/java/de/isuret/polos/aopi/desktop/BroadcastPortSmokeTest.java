package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import de.isuret.polos.aopi.broadcast.*;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.Setting;
import de.isuret.polos.aopi.ui.DashboardSkin;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/** Executes rare original paint branches explicitly, instead of waiting for a random visual event. */
public final class BroadcastPortSmokeTest {

    private BroadcastPortSmokeTest() {
    }

    public static void main(String[] args) {
        var configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi original BroadcastElement port check");
        configuration.setWindowedMode(640, 360);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var skin = DashboardSkin.create();
                var settings = new ApplicationSettings();
                var soundRequests = new java.util.concurrent.atomic.AtomicInteger();
                var seed = new java.util.concurrent.atomic.AtomicInteger(seedForTransparency(0));
                var seedDraws = new java.util.concurrent.atomic.AtomicInteger();
                var layerSeeds = new java.util.ArrayDeque<Integer>();
                var hotbits = new de.isuret.polos.aopi.session.CachedHotbits(java.nio.file.Path.of("unused")) {
                    @Override
                    public synchronized int nextInt(int bound) {
                        if (bound == Integer.MAX_VALUE) {
                            seedDraws.incrementAndGet();
                            if (!layerSeeds.isEmpty()) { return layerSeeds.removeFirst(); }
                            return seed.get();
                        }
                        return 0;
                    }
                };
                var context = new BroadcastContext(skin, settings, hotbits, () -> 0) {
                    @Override
                    public void playAudio(Runnable task) {
                        soundRequests.incrementAndGet();
                    }
                };
                var buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 320, 180, false);
                context.setNowMillis(System.currentTimeMillis());
                var element = new BroadcastElement(context, "BROADCAST", 60, "Sulfur", 10);
                element.setRandom(new Random(123));
                buffer.begin();
                try {
                    var asymmetric = image("orientation.png", 0x0000ffff);
                    asymmetric.pixels().setColor(0xff0000ff);
                    asymmetric.pixels().fillRectangle(0, 0, 320, 90);
                    asymmetric.texture().draw(asymmetric.pixels(), 0, 0);
                    context.image(asymmetric, 0, 0, 320, 180);
                    context.loadPixels();
                    require(context.pixels[10 * 320 + 10] == 0xff0000ff, "Image top is vertically inverted.");
                    require(context.pixels[170 * 320 + 10] == 0x0000ffff, "Image bottom is vertically inverted.");
                    context.updatePixels();
                    context.loadPixels();
                    require(context.pixels[10 * 320 + 10] == 0xff0000ff, "Pixel update inverted the image top.");
                    require(context.pixels[170 * 320 + 10] == 0x0000ffff, "Pixel update inverted the image bottom.");
                    buffer.end();
                    verifyDisplayedOrientation(skin, asymmetric);
                    buffer.begin();
                    asymmetric.dispose();
                    context.noStroke();
                    context.fill(30, 60, 90);
                    context.rect(0, 0, 320, 180);
                    invoke(element, "partialInvert", new Class[]{int.class, int.class, int.class, int.class}, 0, 0, 320, 180);
                    context.loadPixels();
                    require(context.pixels[100] == context.color(225, 195, 165), "Partial inversion differs from Processing.");
                    var green = image("base.png", 0x00ff00ff);
                    var red = image("overlay.png", 0xff0000ff);
                    context.setImages(List.of(green, red), true);
                    settings.setEnabled(Setting.BROADCAST_IMAGES, true);
                    drawLayerFrame(context, new Random() {
                        @Override
                        public int nextInt(int bound) { return bound - 1; }
                    });
                    context.loadPixels();
                    require(context.pixels[100] == 0xff0000ff, "Image overlays did not draw after the base.");
                    int seedsAtStart = seedDraws.get();
                    for (int frame = 0; frame < 120; frame++) {
                        drawLayerFrame(context, new Random());
                    }
                    require(seedDraws.get() == seedsAtStart && seedsAtStart == 2,
                            "Image-layer animation consumed hotbits after its initial seed.");
                    seed.set(seedForTransparency(50));
                    layerSeeds.add(seedForTransparency(0));
                    layerSeeds.add(seedForTransparency(50));
                    context.setBroadcastId("half-opacity");
                    drawLayerFrame(context, new Random());
                    context.loadPixels();
                    // SpriteBatch packs alpha into an even 8-bit value; allow its two-level quantization.
                    require(Math.abs(context.red(context.pixels[100]) - 128) <= 2
                                    && Math.abs(context.green(context.pixels[100]) - 128) <= 2
                                    && (context.pixels[100] & 255) == 255,
                            "50% layer transparency did not blend with the base: " + Integer.toHexString(context.pixels[100]));
                    seed.set(seedForTransparency(100));
                    layerSeeds.add(seedForTransparency(0));
                    layerSeeds.add(seedForTransparency(100));
                    context.setBroadcastId("transparent");
                    drawLayerFrame(context, new Random() {
                        @Override
                        public int nextInt(int bound) { return 0; }
                    });
                    context.loadPixels();
                    require(context.pixels[100] == 0x00ff00ff, "100% transparency did not reveal the base.");
                    require(seedDraws.get() == 6, "A new broadcast did not receive exactly one seed per layer, including the base.");
                    context.setBroadcastId("all-transparent");
                    drawLayerFrame(context, new Random());
                    context.loadPixels();
                    require(context.red(context.pixels[100]) < 10 && context.green(context.pixels[100]) < 10,
                            "Transparent base/overlays retained opaque pixels from the previous cycle.");
                    // Check consecutive cycles against two independent seeded sequences.
                    int baseSeed = seedForTransparency(20);
                    int overlaySeed = seedForTransparency(80);
                    layerSeeds.add(baseSeed);
                    layerSeeds.add(overlaySeed);
                    Random expectedBase = new Random(baseSeed);
                    Random expectedOverlay = new Random(overlaySeed);
                    context.setBroadcastId("independent-animation");
                    int previousPixel = 0;
                    boolean changed = false;
                    for (int frame = 0; frame < 120; frame++) {
                        drawLayerFrame(context, new Random());
                        context.loadPixels();
                        float baseAlpha = (100 - expectedBase.nextInt(101)) / 100f;
                        float overlayAlpha = (100 - expectedOverlay.nextInt(101)) / 100f;
                        float expectedRed = 255 * overlayAlpha + 8 * (1 - baseAlpha) * (1 - overlayAlpha);
                        float expectedGreen = (255 * baseAlpha + 5 * (1 - baseAlpha)) * (1 - overlayAlpha);
                        require(Math.abs(context.red(context.pixels[100]) - expectedRed) < 6
                                        && Math.abs(context.green(context.pixels[100]) - expectedGreen) < 6,
                                "A layer did not receive its independent transparency for this cycle.");
                        changed |= frame > 0 && previousPixel != context.pixels[100];
                        previousPixel = context.pixels[100];
                    }
                    require(changed && seedDraws.get() == 10, "Layer opacity stayed static or consumed additional hotbits.");
                    context.image(green, 0, 0, 320, 180);
                    context.blendMode(BroadcastContext.DIFFERENCE);
                    context.image(red, 0, 0, 320, 180);
                    context.blendMode(BroadcastContext.BLEND);
                    context.loadPixels();
                    require(context.pixels[100] == 0xffff00ff, "Image DIFFERENCE blend is incorrect.");
                    settings.setEnabled(Setting.BROADCAST_IMAGES, false);
                    for (String method : new String[]{"paintPoint", "paintSignatureCharacter", "paintSignatureText", "paintOneLayer", "paintOverlay"}) {
                        invoke(element, method, new Class[0]);
                    }
                    for (String method : new String[]{"paintLine", "paintArc", "paintBezier", "paintTriangle", "paintPolygon", "paintRadionicCard"}) {
                        invoke(element, method, new Class[]{int.class, int.class}, 1, 0);
                    }
                    for (String method : new String[]{"paintRectangle", "paintEllipse"}) {
                        invoke(element, method, new Class[]{int.class, int.class, int.class}, 1, 0, 40);
                    }
                    var random2 = BroadcastElement.class.getDeclaredField("random2");
                    random2.setAccessible(true);
                    random2.set(element, new SecureRandom() {
                        @Override
                        public int nextInt(int bound) { return bound - 1; }
                    });
                    invoke(element, "paintOneLayer", new Class[0]);
                    require(!context.getResonanceList().isEmpty(), "Original resonance test did not record an event.");
                    require(context.getResonatedList().getFirst().getResonateCounter() > 0, "Resonance count missing.");
                    invoke(element, "paintRadionicCardWave", new Class[]{int.class, int.class}, 3, 2);
                    require(element.getMovingWaveAmount() > 1, "Resonance wave did not expand.");
                    element.setPlayingSound(true);
                    invoke(element, "playBinauralSound", new Class[0]);
                    require(soundRequests.get() == 1, "Resonance sound path was not requested.");
                    element.setPlayingSound(false);
                    element.setBinaural(null);
                    var advanced = BroadcastElement.class.getDeclaredField("playingSoundAdvanced");
                    advanced.setAccessible(true);
                    advanced.set(element, true);
                    invoke(element, "playBinauralSound", new Class[0]);
                    require(soundRequests.get() == 2, "Advanced sound path was not requested.");
                    advanced.set(element, false);
                    element.draw();
                    element.draw(0, 0, 320, 180);
                    System.out.println("Original BroadcastElement port passed: every paint family, image layers, DIFFERENCE, inversion, resonance test, counts, wave, and progress.");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                } finally {
                    buffer.end();
                    element.dispose();
                    buffer.dispose();
                    context.dispose();
                    skin.dispose();
                }
                Gdx.app.exit();
            }
        }, configuration);
    }

    private static BroadcastImage image(String name, int color) {
        Pixmap pixels = new Pixmap(320, 180, Pixmap.Format.RGBA8888);
        pixels.setColor(color);
        pixels.fill();
        return new BroadcastImage(name, new Texture(pixels), pixels);
    }

    private static int seedForTransparency(int transparency) {
        for (int seed = 0; seed < 100000; seed++) {
            if (new Random(seed).nextInt(101) == transparency) { return seed; }
        }
        throw new AssertionError("No fixture seed for transparency " + transparency);
    }

    private static void drawLayerFrame(BroadcastContext context, Random random) {
        // This fixture framebuffer represents the temporary image pass, not persistent artwork.
        Gdx.gl.glClearColor(0.03f, 0.02f, 0.06f, 1);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
        context.getImageLayers().drawBroadcast(context, 0, 0, 320, 180, random);
    }

    private static void verifyDisplayedOrientation(com.badlogic.gdx.scenes.scene2d.ui.Skin skin,
                                                   BroadcastImage image) throws Exception {
        ApplicationSettings settings = new ApplicationSettings();
        var seedDraws = new java.util.concurrent.atomic.AtomicInteger();
        int layerSeed = seedForTransparency(50);
        var hotbits = new de.isuret.polos.aopi.session.CachedHotbits(java.nio.file.Path.of("unused")) {
            @Override
            public synchronized int nextInt(int bound) {
                seedDraws.incrementAndGet();
                return layerSeed;
            }
        };
        BroadcastContext context = new BroadcastContext(skin, settings, hotbits, () -> 0);
        BroadcastService service = new BroadcastService();
        service.enqueue(List.of(new BroadcastService.Request("Orientation", 60, 1)));
        service.start();
        BroadcastVisual visual = new BroadcastVisual(service, context, skin);
        visual.setWidth(320);
        visual.setHeight(visual.getPrefHeight());
        visual.update();
        // Replace the random paint with an asymmetric fixture, then exercise the real tile draw.
        var field = BroadcastVisual.class.getDeclaredField("surfaces");
        field.setAccessible(true);
        Object surface = ((java.util.Map<?, ?>) field.get(visual)).values().iterator().next();
        var bufferField = surface.getClass().getDeclaredField("buffer");
        bufferField.setAccessible(true);
        FrameBuffer source = (FrameBuffer) bufferField.get(surface);
        source.begin();
        context.image(image, 0, 0, 320, 180);
        source.end();
        FrameBuffer output = new FrameBuffer(Pixmap.Format.RGBA8888, 320, 240, false);
        var batch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, 320, 240));
        output.begin();
        batch.begin();
        visual.draw(batch, 1);
        batch.end();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, 320, 240);
        try {
            require(pixels.getPixel(10, 150) == 0xff0000ff, "Displayed broadcast image top is inverted.");
            require(pixels.getPixel(10, 20) == 0x0000ffff, "Displayed broadcast image bottom is inverted.");
            output.end();
            // Pause new random paint, retaining existing artwork while testing image pass changes.
            var elementField = surface.getClass().getDeclaredField("element");
            elementField.setAccessible(true);
            ((BroadcastElement) elementField.get(surface)).setSeconds(null);
            context.setImages(List.of(image("green-layer.png", 0x00ff00ff)), true);
            settings.setEnabled(Setting.BROADCAST_IMAGES, true);
            var imageBufferField = surface.getClass().getDeclaredField("imageBuffer");
            imageBufferField.setAccessible(true);
            FrameBuffer imageBuffer = (FrameBuffer) imageBufferField.get(surface);
            Random expected = new Random(layerSeed);
            for (int frame = 0; frame < 120; frame++) {
                visual.update();
                source.begin();
                context.loadPixels();
                require(context.pixels[10 * 320 + 10] == 0xff0000ff
                                && context.pixels[170 * 320 + 10] == 0x0000ffff,
                        "Image-layer rendering erased persistent broadcast artwork.");
                source.end();
                imageBuffer.begin();
                context.loadPixels();
                int expectedAlpha = Math.round((100 - expected.nextInt(101)) * 255 / 100f);
                require(Math.abs((context.pixels[100] & 255) - expectedAlpha) <= 2,
                        "The temporary image pass accumulated old opacity.");
                imageBuffer.end();
            }
            require(seedDraws.get() == 1, "Separate image rendering consumed hotbits per frame.");
            // Restart the layer generator to check a known 50% composite over the saved red/blue artwork.
            context.setImages(List.of(image("green-layer.png", 0x00ff00ff)), true);
            visual.update();
            output.begin();
            batch.begin();
            visual.draw(batch, 1);
            batch.end();
            Pixmap composite = Pixmap.createFromFrameBuffer(0, 0, 320, 240);
            try {
                int top = composite.getPixel(10, 150);
                int bottom = composite.getPixel(10, 20);
                require(Math.abs(context.red(top) - 128) <= 2 && Math.abs(context.green(top) - 128) <= 2,
                        "Image layers did not composite over preserved artwork at the expected opacity.");
                require(context.blue(bottom) > 120 && context.green(bottom) > 120,
                        "The existing lower artwork disappeared behind the image pass.");
            } finally {
                composite.dispose();
            }
        } finally {
            pixels.dispose();
            output.end();
            output.dispose();
            batch.dispose();
            visual.dispose();
        }
    }

    private static void invoke(BroadcastElement element, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = BroadcastElement.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(element, arguments);
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
