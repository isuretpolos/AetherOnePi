package de.isuret.polos.aopi.broadcast;

import com.badlogic.gdx.utils.JsonValue;
import de.isuret.polos.aopi.persistence.JsonFiles;

import java.io.IOException;
import java.nio.file.Path;

/** One document per completed/stopped job avoids overwriting past sessions or replaying a queue on restart. */
public class BroadcastHistory {

    private final Path directory;

    public BroadcastHistory(Path directory) {
        this.directory = directory.resolve("broadcasts");
    }

    public void save(BroadcastService.Report report) throws IOException {
        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        json.addChild("id", new JsonValue(report.id()));
        json.addChild("signature", new JsonValue(report.signature()));
        json.addChild("seconds", new JsonValue(report.seconds()));
        json.addChild("intensity", new JsonValue(report.intensity()));
        json.addChild("multiplier", new JsonValue(report.intensity()));
        json.addChild("started", new JsonValue(report.started().toString()));
        json.addChild("ended", new JsonValue(report.ended().toString()));
        json.addChild("elapsedSeconds", new JsonValue(report.elapsedSeconds()));
        json.addChild("outcome", new JsonValue(report.outcome()));
        JsonFiles.write(directory.resolve(report.id() + ".json"), json);
    }

    public void saveResonances(String id, java.util.List<BroadcastService.ResonanceEvent> events) throws IOException {
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("id", new JsonValue(id));
        JsonValue entries = new JsonValue(JsonValue.ValueType.array);
        for (var event : events) {
            JsonValue entry = new JsonValue(JsonValue.ValueType.object);
            entry.addChild("time", new JsonValue(event.time().toString()));
            entry.addChild("signature", new JsonValue(event.signature()));
            entries.addChild(entry);
        }
        root.addChild("events", entries);
        JsonFiles.write(directory.resolve("resonance").resolve(id + ".json"), root);
    }
}
