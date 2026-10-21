package com.aniplato.support;

import com.aniplato.util.Database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The test database. Every test starts from sql/schema.sql + sql/seed.sql loaded into
 * aniplato_test. It refuses to run against any other database name, so the real
 * aniplato database (and the shared team database) are never touched.
 */
public final class TestDb {

    public static final String NAME = "aniplato_test";
    private static final Path MAIN_CONFIG = Path.of("src", "main", "resources", "db.properties");
    private static final Pattern HOST_PORT = Pattern.compile("jdbc:mysql://([^/?]+)");
    private static final String OPTIONS = "?useSSL=false&serverTimezone=Asia/Manila&allowPublicKeyRetrieval=true&allowMultiQueries=true";

    private static String serverUrl;

    private TestDb() { }

    /** Points the app's Database and FileStorageService at the test database and a temp uploads folder. */
    private static synchronized void configure() {
        if (serverUrl != null) return;
        Properties main = readMainConfig();
        String hostPort = "localhost:3306";
        Matcher m = HOST_PORT.matcher(main.getProperty("url", ""));
        if (m.find()) hostPort = m.group(1);
        String user = env("ANIPLATO_TEST_DB_USER", main.getProperty("user", "root"));
        String password = env("ANIPLATO_TEST_DB_PASSWORD", main.getProperty("password", ""));
        serverUrl = "jdbc:mysql://" + hostPort + "/" + OPTIONS;
        System.setProperty("aniplato.url", "jdbc:mysql://" + hostPort + "/" + NAME + OPTIONS);
        System.setProperty("aniplato.user", user.trim());
        System.setProperty("aniplato.password", password.trim());
        try {
            System.setProperty("aniplato.uploads.dir", Files.createTempDirectory("aniplato-test-uploads").toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create a temp uploads folder", e);
        }
    }

    /** Drops and recreates aniplato_test from the project's own schema and seed files. */
    public static void reset() {
        configure();
        String schema = retarget(read("sql/schema.sql"));
        String seed = retarget(read("sql/seed.sql"));
        try (Connection c = DriverManager.getConnection(serverUrl,
                System.getProperty("aniplato.user"), System.getProperty("aniplato.password"));
             Statement st = c.createStatement()) {
            runScript(st, schema);
            runScript(st, seed);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the test database. Is MySQL running and is the user allowed to create databases?", e);
        }
    }

    /** One value from a query, e.g. scalar("SELECT COUNT(*) FROM account WHERE status = ?", "ACTIVE"). */
    @SuppressWarnings("unchecked")
    public static <T> T scalar(String sql, Object... args) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? (T) rs.getObject(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(sql, e);
        }
    }

    public static long count(String sql, Object... args) {
        Number n = scalar(sql, args);
        return n == null ? 0 : n.longValue();
    }

    /** Runs an INSERT/UPDATE/DELETE to set a test up or to simulate a change made elsewhere. */
    public static void exec(String sql, Object... args) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(sql, e);
        }
    }

    private static Connection open() throws SQLException {
        configure();
        Connection c = Database.getConnection();
        if (!NAME.equals(c.getCatalog())) {
            String actual = c.getCatalog();
            c.close();
            throw new IllegalStateException("Tests must run on " + NAME + " but this connection is on " + actual);
        }
        return c;
    }

    private static void bind(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    /** Points the scripts at aniplato_test. Fails loudly if the scripts no longer look the way this expects. */
    private static String retarget(String script) {
        String out = script.replace("DROP DATABASE IF EXISTS aniplato;", "DROP DATABASE IF EXISTS " + NAME + ";")
                .replace("CREATE DATABASE aniplato ", "CREATE DATABASE " + NAME + " ")
                .replace("USE aniplato;", "USE " + NAME + ";");
        if (Pattern.compile("(DROP DATABASE IF EXISTS|CREATE DATABASE|USE) aniplato\\b(?!_test)").matcher(out).find()) {
            throw new IllegalStateException("A script still targets the aniplato database; update TestDb.retarget");
        }
        return out;
    }

    private static void runScript(Statement st, String script) throws SQLException {
        boolean isResultSet = st.execute(script);
        while (isResultSet || st.getUpdateCount() != -1) {
            isResultSet = st.getMoreResults();
        }
    }

    private static String read(String file) {
        try {
            return Files.readString(Path.of(file));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file + " (run tests from the project folder)", e);
        }
    }

    private static Properties readMainConfig() {
        Properties p = new Properties();
        if (Files.isRegularFile(MAIN_CONFIG)) {
            try (InputStream in = Files.newInputStream(MAIN_CONFIG)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + MAIN_CONFIG, e);
            }
        }
        return p;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
