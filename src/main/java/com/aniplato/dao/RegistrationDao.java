package com.aniplato.dao;

import com.aniplato.model.Account;
import com.aniplato.model.City;
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

    /** One registration with its city, or null. Documents are loaded separately by DocumentDao. */
    public Registration findById(Connection c, int registrationId) throws SQLException {
        String sql = """
                SELECT r.*, city.name AS city_name
                FROM registration r
                JOIN city ON city.city_id = r.city_id
                WHERE r.registration_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Registration r = new Registration();
                r.id = rs.getInt("registration_id");
                r.role = Account.Role.valueOf(rs.getString("role"));
                r.name = rs.getString("name");
                r.contactPerson = rs.getString("contact_person");
                r.contactPhone = rs.getString("contact_phone");
                r.email = rs.getString("email");
                r.status = Registration.Status.valueOf(rs.getString("status"));
                r.rejectionReason = rs.getString("rejection_reason");
                int reviewedBy = rs.getInt("reviewed_by");
                r.reviewedBy = rs.wasNull() ? null : reviewedBy;
                if (rs.getTimestamp("reviewed_at") != null) r.reviewedAt = rs.getTimestamp("reviewed_at").toLocalDateTime();
                r.submittedAt = rs.getTimestamp("submitted_at").toLocalDateTime();

                City city = new City();
                city.id = rs.getInt("city_id");
                city.name = rs.getString("city_name");
                r.city = city;
                return r;
            }
        }
    }

    /** Pending → Approved. Returns 0 if it was no longer Pending. */
    public int approve(Connection c, int registrationId, int reviewedBy, LocalDateTime reviewedAt) throws SQLException {
        String sql = "UPDATE registration SET status = 'APPROVED', reviewed_by = ?, reviewed_at = ? " +
                "WHERE registration_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reviewedBy);
            ps.setObject(2, reviewedAt);
            ps.setInt(3, registrationId);
            return ps.executeUpdate();
        }
    }

    /** Pending → Rejected with the admin's reason. Returns 0 if it was no longer Pending. */
    public int reject(Connection c, int registrationId, String reason, int reviewedBy, LocalDateTime reviewedAt)
            throws SQLException {
        String sql = "UPDATE registration SET status = 'REJECTED', rejection_reason = ?, reviewed_by = ?, reviewed_at = ? " +
                "WHERE registration_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, reviewedBy);
            ps.setObject(3, reviewedAt);
            ps.setInt(4, registrationId);
            return ps.executeUpdate();
        }
    }
}
