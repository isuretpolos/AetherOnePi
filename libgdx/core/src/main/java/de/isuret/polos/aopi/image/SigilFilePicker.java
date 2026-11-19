package de.isuret.polos.aopi.image;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Desktop dialogs return selections to the render thread; drawing itself stays embedded in IMAGE. */
public interface SigilFilePicker {
    void choose(boolean collection, Consumer<Path> selected, Consumer<String> error);
    void chooseColor(int currentArgb, IntConsumer selected);

    default void pasteImage(Consumer<java.awt.image.BufferedImage> selected, Consumer<String> error) {
        error.accept("Clipboard images are not available on this platform.");
    }
}
