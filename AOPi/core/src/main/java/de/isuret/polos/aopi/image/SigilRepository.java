package de.isuret.polos.aopi.image;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import de.isuret.polos.aopi.persistence.JsonFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Collections use the original PNG folder format; a separate snapshot recovers the editor on restart. */
public class SigilRepository {
    public record Draft(SigilCollection collection, boolean dirty, int selected, SigilMaker.Tool tool, int brush, int ink) { }

    private final Path directory;

    public SigilRepository(Path directory) { this.directory = directory; }

    public Path collections() { return directory.resolve("data_images"); }

    public Path save(SigilCollection snapshot) throws IOException { return snapshot.save(collections()); }

    public void saveDraft(Draft draft) throws IOException {
        Path root = directory.resolve("sigil-recovery");
        draft.collection().copy().save(root);
        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        json.addChild("name", new JsonValue(draft.collection().getName()));
        json.addChild("dirty", new JsonValue(draft.dirty()));
        json.addChild("selected", new JsonValue(draft.selected()));
        json.addChild("tool", new JsonValue(draft.tool().name()));
        json.addChild("brush", new JsonValue(draft.brush()));
        json.addChild("ink", new JsonValue(draft.ink()));
        JsonValue visible = new JsonValue(JsonValue.ValueType.array);
        JsonValue order = new JsonValue(JsonValue.ValueType.array);
        for (var layer : draft.collection().getLayers()) {
            visible.addChild(new JsonValue(layer.isVisible()));
            order.addChild(new JsonValue(layer.getName()));
        }
        json.addChild("visible", visible);
        json.addChild("order", order);
        JsonFiles.write(root.resolve("draft.json"), json);
    }

    public Draft loadDraft() throws IOException {
        Path root = directory.resolve("sigil-recovery").toAbsolutePath().normalize();
        if (!Files.exists(root.resolve("draft.json"))) { return null; }
        JsonValue json = new JsonReader().parse(Files.readString(root.resolve("draft.json")));
        Path folder = root.resolve(json.getString("name")).normalize();
        if (!root.equals(folder.getParent())) { throw new IOException("Invalid sigil recovery path."); }
        SigilCollection collection = SigilCollection.open(folder);
        if (json.has("order")) {
            java.util.List<String> order = new java.util.ArrayList<>();
            for (JsonValue layer : json.get("order")) { order.add(layer.asString()); }
            collection.restoreOrder(order);
        }
        JsonValue visible = json.get("visible");
        if (visible != null && visible.isArray()) {
            for (int index = 0; index < Math.min(visible.size, collection.getLayers().size()); index++) {
                if (!visible.getBoolean(index)) { collection.getLayers().get(index).toggleVisible(); }
            }
        }
        return new Draft(collection, json.getBoolean("dirty", true), json.getInt("selected", 0),
                SigilMaker.Tool.valueOf(json.getString("tool", "PEN")), json.getInt("brush", 4), json.getInt("ink", 0xff000000));
    }
}
