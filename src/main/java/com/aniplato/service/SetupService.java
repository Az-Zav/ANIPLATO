package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.SetupDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.util.AppConfig;
import com.aniplato.util.Database;
import com.aniplato.util.PasswordHasher;
import com.aniplato.util.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First-run setup (6.8): tests a MySQL login, says what the database holds, then either prepares an empty one (tables, cities,
 * first admin) or only saves the settings for one that is already set up. It never drops or overwrites anything. MySQL cannot
 * roll back table creation, so this is the one service that is not a single transaction: it checks first, and if creating the
 * tables fails it removes the tables it had just made, so setup can simply be run again.
 */
public class SetupService {

    /** What Test connection found. */
    public enum Kind { EMPTY, ALREADY_SET_UP, ADMIN_MISSING, CANNOT_CONTINUE }

    public record Finding(Kind kind, String message, boolean databaseExists) { }

    /** Optional settings entered on the same screen; blank ones are left as they are. */
    public record Extras(String geminiKey, String mailUser, String mailPassword) {
        public static final Extras NONE = new Extras("", "", "");
    }

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CREATE_TABLE = Pattern.compile("(?i)CREATE\\s+TABLE\\s+`?(\\w+)`?");
    private static final String ADMIN_NAME = "ANIPLATO Admin";

    private final SetupDao setupDao = new SetupDao();
    private final AccountDao accountDao = new AccountDao();
    private final Path settingsFile;

    public SetupService() {
        this(AppConfig.settingsFile());
    }

    /** For tests: the settings are written to this file instead of the user's own. */
    SetupService(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    /** Test connection: connects and reports what the database holds. Nothing is changed. */
    public Finding inspect(SetupDetails details) {
        SetupDetails d = details.checked();
        try (Connection c = open(d)) {
            return classify(c, d);
        } catch (SQLException e) {
            return cannot(explain(e, d), false);
        } catch (IllegalStateException e) {
            return cannot(e.getMessage(), false);   // an unreadable certificate file
        }
    }

    /**
     * Prepares an empty database (or only the missing admin of a half-finished one), creates the first admin and saves the
     * settings. Anything else is refused: a database that already has data is never touched.
     */
    public void create(SetupDetails details, String adminEmail, String adminPassword, String confirmPassword, Extras extras) {
        create(details, adminEmail, adminPassword, confirmPassword, extras, null);
    }

    void create(SetupDetails details, String adminEmail, String adminPassword, String confirmPassword, Extras extras, String scriptOverride) {
        SetupDetails d = details.checked();
        String email = adminEmail == null ? "" : adminEmail.trim();
        if (!EMAIL.matcher(email).matches()) throw new RuleViolationException("Enter a valid email address for the admin.");
        if (adminPassword == null || adminPassword.length() < Settings.MIN_PASSWORD_LENGTH) {
            throw new RuleViolationException("The admin password must be at least " + Settings.MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!adminPassword.equals(confirmPassword)) throw new RuleViolationException("The two passwords do not match.");
        String script = scriptOverride == null ? schemaScript() : scriptOverride;
        try (Connection c = open(d)) {
            Finding found = classify(c, d);
            switch (found.kind()) {
                case ALREADY_SET_UP -> throw new RuleViolationException(
                        "This database is already set up with an admin. Use Connect to use it as it is.");
                case CANNOT_CONTINUE -> throw new RuleViolationException(found.message());
                case EMPTY -> buildTables(c, d, script, found.databaseExists());
                case ADMIN_MISSING -> c.setCatalog(d.database());
            }
            accountDao.insertAdmin(c, email, PasswordHasher.hash(adminPassword), ADMIN_NAME, LocalDateTime.now());
        } catch (SQLException e) {
            throw new RuleViolationException(explain(e, d));
        }
        save(d, extras);
    }

    /** Connect: saves the settings for a database that is already set up. Creates and changes nothing in it. */
    public void connect(SetupDetails details, Extras extras) {
        SetupDetails d = details.checked();
        Finding found = inspect(d);
        if (found.kind() != Kind.ALREADY_SET_UP) {
            throw new RuleViolationException(found.kind() == Kind.CANNOT_CONTINUE ? found.message()
                    : "This database is not set up yet. Create it instead.");
        }
        save(d, extras);
    }

    //---INTERNALS---

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static Connection open(SetupDetails d) throws SQLException {
        return Database.openOnce(d.serverUrl(), d.user(), d.password(), d.caFile());
    }

    private static Finding cannot(String why, boolean exists) {
        return new Finding(Kind.CANNOT_CONTINUE, why, exists);
    }

    private Finding classify(Connection c, SetupDetails d) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        String product = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        if (product.toLowerCase().contains("mariadb") || tooOld(meta.getDatabaseMajorVersion(), meta.getDatabaseMinorVersion(), product)) {
            return cannot("ANIPLATO needs MySQL 8.0.16 or newer. This server is " + product + ".", false);
        }
        if (!setupDao.databaseExists(c, d.database())) {
            return new Finding(Kind.EMPTY, "Connected. The database '" + d.database() + "' does not exist yet and will be created.", false);
        }
        Set<String> ours = schemaTables();
        Set<String> present = new LinkedHashSet<>(setupDao.tableNames(c, d.database()));
        present.retainAll(ours);
        if (present.isEmpty()) {
            return new Finding(Kind.EMPTY, "Connected. The database '" + d.database() + "' has no ANIPLATO tables and is ready.", true);
        }
        if (present.size() < ours.size()) {
            return cannot("The database '" + d.database() + "' already has some of the tables ANIPLATO uses (" + String.join(", ", present)
                    + ") but not all of them. Choose an empty database or another name; nothing was changed.", true);
        }
        if (setupDao.countAdmins(c, d.database()) == 0) {
            return new Finding(Kind.ADMIN_MISSING, "Connected. The tables are there but no admin account exists. Choose one to create.", true);
        }
        return new Finding(Kind.ALREADY_SET_UP, "Connected. This database is already set up. Connect to use it as it is.", true);
    }

    private static boolean tooOld(int major, int minor, String version) {
        if (major != 8 || minor != 0) return major < 8;
        Matcher m = Pattern.compile("8\\.0\\.(\\d+)").matcher(version);
        return m.find() && Integer.parseInt(m.group(1)) < 16;   // CHECK constraints arrived in 8.0.16
    }

    private void buildTables(Connection c, SetupDetails d, String script, boolean databaseExists) throws SQLException {
        if (!databaseExists) setupDao.createDatabase(c, d.database());
        c.setCatalog(d.database());
        try {
            setupDao.runScript(c, script);
        } catch (SQLException e) {
            try {
                setupDao.dropTables(c, tablesIn(script));   // the database held none of them before, so these are the ones just made
            } catch (SQLException ignored) {
                // the original problem is the one to report
            }
            throw e;
        }
    }

    private void save(SetupDetails d, Extras extras) {
        Map<String, String> values = new LinkedHashMap<>();
        Properties old = new Properties();
        if (Files.isRegularFile(settingsFile)) {
            try (InputStream in = Files.newInputStream(settingsFile)) {
                old.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        values.put("url", d.databaseUrl());
        values.put("user", d.user());
        values.put("password", d.password());
        if (d.encrypted()) {
            values.put("ssl.ca", d.caFile().replace('\\', '/'));
            values.put("session.timezone", "+08:00");
        }
        values.put("pool.size", old.getProperty("pool.size", "3"));
        values.put("pool.minIdle", old.getProperty("pool.minIdle", "1"));
        Extras e = extras == null ? Extras.NONE : extras;
        keep(values, old, "ai.gemini.api-key", e.geminiKey());
        if (!blank(e.mailUser())) {
            values.put("mail.smtp.host", old.getProperty("mail.smtp.host", "smtp.gmail.com"));
            values.put("mail.smtp.port", old.getProperty("mail.smtp.port", "587"));
            values.put("mail.smtp.auth", "true");
            values.put("mail.smtp.starttls.enable", "true");
            values.put("mail.user", e.mailUser().trim());
            values.put("mail.password", e.mailPassword() == null ? "" : e.mailPassword());
        }
        for (String key : old.stringPropertyNames()) {
            if (!values.containsKey(key) && !key.equals("ssl.ca") && !key.equals("session.timezone")) values.put(key, old.getProperty(key));
        }
        AppConfig.write(settingsFile, values);
        AppConfig.reload();
    }

    /** A new value wins; otherwise whatever the file already had stays. */
    private static void keep(Map<String, String> values, Properties old, String key, String entered) {
        if (!blank(entered)) values.put(key, entered.trim());
        else if (old.getProperty(key) != null) values.put(key, old.getProperty(key));
    }

    /** The schema script, read from the program itself (the build copies sql/schema.sql in, so there is one copy). */
    static String schemaScript() {
        try (InputStream in = SetupService.class.getResourceAsStream("/sql/schema.sql")) {
            if (in == null) throw new IllegalStateException("The program is missing its schema file (sql/schema.sql).");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Set<String> schemaTables() {
        return tablesIn(schemaScript());
    }

    private static Set<String> tablesIn(String script) {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = CREATE_TABLE.matcher(script);
        while (m.find()) names.add(m.group(1).toLowerCase());
        return names;
    }

    /** One plain sentence for a database error: the person can fix the cause, so no stack trace and no SQL. */
    static String explain(SQLException e, SetupDetails d) {
        String text = String.valueOf(e.getMessage());
        int code = e.getErrorCode();
        if (code == 1045) return "The database did not accept that user name and password.";
        if (code == 1044 || code == 1142 || code == 1227 || code == 1370 || code == 1410) {
            return "This database user is not allowed to create the database or tables. Use a user with permission, or an empty database that already exists.";
        }
        if (text.toLowerCase().contains("ssl") || text.toLowerCase().contains("certificate")) {
            return "The secure connection failed. Check that the certificate file is the one your database provider gave you.";
        }
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        if (state.startsWith("08") || text.contains("Communications link failure") || text.contains("Connection refused")
                || text.contains("UnknownHost") || text.contains("timed out")) {
            return "Cannot reach " + d.host() + ":" + d.port() + ". Check the address, the port and your internet connection, and that MySQL is running.";
        }
        return "The database reported a problem: " + (text.length() > 200 ? text.substring(0, 200) : text);
    }
}
