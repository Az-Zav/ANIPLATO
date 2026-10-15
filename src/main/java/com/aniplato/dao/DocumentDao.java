package com.aniplato.dao;

import com.aniplato.model.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table. */
public class DocumentDao {

    public record ExpiredRegistration(int accountId, int registrationId) { }

    public List<ExpiredRegistration> findCurrentExpiredRegistrations(Connection c, LocalDate today)
            throws SQLException {
        String sql = """
                SELECT DISTINCT a.account_id, a.registration_id
                FROM account a
                JOIN document d ON d.registration_id = a.registration_id
                WHERE a.status = 'ACTIVE'
                  AND d.review = 'OK'
                  AND d.expires_on < ?
                  AND NOT EXISTS (
                      SELECT 1 FROM document newer
                      WHERE newer.registration_id = d.registration_id
                        AND newer.doc_type = d.doc_type
                        AND newer.review = 'OK'
                        AND (newer.uploaded_at > d.uploaded_at
                             OR (newer.uploaded_at = d.uploaded_at AND newer.document_id > d.document_id))
                  )
                ORDER BY a.account_id
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, today);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExpiredRegistration> registrations = new ArrayList<>();
                while (rs.next()) {
                    registrations.add(new ExpiredRegistration(
                            rs.getInt("account_id"), rs.getInt("registration_id")));
                }
                return registrations;
            }
        }
    }

    public List<Document> findCurrentAlerts(Connection c, int registrationId, LocalDate today, int warningDays)
            throws SQLException {
        String sql = """
                SELECT d.*
                FROM document d
                WHERE d.registration_id = ?
                  AND d.review = 'OK'
                  AND (d.expires_on < ? OR d.expires_on <= DATE_ADD(?, INTERVAL ? DAY))
                  AND NOT EXISTS (
                      SELECT 1 FROM document newer
                      WHERE newer.registration_id = d.registration_id
                        AND newer.doc_type = d.doc_type
                        AND newer.review = 'OK'
                        AND (newer.uploaded_at > d.uploaded_at
                             OR (newer.uploaded_at = d.uploaded_at AND newer.document_id > d.document_id))
                  )
                ORDER BY d.expires_on, d.document_id
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            ps.setObject(2, today);
            ps.setObject(3, today);
            ps.setInt(4, warningDays);
            try (ResultSet rs = ps.executeQuery()) {
                List<Document> documents = new ArrayList<>();
                while (rs.next()) documents.add(map(rs));
                return documents;
            }
        }
    }

    private Document map(ResultSet rs) throws SQLException {
        Document document = new Document();
        document.id = rs.getInt("document_id");
        document.registrationId = rs.getInt("registration_id");
        document.type = Document.Type.fromDb(rs.getString("doc_type"));
        document.filePath = rs.getString("file_path");
        document.expiresOn = rs.getObject("expires_on", LocalDate.class);
        String review = rs.getString("review");
        document.review = review == null ? null : Document.Review.valueOf(review);
        document.flagReason = rs.getString("flag_reason");
        document.uploadedAt = rs.getTimestamp("uploaded_at").toLocalDateTime();
        int reviewerId = rs.getInt("reviewed_by");
        document.reviewedBy = rs.wasNull() ? null : reviewerId;
        if (rs.getTimestamp("reviewed_at") != null) {
            document.reviewedAt = rs.getTimestamp("reviewed_at").toLocalDateTime();
        }
        return document;
    }
}
