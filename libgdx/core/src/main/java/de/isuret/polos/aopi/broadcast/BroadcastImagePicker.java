package de.isuret.polos.aopi.broadcast;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface BroadcastImagePicker {
    enum Source { FILES, LAYERS, CLIPBOARD }

    void choose(Source source, Consumer<List<Path>> selected, Consumer<String> error);
}
