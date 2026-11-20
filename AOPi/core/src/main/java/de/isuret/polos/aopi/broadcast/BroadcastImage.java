package de.isuret.polos.aopi.broadcast;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

/** Image handle used by the port of the original Processing broadcast code. */
public record BroadcastImage(String name, Texture texture, com.badlogic.gdx.graphics.Pixmap pixels) implements Disposable {
    public static BroadcastImage load(java.nio.file.Path path) {
        var pixels = new com.badlogic.gdx.graphics.Pixmap(new com.badlogic.gdx.files.FileHandle(path.toFile()));
        return new BroadcastImage(path.getFileName().toString(), new Texture(pixels), pixels);
    }
    @Override
    public void dispose() {
        texture.dispose();
        pixels.dispose();
    }
}
