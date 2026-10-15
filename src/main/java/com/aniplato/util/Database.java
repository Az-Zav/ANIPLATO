package com.aniplato.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens JDBC connections from db.properties. */
public class Database {

    private static final Properties CONFIG = loadConfig();

    private Database() { }

    public static Connection getConnection() throws SQLException {
        String url = required("url");
        String user = required("user");
        String password = required("password");
        return DriverManager.getConnection(url, user, password);
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = Database.class.getResourceAsStream("/db.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing db.properties. Copy db.properties.example and configure MySQL.");
            }
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String required(String key) {
        String value = CONFIG.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing '" + key + "' in db.properties.");
        }
        return value.trim();
    }
}
