package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SQL for the Setup screen (6.8): what a database already holds, and creating the database and tables. Database and table
 * names cannot be bound as parameters, so the service checks every name against a strict pattern before it gets here.
 */
public class SetupDao {

    public boolean databaseExists(Connection c, String database) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?")) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** The names of the tables in the database, in lower case. */
    public Set<String> tableNames(Connection c, String database) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema = ?")) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> names = new LinkedHashSet<>();
                while (rs.next()) names.add(rs.getString(1).toLowerCase());
                return names;
            }
        }
    }

    public int countAdmins(Connection c, String database) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM `" + database + "`.account WHERE role = 'ADMIN'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Creates the database only if it is missing; an existing one is left exactly as it is. */
    public void createDatabase(Connection c, String database) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    /** Runs a whole script (several commands) on the connection's current database. */
    public void runScript(Connection c, String script) throws SQLException {
        try (Statement st = c.createStatement()) {
            boolean isResultSet = st.execute(script);
            while (isResultSet || st.getUpdateCount() != -1) isResultSet = st.getMoreResults();
        }
    }

    /** Removes tables that a failed setup left behind, in any order. Only ever called with tables the setup itself had just made. */
    public void dropTables(Connection c, Collection<String> tables) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String table : tables) st.execute("DROP TABLE IF EXISTS `" + table + "`");
            } finally {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }
}
