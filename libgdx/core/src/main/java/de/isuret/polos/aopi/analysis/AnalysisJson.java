package de.isuret.polos.aopi.analysis;

import com.badlogic.gdx.utils.JsonValue;

import java.time.Instant;
import java.util.ArrayList;

/** Explicit JSON fields keep saved results understandable and independent of Java reflection. */
public final class AnalysisJson {

    private AnalysisJson() {
    }

    public static JsonValue write(AnalysisResult result) {
        JsonValue root = new JsonValue(JsonValue.ValueType.object);
        root.addChild("id", new JsonValue(result.id()));
        root.addChild("created", new JsonValue(result.created().toString()));
        root.addChild("database", new JsonValue(result.database()));
        root.addChild("hitLimit", new JsonValue(result.hitLimit()));
        root.addChild("trials", new JsonValue(result.trials()));
        if (result.generalVitality() != null) {
            root.addChild("generalVitality", new JsonValue(result.generalVitality()));
        }
        JsonValue rows = new JsonValue(JsonValue.ValueType.array);
        for (AnalysisResult.Row row : result.rows()) {
            JsonValue value = new JsonValue(JsonValue.ValueType.object);
            value.addChild("name", new JsonValue(row.name()));
            value.addChild("url", new JsonValue(row.url()));
            value.addChild("classification", new JsonValue(row.classification()));
            value.addChild("subfamily", new JsonValue(row.subfamily()));
            value.addChild("energeticValue", new JsonValue(row.energeticValue()));
            value.addChild("potency", new JsonValue(row.potency()));
            value.addChild("level", new JsonValue(row.level()));
            value.addChild("recurring", new JsonValue(row.recurring()));
            if (row.generalVitality() != null) {
                value.addChild("generalVitality", new JsonValue(row.generalVitality()));
            }
            rows.addChild(value);
        }
        root.addChild("rows", rows);
        return root;
    }

    public static AnalysisResult read(JsonValue root) {
        if (!root.isObject() || !root.require("rows").isArray()) {
            throw new IllegalArgumentException("Invalid analysis snapshot.");
        }
        var rows = new ArrayList<AnalysisResult.Row>();
        for (JsonValue row : root.require("rows")) {
            rows.add(new AnalysisResult.Row(row.getString("name"), row.getString("url", ""),
                    row.getString("classification", ""), row.getString("subfamily", ""),
                    row.getInt("energeticValue"), row.getString("potency"), row.getInt("level"),
                    row.has("generalVitality") ? row.getInt("generalVitality") : null, row.getInt("recurring", 0)));
        }
        return new AnalysisResult(root.getString("id"), Instant.parse(root.getString("created")),
                root.getString("database"), root.getInt("hitLimit"), root.getInt("trials"),
                root.has("generalVitality") ? root.getInt("generalVitality") : null, rows);
    }
}
