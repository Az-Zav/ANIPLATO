package com.aniplato;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules in AGENTS.md and the spec's section 7.4, checked on the source files so a breach fails the
 * build instead of waiting for a review. Each rule lists the breaches that exist today; a new breach
 * fails the test, and so does fixing one without removing it from its list.
 */
class ArchitectureTest {

    private static final Path MAIN = Path.of("src", "main", "java", "com", "aniplato");
    private static final int MAX_LINES = 300;

    /** Files already over the limit. Split them, then delete the entry. */
    private static final Map<String, Integer> OVER_LIMIT = Map.of(
            "controller/RegisterStep2Controller.java", 342);

    private static final List<String> CONTROLLERS_IMPORTING_A_DAO = List.of(
            "controller/admin/SafetyNoticesController.java");   // only the NoticeRow record type

    private static final List<String> UTILS_USING_A_SERVICE = List.of(
            "util/RegistrationDraft.java");   // holds the DocumentUpload record

    //---HELPERS---

    private record Source(String path, List<String> lines) {
        String text() {
            return String.join("\n", lines);
        }
    }

    private static List<Source> sources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(p -> p.toString().endsWith(".java")).sorted().map(p -> {
                try {
                    return new Source(MAIN.relativize(p).toString().replace('\\', '/'), Files.readAllLines(p));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        }
    }

    private static List<String> filesWhere(java.util.function.Predicate<Source> test) throws IOException {
        return sources().stream().filter(test).map(Source::path).toList();
    }

    private static boolean in(Source s, String folder) {
        return s.path().startsWith(folder + "/");
    }

    private static void assertOnlyKnown(String rule, List<String> actual, List<String> known) {
        assertEquals(new TreeSet<>(known), new TreeSet<>(actual), rule);
    }

    //---LAYERS---

    @Test
    void controllersDoNotCallDaos() throws IOException {
        assertOnlyKnown("Controllers call services, never DAOs (A1). Known exceptions are listed in this test.",
                filesWhere(s -> in(s, "controller") && s.text().contains("import com.aniplato.dao.")),
                CONTROLLERS_IMPORTING_A_DAO);
    }

    @Test
    void sqlLivesOnlyInDaos() throws IOException {
        Pattern sql = Pattern.compile("\"[^\"]*\\b(SELECT .+ FROM |INSERT INTO |DELETE FROM |UPDATE \\w+ SET )");
        Pattern textBlock = Pattern.compile("\"\"\"\\s*\\n\\s*(SELECT|INSERT|UPDATE|DELETE)\\b");
        List<String> breaches = filesWhere(s -> !in(s, "dao")
                && (sql.matcher(s.text()).find() || textBlock.matcher(s.text()).find()));
        assertEquals(List.of(), breaches, "SQL belongs in a DAO, one per table");
    }

    @Test
    void onlyDatabaseOpensConnectionsAndRunsTransactions() throws IOException {
        Pattern jdbc = Pattern.compile("DriverManager|setAutoCommit|\\.commit\\(\\)|\\.rollback\\(\\)");
        List<String> breaches = filesWhere(s -> !s.path().equals("util/Database.java") && jdbc.matcher(s.text()).find());
        assertEquals(List.of(), breaches, "Use Database.getConnection() and Database.inTransaction(...) (A2)");
    }

    @Test
    void servicesDaosAndModelsKnowNothingOfJavaFx() throws IOException {
        List<String> breaches = filesWhere(s -> (in(s, "service") || in(s, "dao") || in(s, "model") || in(s, "exception"))
                && s.text().contains("import javafx"));
        assertEquals(List.of(), breaches, "Rules and data must work without a screen");
    }

    @Test
    void maintenanceOnlyCallsOtherServices() throws IOException {
        List<String> breaches = filesWhere(s -> s.path().equals("service/MaintenanceService.java")
                && Pattern.compile("import com\\.aniplato\\.(dao|util\\.Database)").matcher(s.text()).find());
        assertEquals(List.of(), breaches, "MaintenanceService changes data only by calling other services (A4)");
    }

    @Test
    void daosDoNotCallServices() throws IOException {
        List<String> breaches = filesWhere(s -> in(s, "dao") && s.text().contains("import com.aniplato.service."));
        assertEquals(List.of(), breaches, "Calls go View -> Controller -> Service -> DAO, never back up");
    }

    @Test
    void utilitiesDoNotDependOnServicesOrDaos() throws IOException {
        assertOnlyKnown("util/ sits below the services. Known exceptions are listed in this test.",
                filesWhere(s -> in(s, "util") && Pattern.compile("import com\\.aniplato\\.(service|dao)\\.").matcher(s.text()).find()),
                UTILS_USING_A_SERVICE);
    }

    @Test
    void allPopupsGoThroughTheDialogs() throws IOException {
        List<String> breaches = filesWhere(s -> !s.path().startsWith("controller/shared/Dialog")
                && !in(s, "controller/shared/dialog") && s.text().contains("new Alert("));
        assertEquals(List.of(), breaches, "Popups are dialogs in controller/shared/dialog, or DialogController.showError");
    }

    //---SIZE---

    @Test
    void noClassGrowsPast300Lines() throws IOException {
        Map<String, Integer> over = new TreeMap<>();
        for (Source s : sources()) {
            if (s.lines().size() > MAX_LINES) over.put(s.path(), s.lines().size());
        }
        // an entry may only stay while its file is still over the limit, and no listed file may grow
        Map<String, Integer> allowed = new TreeMap<>(OVER_LIMIT);
        for (Map.Entry<String, Integer> e : over.entrySet()) {
            Integer limit = allowed.get(e.getKey());
            assertTrue(limit != null && e.getValue() <= limit,
                    e.getKey() + " has " + e.getValue() + " lines; keep classes under " + MAX_LINES + " (split it instead of adding to it)");
        }
        for (String listed : allowed.keySet()) {
            assertTrue(over.containsKey(listed), listed + " is under " + MAX_LINES + " lines now: remove it from OVER_LIMIT");
        }
    }
}
