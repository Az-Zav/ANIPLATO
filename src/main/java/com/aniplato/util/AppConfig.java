package com.aniplato.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads db.properties. A system property named aniplato.&lt;key&gt; wins over the file, which is how
 * the tests point the app at their own database without editing the file.
 */
public final class AppConfig {

    private static final Properties FILE = load();

    private AppConfig() { }

    /** The value for key, or fallback when it is missing or blank. */
    public static String get(String key, String fallback) {
        String value = System.getProperty("aniplato." + key, FILE.getProperty(key));
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** The value for key; a missing one is a setup mistake. */
    public static String required(String key) {
        String value = get(key, null);
        if (value == null) throw new IllegalStateException("Missing '" + key + "' in db.properties.");
        return value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getResourceAsStream("/db.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing db.properties. Copy db.properties.example and configure MySQL.");
            }
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
