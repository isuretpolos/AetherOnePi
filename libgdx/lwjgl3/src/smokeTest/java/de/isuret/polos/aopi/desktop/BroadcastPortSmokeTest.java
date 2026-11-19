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
                var context = new BroadcastContext(skin, settings, null, () -> 0) {
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
                    context.getImageLayers().drawBroadcast(context, 0, 0, 320, 180, new Random() {
                        @Override
                        public int nextInt(int bound) { return bound - 1; }
                    });
                    context.loadPixels();
                    require(context.pixels[100] == 0xff0000ff, "Image overlays did not draw after the base.");
                    context.getImageLayers().drawBroadcast(context, 0, 0, 320, 180, new Random() {
                        @Override
                        public int nextInt(int bound) { return 0; }
                    });
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

    private static void verifyDisplayedOrientation(com.badlogic.gdx.scenes.scene2d.ui.Skin skin,
                                                   BroadcastImage image) throws Exception {
        BroadcastContext context = new BroadcastContext(skin, new ApplicationSettings(), null, () -> 0);
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
