package com.aniplato;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * docs/MAP.md must list every file and every public method of the service, DAO, util and model layers,
 * and must not list a file that is gone. A new file or method means a new row in the map, in the same pull request.
 */
class MapCoverageTest {

    private static final Path MAP = Path.of("docs", "MAP.md");
    private static final Path MAIN = Path.of("src", "main", "java", "com", "aniplato");
    private static final Pattern TOKEN = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern PUBLIC_METHOD = Pattern.compile(
            "^\\s+public\\s+(?:static\\s+)?(?:abstract\\s+)?(?:final\\s+)?[\\w<>\\[\\], .?]+?\\s+(\\w+)\\s*\\(");
    private static final Set<String> NOT_LISTED = Set.of("toString", "equals", "hashCode");
    private static final List<String> METHOD_LAYERS = List.of("service", "dao", "util", "model");

    private static String map() throws IOException {
        return Files.readString(MAP);
    }

    /** Every token in backticks on the page, outside the ``` diagram blocks. */
    private static Set<String> tokens(String text) {
        Set<String> found = new TreeSet<>();
        Matcher m = TOKEN.matcher(text.replaceAll("(?s)```.*?```", ""));
        while (m.find()) found.add(m.group(1));
        return found;
    }

    private static void addAll(Set<String> keys, Path root, String prefixForRootFiles) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile).forEach(p -> {
                String rel = root.relativize(p).toString().replace('\\', '/');
                keys.add(rel.contains("/") ? rel : prefixForRootFiles + rel);
            });
        }
    }

    /** The key the map uses for each file in the project. */
    private static Set<String> existingFiles() throws IOException {
        Set<String> keys = new TreeSet<>();
        addAll(keys, MAIN, "aniplato/");
        addAll(keys, Path.of("src", "main", "resources"), "resources/");
        addAll(keys, Path.of("src", "test", "java", "com", "aniplato"), "aniplato/");
        addAll(keys, Path.of("src", "test", "resources"), "resources/");
        if (Files.exists(Path.of("uploads", ".gitkeep"))) keys.add("uploads/.gitkeep");   // the rest of uploads/ is git-ignored
        for (String folder : List.of("sql", "scripts", "docs")) {
            Path root = Path.of(folder);
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(Files::isRegularFile).forEach(p -> keys.add(p.toString().replace('\\', '/')));
            }
        }
        for (String name : List.of("pom.xml", "README.md", "AGENTS.md", "CLAUDE.md", "LICENSE", "setup-db.bat",
                "setup-db.ps1", ".gitignore", ".gitattributes")) {
            if (Files.exists(Path.of(name))) keys.add(name);
        }
        return keys;
    }

    @Test
    void everyFileIsListedInTheMap() throws IOException {
        Set<String> listed = tokens(map());
        Set<String> missing = new TreeSet<>();
        for (String key : existingFiles()) {
            if (!listed.contains(key)) missing.add(key);
        }
        assertEquals(Set.of(), missing, "add a row for each of these files to docs/MAP.md (Reference A)");
    }

    @Test
    void everyFileTheMapListsExists() throws IOException {
        String text = map();
        int from = text.indexOf("## 4. Reference A");
        int to = text.indexOf("## 5. Reference B");
        Set<String> existing = existingFiles();
        Set<String> gone = new TreeSet<>();
        for (String line : text.substring(from, to).split("\n")) {
            if (!line.startsWith("| `")) continue;
            String firstCell = line.substring(2, line.indexOf(" | ", 2) < 0 ? line.length() : line.indexOf(" | ", 2));
            for (String token : tokens(firstCell)) {
                if (!existing.contains(token)) gone.add(token);
            }
        }
        assertEquals(Set.of(), gone, "these rows in docs/MAP.md Reference A name files that do not exist");
    }

    @Test
    void everyPublicMethodOfTheServiceDaoUtilAndModelLayersIsListed() throws IOException {
        Set<String> listed = tokens(map());
        Set<String> missing = new TreeSet<>();
        for (String layer : METHOD_LAYERS) {
            try (Stream<Path> files = Files.list(MAIN.resolve(layer))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String cls = file.getFileName().toString().replace(".java", "");
                    for (String line : Files.readAllLines(file)) {
                        Matcher m = PUBLIC_METHOD.matcher(line);
                        if (!m.find()) continue;
                        String name = m.group(1);
                        if (name.equals(cls) || NOT_LISTED.contains(name) || line.contains(" class ")
                                || line.contains(" record ") || line.contains(" enum ") || line.contains(" interface ")) continue;
                        if (!listed.contains(cls + "." + name)) missing.add(cls + "." + name);
                    }
                }
            }
        }
        assertEquals(Set.of(), missing, "add each of these methods to docs/MAP.md (Reference B)");
    }
}
