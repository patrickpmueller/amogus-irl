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
import java.util.stream.Collectors;

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
        Map<String, Object> flatExisting = flattenMap(existingSettings);
        Map<String, Object> flatNew = flattenMap(newSettings);

        flatExisting.putAll(flatNew);

        existingSettings.clear();
        existingSettings.putAll(flatExisting);
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

    public static Map<String, Object> readSettingsAsMap() {
        return flattenMap(readSettings().toMap(), ".").entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() instanceof Long ? ((Long) entry.getValue()).intValue() : entry.getValue()
                ));
    }


    public static Map<String, Object> flattenMap(Map<String, Object> map) {
        return flattenMap(map, null);
    }


    public static Map<String, Object> flattenMap(Map<String, Object> map, String separator) {
        return map.entrySet().stream()
                .flatMap(entry -> flatten(entry.getKey(), entry.getValue(), separator).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public static void changeSettingsFromJson(JsonObject jsonSettings, Game game) throws IllegalArgumentException {
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> roles = new HashMap<>();
        Map<String, Object> tasks = new HashMap<>();

        settings.put("roles", roles);
        settings.put("tasks", tasks);

        jsonSettings.forEach((key, value) -> settings.put(key, convertValue(value)));

        game.updateSettings(settings);

        TomlSettingsManager.writeSettings(settings);
    }


    private static Map<String, Object> convertJsonObjectToMap(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        jsonObject.forEach((key, value) -> {
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                map.put(key, convertJsonObjectToMap(value.asJsonObject()));
            } else {
                map.put(key, convertValue(value));
            }
        });
        return map;
    }


    private static Object convertValue(JsonValue value) {
        return switch (value.getValueType()) {
            case OBJECT -> convertJsonObjectToMap(value.asJsonObject());
            case STRING -> value.toString();
            case NUMBER -> Integer.parseInt(value.toString());
            case TRUE, FALSE -> Boolean.parseBoolean(value.toString());
            case NULL -> null;
            case ARRAY -> value.asJsonArray().stream()
                    .map(TomlSettingsManager::convertValue)
                    .toList();
        };
    }


    private static Map<String, Object> flatten(String prefix, Object value, String separator) {
        Map<String, Object> result = new HashMap<>();

        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) value;
            nestedMap.forEach((key, val) -> {
                String newKey = separator != null ? prefix + separator + key : key;
                result.putAll(flatten(newKey, val, separator));
            });
        } else {
            result.put(prefix, value);
        }

        return result;
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
}