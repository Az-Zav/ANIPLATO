package com.aniplato.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens JDBC connections from db.properties. */
public class Database {

    private Database() { }

    public static Connection getConnection() throws SQLException {
        String url = AppConfig.required("url");
        String user = AppConfig.required("user");
        String password = AppConfig.required("password");
        return DriverManager.getConnection(url, user, password);
    }
}
