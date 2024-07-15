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

/**
 * A utility class for managing settings in Toml files.
 *
 * @author @pellacanimuller
 */
public class TomlSettingsManager {
    private static final String SETTINGS_FILE = "config/settings.toml";
    private final static Logger log = LogManager.getLogger(TomlSettingsManager.class);

    /**
     * Writes the given settings to the TOML file.
     *
     * @param settings The settings to write.
     */
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


    /**
     * Merges the given new settings into the existing settings object.
     *
     * @param existingSettings The existing settings. Will have all settings in it.
     * @param newSettings The new settings to merge.
     */
    @SuppressWarnings("unchecked")
    public static void mergeSettings(Map<String, Object> existingSettings, Map<String, Object> newSettings) {
        newSettings.forEach((key, value) -> {
            if (value instanceof Map && existingSettings.get(key) instanceof Map) {
                mergeSettings((Map<String, Object>) existingSettings.get(key), (Map<String, Object>) value);
            } else {
                existingSettings.put(key, value);
            }
        });
    }

    /**
     * Reads the settings from the TOML file.
     *
     * @return The read settings.
     */
    public static Toml readSettings() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            return new Toml();
        }
        return new Toml().read(file);
    }

    /**
     * Reads the settings as a JSON string.
     *
     * @return The settings as a JSON string.
     */
    public static String readSettingsAsJson() {
        return mapToJson(readSettings().toMap()).toString();
    }


    /**
     * Reads the settings as a map.
     *
     * @return The settings as a map.
     */
    public static Map<String, Object> readSettingsAsMap() {
        return flattenMap(readSettings().toMap(), ".").entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() instanceof Long ? ((Long) entry.getValue()).intValue() : entry.getValue()
                ));
    }

    /**
     * Flattens the given map.
     *
     * @param map The map to flatten.
     * @param separator The separator to use between levels of nesting, when `null`, the top level is returned.
     * @return The flattened map.
     */
    public static Map<String, Object> flattenMap(Map<String, Object> map, String separator) {
        return map.entrySet().stream()
                .flatMap(entry -> flatten(entry.getKey(), entry.getValue(), separator).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    /**
     * Updates the settings from the given JSON object.
     *
     * @param jsonSettings The JSON object containing the new settings.
     * @param game The game to update the settings for.
     * @throws IllegalArgumentException If the JSON object contains unsupported types.
     */
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

    /**
     * Converts the given JSON object to a map.
     *
     * @param jsonObject The JSON object to convert.
     * @return The converted map.
     */
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

    /**
     * Converts the given JSON value to an object.
     *
     * @param value The JSON value to convert.
     * @return The converted object.
     */
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

    /**
     * Flattens the given map with the given prefix and separator.
     *
     * @param prefix The prefix to use.
     * @param value The value to flatten.
     * @param separator The separator to use.
     * @return The flattened map.
     */
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

    /**
     * Converts the given map to a JSON object.
     *
     * @param map The map to convert.
     * @return The converted JSON object.
     * @throws IllegalArgumentException If the map contains unsupported types.
     */
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