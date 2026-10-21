package com.aniplato.util;

import com.aniplato.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens JDBC connections from db.properties and runs transactions (A2). */
public class Database {

    /** Database work that runs on one connection, inside one transaction. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    private Database() { }

    public static Connection getConnection() throws SQLException {
        String url = AppConfig.required("url");
        String user = AppConfig.required("user");
        String password = AppConfig.required("password");
        return DriverManager.getConnection(url, user, password);
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
