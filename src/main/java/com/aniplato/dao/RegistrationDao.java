package com.aniplato.dao;

import com.aniplato.model.Registration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/** SQL only. One DAO per table. */
public class RegistrationDao {

    /** Saves a new Pending registration and returns its id. */
    public int insert(Connection c, Registration r, LocalDateTime submittedAt) throws SQLException {
        String sql = "INSERT INTO registration (role, name, city_id, contact_person, contact_phone, email, " +
                "status, submitted_at) VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.role.name());
            ps.setString(2, r.name);
            ps.setInt(3, r.city.id);
            ps.setString(4, r.contactPerson);
            ps.setString(5, r.contactPhone);
            ps.setString(6, r.email);
            ps.setObject(7, submittedAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}
