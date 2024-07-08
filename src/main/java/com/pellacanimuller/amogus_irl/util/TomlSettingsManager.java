package com.pellacanimuller.amogus_irl.util;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TomlSettingsManager {

    private static final String SETTINGS_FILE = "src/main/settings/settings.toml";
    private final static Logger log = LogManager.getLogger(TomlSettingsManager.class);

    public static void main(String[] args) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("title", "Test");

        Map<String, Object> database = new HashMap<>();
        database.put("stuff", "a string");
        settings.put("database", database);

        writeSettings(settings);

        Map<String, Object> owner = new HashMap<>();
        owner.put("name", "Tom Preston-Werner");
        owner.put("dob", "1979-05-27T07:32:00-08:00");
        settings.put("owner", owner);

        writeSettings(settings);
    }

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
}
