package com.pellacanimuller.amogus_irl.util;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.pellacanimuller.amogus_irl.game.Game;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TomlSettingsManager {

    private static final String SETTINGS_FILE = "src/main/settings/settings.toml";
    private final static Logger log = LogManager.getLogger(TomlSettingsManager.class);

    public static void writeSettings(Map<String, Object> settings) {
        Toml existingToml = readSettings();

        // Merge new settings into existing ones
        Map<String, Object> existingSettings = new HashMap<>(existingToml.toMap());
        mergeSettings(existingSettings, settings);

        try {
            new TomlWriter().write(existingSettings, new File(SETTINGS_FILE));
        } catch (IOException e) {
            log.error(e.getStackTrace());
        }
    }

    public static void mergeSettings(Map<String, Object> existingSettings, Map<String, Object> newSettings) {
        for (Map.Entry<String, Object> entry : newSettings.entrySet()) {
            if (entry.getValue() instanceof Map) {
                existingSettings.merge(entry.getKey(), entry.getValue(), (oldVal, newVal) -> {
                    if (oldVal instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> oldMap = (Map<String, Object>) oldVal;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> newMap = (Map<String, Object>) newVal;
                        Map<String, Object> mergedMap = new HashMap<>(oldMap);
                        mergeSettings(mergedMap, newMap);
                        return mergedMap;
                    }
                    return newVal;
                });
            } else {
                existingSettings.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static Toml readSettings() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            return new Toml();
        }
        return new Toml().read(file);
    }

    public static String readSettingsAsJson() {
        return mapToJson(readSettings().toMap()).toString();
    }


    @SuppressWarnings("unchecked")
    private static JsonObject mapToJson(Map<String, Object> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            switch (entry.getValue()) {
                case Map<?, ?> nestedMap -> builder.add(entry.getKey(), mapToJson((Map<String, Object>) nestedMap));
                case Integer value -> builder.add(entry.getKey(), value);
                case String value -> builder.add(entry.getKey(), value);
                case Boolean value -> builder.add(entry.getKey(), value);
                case Double value -> builder.add(entry.getKey(), value);
                case Long value -> builder.add(entry.getKey(), value);
                default -> throw new IllegalArgumentException("Unsupported type: " + entry.getValue().getClass());
            }
        }

        return builder.build();
    }


    public static void changeSettingsFromJson(JsonObject json, Game game) throws IllegalArgumentException {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> roles = new HashMap<>();
        Map<String, Object> tasks = new HashMap<>();

        settings.put("roles", roles);
        settings.put("tasks", tasks);

        json.forEach((key, value) -> updateVars(key, value, settings, game));

        TomlSettingsManager.writeSettings(settings);
    }

    private static Map<String, Object> convertJsonObjectToMap(JsonObject jsonObject, Game game) {
        Map<String, Object> map = new HashMap<>();
        jsonObject.forEach((key, value) -> {
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                map.put(key, convertJsonObjectToMap(value.asJsonObject(), game));
            } else {
                updateVars(key, value, map, game);
            }
        });
        return map;
    }

    @SuppressWarnings("unchecked")
    private static void updateVars(String key, JsonValue value, Map<String, Object> settings, Game game) {
        switch (value.getValueType()) {
            case OBJECT -> settings.put(key, convertJsonObjectToMap(value.asJsonObject(), game));
            case STRING -> log.info("Strings not used for config yet");
            case NUMBER -> {
                Map<String, Object> roles = (Map<String, Object>) settings.get("roles");
                Map<String, Object> tasks = (Map<String, Object>) settings.get("tasks");

                switch (key) {
                    case "impostors", "crewmates", "healers" -> roles.put(key, Integer.parseInt(value.toString()));
                    case "total", "perPlayer" -> tasks.put(key, Integer.parseInt(value.toString()));
                    case "maxPlayers" -> settings.put("maxPlayers", Integer.parseInt(value.toString()));
                    default -> {
                        log.error("Unknown key: {}", key);
                        return;
                    }
                }
                game.updateSetting(key, value.toString());
            }
            case TRUE, FALSE -> log.info("Boolean values not yet used in config");
            case NULL, ARRAY -> throw new IllegalArgumentException("Cannot parse " + value.getValueType());
        }
    }
}