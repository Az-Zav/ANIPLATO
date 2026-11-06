package com.aniplato.util;

import com.aniplato.exception.DataAccessException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HexFormat;

/**
 * Opens JDBC connections from db.properties and runs transactions (A2). Connections come from a small pool: opening one
 * to a remote database costs about half a second (TCP, TLS, login) and four setup statements, which a screen that
 * makes a dozen database calls cannot afford. Closing a connection returns it to the pool.
 */
public class Database {

    /** Database work that runs on one connection, inside one transaction. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    private static HikariDataSource pool;
    private static String poolKey = "";

    static {
        // A connection used in the last 30 s is trusted without a ping (a ping is one more round trip per call);
        // the pool's keep-alive tests idle ones every 30 s, so a stale connection is replaced before it is handed out.
        System.setProperty("com.zaxxer.hikari.aliveBypassWindowMs", "30000");
    }

    private Database() { }

    /** A connection from the pool. Close it (try-with-resources) to give it back. */
    public static Connection getConnection() throws SQLException {
        return pool().getConnection();
    }

    /** Closes every pooled connection; the next call opens new ones. Tests call it before they recreate their database. */
    public static synchronized void closePool() {
        if (pool != null) pool.close();
        pool = null;
        poolKey = "";
    }

    private static synchronized HikariDataSource pool() {
        String url = AppConfig.required("url");
        String user = AppConfig.required("user");
        String password = AppConfig.required("password");
        String key = url + "|" + user + "|" + password;
        if (pool == null || pool.isClosed() || !key.equals(poolKey)) {
            if (pool != null) pool.close();   // the settings changed (a test pointed the app at its own database)
            HikariConfig config = new HikariConfig();
            config.setPoolName("aniplato");
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(Integer.parseInt(AppConfig.get("pool.size", "3")));   // shared server: every teammate's pool counts
            config.setMinimumIdle(Integer.parseInt(AppConfig.get("pool.minIdle", "1")));
            config.setConnectionTimeout(Long.parseLong(AppConfig.get("pool.connectTimeoutMs", "20000")));
            config.setKeepaliveTime(Long.parseLong(AppConfig.get("pool.keepAliveMs", "30000")));
            config.setMaxLifetime(Long.parseLong(AppConfig.get("pool.maxLifetimeMs", "1500000")));
            addOnlineSettings(config);
            config.setInitializationFailTimeout(-1);   // a database that is down shows as a message on the screen, not at start-up
            pool = new HikariDataSource(config);
            poolKey = key;
        }
        return pool;
    }

    /**
     * A hosted database (Aiven) needs an encrypted link checked against the provider's CA, and its server clock is UTC
     * while the app's times are Manila's. Both are plain settings: ssl.ca (path to the CA .pem) and session.timezone
     * (default +08:00, applied to every connection so NOW() and stored times agree with the app).
     */
    private static void addOnlineSettings(HikariConfig config) {
        String zone = AppConfig.get("session.timezone", "+08:00");
        if (AppConfig.get("ssl.ca", null) != null || AppConfig.get("url", "").contains("aivencloud")) {
            config.addDataSourceProperty("sessionVariables", "time_zone='" + zone + "'");
        }
        String ca = AppConfig.get("ssl.ca", null);
        if (ca == null) return;
        String password = HexFormat.of().formatHex(randomBytes());
        Path store = truststoreFrom(AppConfig.path(ca), password);
        config.addDataSourceProperty("sslMode", "VERIFY_CA");
        config.addDataSourceProperty("trustCertificateKeyStoreUrl", store.toUri().toString());
        config.addDataSourceProperty("trustCertificateKeyStorePassword", password);
        config.addDataSourceProperty("trustCertificateKeyStoreType", "PKCS12");
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /** Builds a throw-away PKCS12 truststore (deleted on exit) from a PEM CA file, which is the form providers hand out. */
    static Path truststoreFrom(Path pem, String password) {
        try (InputStream in = Files.newInputStream(pem)) {
            Collection<? extends Certificate> certs = CertificateFactory.getInstance("X.509").generateCertificates(in);
            if (certs.isEmpty()) throw new GeneralSecurityException("no certificate");
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(null, null);
            int n = 0;
            for (Certificate cert : certs) store.setCertificateEntry("ca" + n++, cert);
            Path file = Files.createTempFile("aniplato-ca", ".p12");
            file.toFile().deleteOnExit();
            try (OutputStream out = Files.newOutputStream(file)) {
                store.store(out, password.toCharArray());
            }
            return file;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Could not read the CA certificate '" + pem
                    + "' (ssl.ca in db.properties). It must be the PEM file downloaded from the database provider.", e);
        }
    }

    /**
     * Runs read-only work on a connection from the pool, with no transaction. A RuntimeException reaches the caller
     * unchanged; an SQLException becomes a DataAccessException with the given message.
     */
    public static <T> T read(String errorMessage, SqlWork<T> work) {
        try (Connection c = getConnection()) {
            return work.run(c);
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    /**
     * Runs the work as one transaction: commits when it finishes, rolls back when anything throws (A2).
     * A RuleViolationException or other RuntimeException reaches the caller unchanged; an SQLException
     * becomes a DataAccessException with the given message.
     */
    public static <T> T inTransaction(String errorMessage, SqlWork<T> work) {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }
}
