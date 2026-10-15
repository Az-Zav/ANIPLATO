package com.aniplato.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;

/** SQL only. One DAO per table (Log = status_change + email_log). */
public class LogDao {

    public void logStatusChange(Connection c, String entityType, int entityId, String fromStatus,
                                String toStatus, String reason, Integer changedBy) throws SQLException {
        String sql = "INSERT INTO status_change " +
                "(entity_type, entity_id, from_status, to_status, reason, changed_by, changed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);
            ps.setString(3, fromStatus);
            ps.setString(4, toStatus);
            ps.setString(5, reason);
            if (changedBy == null) ps.setNull(6, Types.INTEGER);
            else ps.setInt(6, changedBy);
            ps.setObject(7, LocalDateTime.now());
            ps.executeUpdate();
        }
    }

    public void logEmail(Connection c, String toEmail, String kind, LocalDateTime sentAt,
                         boolean success, String error) throws SQLException {
        String sql = "INSERT INTO email_log (to_email, kind, sent_at, success, error) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, toEmail);
            ps.setString(2, kind);
            ps.setObject(3, sentAt);
            ps.setBoolean(4, success);
            ps.setString(5, error);
            ps.executeUpdate();
        }
    }
}
