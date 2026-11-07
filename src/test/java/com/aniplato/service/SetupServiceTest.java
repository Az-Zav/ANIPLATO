package com.aniplato.service;

import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.SetupService.Extras;
import com.aniplato.service.SetupService.Finding;
import com.aniplato.service.SetupService.Kind;
import com.aniplato.support.TestDb;
import com.aniplato.util.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec 6.8, First-Run Setup. Each test sets up a scratch database named after the test database (so it can never be the real
 * one) on the test server, and drops it afterwards. The settings go to a temp file, never the user's own.
 */
class SetupServiceTest {

    private static final String SCRATCH = TestDb.NAME + "_setup";
    private static final int TABLES = SetupService.schemaTables().size();

    @TempDir Path folder;
    private Path settings;
    private SetupService service;

    @BeforeEach
    void freshScratch() {
        Assumptions.assumeFalse(TestDb.isOnline(), "the Setup tests need a plain local server");
        TestDb.dropScratch(SCRATCH);
        settings = folder.resolve("db.properties");
        service = new SetupService(settings);
    }

    @AfterEach
    void dropScratch() {
        if (!TestDb.isOnline()) TestDb.dropScratch(SCRATCH);
    }

    private SetupDetails login() {
        String[] hp = TestDb.hostPort().split(":");
        return new SetupDetails(hp[0], hp.length > 1 ? hp[1] : "3306", SCRATCH, TestDb.serverUser(), TestDb.serverPassword(), "");
    }

    private void createAdmin() {
        service.create(login(), "owner@example.org", "a-good-password", "a-good-password", Extras.NONE);
    }

    private long count(String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection(serverUrl(), TestDb.serverUser(), TestDb.serverPassword());
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql.replace("{db}", "`" + SCRATCH + "`"))) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void run(String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection(serverUrl(), TestDb.serverUser(), TestDb.serverPassword());
             Statement st = c.createStatement()) {
            st.execute(sql.replace("{db}", "`" + SCRATCH + "`"));
        }
    }

    private static String serverUrl() {
        return "jdbc:mysql://" + TestDb.hostPort() + "/?useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true";
    }

    private Properties saved() throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(settings)) {
            p.load(in);
        }
        return p;
    }

    //---TEST CONNECTION---

    @Test
    void aMissingDatabaseIsEmptyAndWillBeCreated() {
        Finding f = service.inspect(login());
        assertEquals(Kind.EMPTY, f.kind());
        assertFalse(f.databaseExists());
        assertTrue(f.message().contains("will be created"), f.message());
    }

    @Test
    void aWrongPasswordIsSaidInPlainWordsAndChangesNothing() {
        SetupDetails d = login();
        Finding f = service.inspect(new SetupDetails(d.host(), d.port(), d.database(), d.user(), "not-the-password", ""));
        assertEquals(Kind.CANNOT_CONTINUE, f.kind());
        assertTrue(f.message().contains("user name and password"), f.message());
    }

    @Test
    void anUnreachableServerSaysSo() {
        SetupDetails d = login();
        Finding f = service.inspect(new SetupDetails(d.host(), "1", d.database(), d.user(), d.password(), ""));
        assertEquals(Kind.CANNOT_CONTINUE, f.kind());
        assertTrue(f.message().contains("Cannot reach"), f.message());
    }

    @Test
    void badFieldsAreRefusedBeforeAnyConnection() {
        SetupDetails d = login();
        for (SetupDetails bad : new SetupDetails[]{
                new SetupDetails("", d.port(), d.database(), d.user(), d.password(), ""),
                new SetupDetails(d.host(), "abc", d.database(), d.user(), d.password(), ""),
                new SetupDetails(d.host(), d.port(), "x`; DROP DATABASE y", d.user(), d.password(), ""),
                new SetupDetails(d.host(), d.port(), d.database(), "", d.password(), ""),
                new SetupDetails(d.host(), d.port(), d.database(), d.user(), "", ""),
                new SetupDetails(d.host(), d.port(), d.database(), d.user(), d.password(), folder.resolve("none.pem").toString())}) {
            assertThrows(RuleViolationException.class, () -> service.inspect(bad));
        }
    }

    @Test
    void aPastedUrlGivesHostPortAndDatabase() {
        SetupDetails d = SetupDetails.fromUrl("jdbc:mysql://db.example.com:23021/shelter?ssl-mode=REQUIRED", "u", "p", "");
        assertEquals("db.example.com", d.host());
        assertEquals("23021", d.port());
        assertEquals("shelter", d.database());
        assertEquals("3306", SetupDetails.fromUrl("jdbc:mysql://localhost/", "u", "p", "").port());
        assertThrows(RuleViolationException.class, () -> SetupDetails.fromUrl("postgres://x", "u", "p", ""));
    }

    //---CREATE---

    @Test
    void createMakesTheDatabaseTablesCitiesAndTheAdminTheyChose() throws Exception {
        createAdmin();
        assertEquals(TABLES, count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + SCRATCH + "'"));
        assertEquals(17, count("SELECT COUNT(*) FROM {db}.city"));
        assertEquals(1, count("SELECT COUNT(*) FROM {db}.account"));
        assertEquals(0, count("SELECT must_change_password FROM {db}.account WHERE role = 'ADMIN'"), "they chose the password, so none is forced");
        try (Connection c = DriverManager.getConnection(serverUrl(), TestDb.serverUser(), TestDb.serverPassword());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT email, password_hash FROM `" + SCRATCH + "`.account")) {
            assertTrue(rs.next());
            assertEquals("owner@example.org", rs.getString("email"));
            assertTrue(PasswordHasher.matches("a-good-password", rs.getString("password_hash")), "stored as a BCrypt hash, never plain");
        }
    }

    @Test
    void createSavesTheSettingsAndKeepsWhatTheFileAlreadyHad() throws Exception {
        Files.writeString(settings, "ai.gemini.api-key=old-key\nmail.user=me@example.org\npool.size=5\n");
        service.create(login(), "owner@example.org", "a-good-password", "a-good-password", new Extras("", "", ""));
        Properties p = saved();
        assertEquals(TestDb.serverUser(), p.getProperty("user"));
        assertEquals(TestDb.serverPassword(), p.getProperty("password"));
        assertTrue(p.getProperty("url").contains("/" + SCRATCH), p.getProperty("url"));
        assertEquals("old-key", p.getProperty("ai.gemini.api-key"), "a blank optional field leaves the earlier value");
        assertEquals("me@example.org", p.getProperty("mail.user"));
        assertEquals("5", p.getProperty("pool.size"));
        assertFalse(p.containsKey("ssl.ca"), "no certificate was given");
    }

    @Test
    void optionalKeyAndMailAreSavedWhenGiven() throws Exception {
        service.create(login(), "owner@example.org", "a-good-password", "a-good-password", new Extras(" my-key ", "me@gmail.com", "app pass"));
        Properties p = saved();
        assertEquals("my-key", p.getProperty("ai.gemini.api-key"));
        assertEquals("me@gmail.com", p.getProperty("mail.user"));
        assertEquals("app pass", p.getProperty("mail.password"));
        assertEquals("smtp.gmail.com", p.getProperty("mail.smtp.host"));
    }

    @Test
    void theAdminDetailsAreCheckedBeforeAnythingIsCreated() throws Exception {
        for (String[] bad : new String[][]{
                {"not-an-email", "a-good-password", "a-good-password"},
                {"owner@example.org", "short", "short"},
                {"owner@example.org", "a-good-password", "different-password"}}) {
            assertThrows(RuleViolationException.class, () -> service.create(login(), bad[0], bad[1], bad[2], Extras.NONE));
        }
        assertEquals(Kind.EMPTY, service.inspect(login()).kind());
        assertFalse(Files.exists(settings));
    }

    @Test
    void aDatabaseThatExistsButIsEmptyIsUsedNotRecreated() throws Exception {
        run("CREATE DATABASE {db}");
        run("CREATE TABLE {db}.other_apps_notes (id INT)");
        run("INSERT INTO {db}.other_apps_notes VALUES (42)");
        assertEquals(Kind.EMPTY, service.inspect(login()).kind());
        createAdmin();
        assertEquals(42, count("SELECT id FROM {db}.other_apps_notes"), "an unrelated table is left alone");
        assertEquals(1, count("SELECT COUNT(*) FROM {db}.account"));
    }

    //---NEVER OVERWRITE---

    @Test
    void aDatabaseThatIsAlreadySetUpIsRefusedAndKeepsItsData() throws Exception {
        createAdmin();
        assertEquals(Kind.ALREADY_SET_UP, service.inspect(login()).kind());
        run("INSERT INTO {db}.city (name) VALUES ('Extra City')");
        RuleViolationException e = assertThrows(RuleViolationException.class,
                () -> service.create(login(), "second@example.org", "another-password", "another-password", Extras.NONE));
        assertTrue(e.getMessage().contains("already set up"), e.getMessage());
        assertEquals(18, count("SELECT COUNT(*) FROM {db}.city"), "nothing was dropped or reloaded");
        assertEquals(1, count("SELECT COUNT(*) FROM {db}.account"), "no second admin");
    }

    @Test
    void connectOnlySavesTheSettingsOfADatabaseThatIsSetUp() throws Exception {
        createAdmin();
        Files.delete(settings);
        long accounts = count("SELECT COUNT(*) FROM {db}.account");
        service.connect(login(), Extras.NONE);
        assertEquals(TestDb.serverUser(), saved().getProperty("user"));
        assertEquals(accounts, count("SELECT COUNT(*) FROM {db}.account"));
        assertEquals(TABLES, count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + SCRATCH + "'"));
    }

    @Test
    void connectRefusesAnEmptyDatabaseAndWritesNoFile() {
        assertThrows(RuleViolationException.class, () -> service.connect(login(), Extras.NONE));
        assertFalse(Files.exists(settings));
    }

    @Test
    void someOfOurTablesWithoutTheRestIsRefusedAndLeftAlone() throws Exception {
        run("CREATE DATABASE {db}");
        run("CREATE TABLE {db}.city (name VARCHAR(5))");
        run("INSERT INTO {db}.city VALUES ('keep')");
        Finding f = service.inspect(login());
        assertEquals(Kind.CANNOT_CONTINUE, f.kind());
        assertTrue(f.message().contains("city"), f.message());
        assertThrows(RuleViolationException.class,
                () -> service.create(login(), "owner@example.org", "a-good-password", "a-good-password", Extras.NONE));
        assertEquals(1, count("SELECT COUNT(*) FROM {db}.city"));
        assertEquals(1, count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + SCRATCH + "'"));
    }

    @Test
    void tablesWithoutAnAdminAskForOneAndAddOnlyTheAdmin() throws Exception {
        createAdmin();
        run("DELETE FROM {db}.account");
        assertEquals(Kind.ADMIN_MISSING, service.inspect(login()).kind());
        run("INSERT INTO {db}.city (name) VALUES ('Extra City')");
        service.create(login(), "new@example.org", "a-good-password", "a-good-password", Extras.NONE);
        assertEquals(1, count("SELECT COUNT(*) FROM {db}.account"));
        assertEquals(18, count("SELECT COUNT(*) FROM {db}.city"), "the cities were not reloaded");
    }

    @Test
    void aFailureHalfwayRemovesTheTablesItMadeSoSetupCanBeRunAgain() throws Exception {
        String broken = "CREATE TABLE city (city_id INT PRIMARY KEY);\nCREATE TABLE account (id INT);\nTHIS IS NOT SQL;";
        assertThrows(RuleViolationException.class, () -> service.create(login(), "owner@example.org", "a-good-password",
                "a-good-password", Extras.NONE, broken));
        assertEquals(0, count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + SCRATCH + "'"));
        assertEquals(Kind.EMPTY, service.inspect(login()).kind());
        createAdmin();   // the real script then works
        assertEquals(1, count("SELECT COUNT(*) FROM {db}.account"));
        assertNotNull(saved().getProperty("url"));
    }

    @Test
    void theSchemaScriptInTheProgramNeverDropsAnything() {
        String script = SetupService.schemaScript().toUpperCase();
        assertFalse(script.contains("DROP "), "setup must not be able to drop");
        assertFalse(script.contains("CREATE DATABASE"));
        assertEquals(14, SetupService.schemaTables().size());
    }
}
