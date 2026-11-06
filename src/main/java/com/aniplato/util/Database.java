package com.aniplato.util;

import com.aniplato.exception.DataAccessException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

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
            config.setInitializationFailTimeout(-1);   // a database that is down shows as a message on the screen, not at start-up
            pool = new HikariDataSource(config);
            poolKey = key;
        }
        return pool;
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
