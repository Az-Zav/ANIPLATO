package com.aniplato.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Reads db.properties. A system property named aniplato.&lt;key&gt; wins over the file, which is how the tests point the
 * app at their own database without editing the file. The file is the one on the classpath (a developer's
 * src/main/resources); a packaged app has none inside it, so it looks for one beside the user instead:
 * %APPDATA%\ANIPLATO\db.properties, then next to the program, then in the working folder.
 */
public final class AppConfig {

    private static volatile Path EXTERNAL = findExternal();
    private static volatile Properties FILE = load();

    private AppConfig() { }

    /** The value for key, or fallback when it is missing or blank. */
    public static String get(String key, String fallback) {
        String value = System.getProperty("aniplato." + key, FILE.getProperty(key));
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** The value for key; a missing one is a setup mistake. */
    public static String required(String key) {
        String value = get(key, null);
        if (value == null) {
            if (FILE.isEmpty()) throw new IllegalStateException(missingFileMessage());
            throw new IllegalStateException("Missing '" + key + "' in db.properties.");
        }
        return value;
    }

    /**
     * Every setting whose key starts with the prefix, as a Properties copy (the file, then any -Daniplato.&lt;key&gt;
     * override). For a library that wants a Properties, such as the mail session; it never gets the unrelated settings.
     */
    public static Properties withPrefix(String prefix) {
        Properties found = new Properties();
        for (String key : FILE.stringPropertyNames()) {
            if (key.startsWith(prefix)) found.setProperty(key, FILE.getProperty(key));
        }
        String systemPrefix = "aniplato." + prefix;
        for (String name : System.getProperties().stringPropertyNames()) {
            if (name.startsWith(systemPrefix)) found.setProperty(name.substring("aniplato.".length()), System.getProperty(name));
        }
        return found;
    }

    /**
     * A file named in the settings. A relative path is looked for beside db.properties first (so a packaged app can keep
     * ca.pem next to it), then as written.
     */
    public static Path path(String value) {
        Path given = Paths.get(value);
        if (given.isAbsolute() || EXTERNAL == null) return given;
        Path beside = EXTERNAL.toAbsolutePath().getParent().resolve(given);
        return Files.exists(beside) ? beside : given;
    }

    /** True when the database login (url, user, password) is there; if not, the app shows the Setup screen (6.8). */
    public static boolean isConfigured() {
        return get("url", null) != null && get("user", null) != null && get("password", null) != null;
    }

    /** The settings file a packaged app writes and reads first: %APPDATA%\ANIPLATO\db.properties. */
    public static Path settingsFile() {
        return Paths.get(expectedLocation());
    }

    /**
     * Writes the settings to the file, one key per line in the order given, escaped so that any password reads back exactly.
     * It creates the folder if needed. Call {@link #reload()} afterwards for the running app to see the new values.
     */
    public static void write(Path file, java.util.Map<String, String> values) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            text.write(("# ANIPLATO settings. Keep this file private: it holds your database password." + System.lineSeparator())
                    .getBytes(StandardCharsets.ISO_8859_1));
            for (java.util.Map.Entry<String, String> entry : values.entrySet()) {
                Properties one = new Properties();
                one.setProperty(entry.getKey(), entry.getValue());
                ByteArrayOutputStream line = new ByteArrayOutputStream();
                one.store((OutputStream) line, null);   // Properties does the escaping; its date comment is dropped below
                for (String part : line.toString(StandardCharsets.ISO_8859_1).split("\\R")) {
                    if (!part.isEmpty() && !part.startsWith("#")) {
                        text.write((part + System.lineSeparator()).getBytes(StandardCharsets.ISO_8859_1));
                    }
                }
            }
            Files.write(file, text.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the settings to " + file, e);
        }
    }

    /** Reads the settings again, after {@link #write} or when a file has been put in place. */
    public static synchronized void reload() {
        EXTERNAL = findExternal();
        FILE = load();
    }

    /** Where the settings file is expected for a packaged app, for the message that tells the user to create it. */
    static String expectedLocation() {
        String appData = System.getenv("APPDATA");
        return (appData != null ? Paths.get(appData, "ANIPLATO") : Paths.get(System.getProperty("user.home"), ".aniplato"))
                .resolve("db.properties").toString();
    }

    private static String missingFileMessage() {
        return "ANIPLATO has no settings yet. Put the db.properties file you were given at " + expectedLocation() + " and start again.";
    }

    private static Path findExternal() {
        List<Path> candidates = new java.util.ArrayList<>();
        candidates.add(Paths.get(expectedLocation()));
        String exe = System.getProperty("jpackage.app-path");
        if (exe != null) candidates.add(Paths.get(exe).toAbsolutePath().getParent().resolve("db.properties"));
        candidates.add(Paths.get("db.properties"));
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream inside = AppConfig.class.getResourceAsStream("/db.properties")) {
            if (inside != null) {
                properties.load(inside);
            } else if (EXTERNAL != null) {
                try (InputStream outside = Files.newInputStream(EXTERNAL)) {
                    properties.load(outside);
                }
            }
            return properties;   // none found: empty, and required() says where to put one
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
