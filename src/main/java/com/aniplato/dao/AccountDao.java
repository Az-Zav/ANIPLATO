package com.aniplato.dao;

import com.aniplato.model.Account;
import com.aniplato.model.Admin;
import com.aniplato.model.City;
import com.aniplato.model.Donor;
import com.aniplato.model.FoodGroup;
import com.aniplato.model.Registration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/** SQL only. One DAO per table. */
public class AccountDao {

    public Account findByEmail(Connection c, String email) throws SQLException {
        String sql = """
                SELECT a.account_id, a.email, a.role, a.status, a.suspend_reason,
                       a.must_change_password, a.password_changed_at, a.created_at, a.admin_name,
                       r.registration_id, r.name AS registration_name, r.contact_person, r.email AS registration_email,
                       r.status AS registration_status, r.rejection_reason, r.reviewed_by, r.reviewed_at,
                       r.submitted_at, city.city_id, city.name AS city_name
                FROM account a
                LEFT JOIN registration r ON r.registration_id = a.registration_id
                LEFT JOIN city ON city.city_id = r.city_id
                WHERE a.email = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Account.Role role = Account.Role.valueOf(rs.getString("role"));
                Account account = switch (role) {
                    case ADMIN -> new Admin();
                    case BUSINESS_DONOR, CASUAL_DONOR -> new Donor();
                    case FOOD_GROUP -> new FoodGroup();
                };
                account.id = rs.getInt("account_id");
                account.email = rs.getString("email");
                account.role = role;
                account.status = Account.Status.valueOf(rs.getString("status"));
                account.suspendReason = rs.getString("suspend_reason");
                account.mustChangePassword = rs.getBoolean("must_change_password");
                account.passwordChangedAt = rs.getTimestamp("password_changed_at").toLocalDateTime();
                account.createdAt = rs.getTimestamp("created_at").toLocalDateTime();

                if (account instanceof Admin admin) {
                    admin.adminName = rs.getString("admin_name");
                } else {
                    Registration registration = new Registration();
                    registration.id = rs.getInt("registration_id");
                    registration.role = role;
                    registration.name = rs.getString("registration_name");
                    registration.contactPerson = rs.getString("contact_person");
                    registration.email = rs.getString("registration_email");
                    registration.status = Registration.Status.valueOf(rs.getString("registration_status"));
                    registration.rejectionReason = rs.getString("rejection_reason");
                    int reviewedBy = rs.getInt("reviewed_by");
                    registration.reviewedBy = rs.wasNull() ? null : reviewedBy;
                    LocalDateTime reviewedAt = localDateTime(rs, "reviewed_at");
                    registration.reviewedAt = reviewedAt;
                    registration.submittedAt = rs.getTimestamp("submitted_at").toLocalDateTime();

                    City city = new City();
                    city.id = rs.getInt("city_id");
                    city.name = rs.getString("city_name");
                    registration.city = city;
                    account.registration = registration;
                }
                return account;
            }
        }
    }

    /** True if a login already uses this email (an account email is unique). */
    public boolean existsByEmail(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM account WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public String findPasswordHash(Connection c, int accountId) throws SQLException {
        String sql = "SELECT password_hash FROM account WHERE account_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("password_hash") : null;
            }
        }
    }

    public int updatePassword(Connection c, int accountId, String passwordHash, LocalDateTime changedAt)
            throws SQLException {
        String sql = "UPDATE account SET password_hash = ?, must_change_password = FALSE, password_changed_at = ? " +
                "WHERE account_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setObject(2, changedAt);
            ps.setInt(3, accountId);
            return ps.executeUpdate();
        }
    }

    /** Replaces the password with a temporary one; login then reaches only Change Password (R1.7). */
    public int setTemporaryPassword(Connection c, int accountId, String passwordHash) throws SQLException {
        String sql = "UPDATE account SET password_hash = ?, must_change_password = TRUE WHERE account_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    /** Creates the login of an approved registration with a temporary password (R1.5) and returns its id. */
    public int insert(Connection c, String email, String passwordHash, Account.Role role, int registrationId,
                      LocalDateTime createdAt) throws SQLException {
        String sql = "INSERT INTO account (email, password_hash, role, status, must_change_password, " +
                "password_changed_at, registration_id, created_at) VALUES (?, ?, ?, 'ACTIVE', TRUE, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            ps.setObject(4, createdAt);
            ps.setInt(5, registrationId);
            ps.setObject(6, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** An approved upgrade: the account points to the new registration and takes its role (6.2 Stage 1). */
    public int moveToRegistration(Connection c, int accountId, int registrationId, Account.Role role) throws SQLException {
        String sql = "UPDATE account SET registration_id = ?, role = ? WHERE account_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            ps.setString(2, role.name());
            ps.setInt(3, accountId);
            return ps.executeUpdate();
        }
    }

    public int suspendIfActive(Connection c, int accountId, String reason) throws SQLException {
        String sql = "UPDATE account SET status = 'SUSPENDED', suspend_reason = ? " +
                "WHERE account_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        }
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
