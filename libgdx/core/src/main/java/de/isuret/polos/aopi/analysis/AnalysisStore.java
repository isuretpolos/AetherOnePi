package de.isuret.polos.aopi.analysis;

import com.badlogic.gdx.utils.JsonReader;
import de.isuret.polos.aopi.persistence.JsonFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Last completed result recovery is independent of any case attachment. */
public class AnalysisStore {

    private final Path file;

    public AnalysisStore(Path directory) {
        file = directory.resolve("last-analysis.json");
    }

    public AnalysisResult load() throws IOException {
        return Files.exists(file) ? AnalysisJson.read(new JsonReader().parse(Files.readString(file))) : null;
    }

    public void save(AnalysisResult result) throws IOException {
        JsonFiles.write(file, AnalysisJson.write(result));
    }

    public void clear() throws IOException {
        Files.deleteIfExists(file);
    }
}
