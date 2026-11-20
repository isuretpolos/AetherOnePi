package de.isuret.polos.aopi.analysis;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** The desktop module supplies the native file chooser; rate parsing stays in core. */
@FunctionalInterface
public interface RateFilePicker {
    void choose(Consumer<List<Path>> onSelected, Consumer<String> onError);
}
