package com.aniplato.dao;

import com.aniplato.model.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

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

    /** Every file uploaded with a registration, oldest first. */
    public List<Document> findByRegistration(Connection c, int registrationId) throws SQLException {
        String sql = "SELECT * FROM document WHERE registration_id = ? ORDER BY uploaded_at, document_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Document> documents = new ArrayList<>();
                while (rs.next()) documents.add(map(rs));
                return documents;
            }
        }
    }

    /** Awaiting review → OK. Returns 0 if it was already reviewed. */
    public int markOk(Connection c, int documentId, int reviewedBy, LocalDateTime reviewedAt) throws SQLException {
        String sql = "UPDATE document SET review = 'OK', reviewed_by = ?, reviewed_at = ? " +
                "WHERE document_id = ? AND review IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reviewedBy);
            ps.setObject(2, reviewedAt);
            ps.setInt(3, documentId);
            return ps.executeUpdate();
        }
    }

    /** Saves an uploaded file, awaiting the admin's review. */
    public void insert(Connection c, Document d) throws SQLException {
        String sql = "INSERT INTO document (registration_id, doc_type, file_path, expires_on, uploaded_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, d.registrationId);
            ps.setString(2, d.type.dbValue());
            ps.setString(3, d.filePath);
            ps.setObject(4, d.expiresOn);
            ps.setObject(5, d.uploadedAt);
            ps.executeUpdate();
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

    /** A document in force that has expired or expires soon, for the All Accounts note. */
    public record ExpiringDocument(int registrationId, Document.Type type, LocalDate expiresOn) { }

    public List<Document> findByRegistration(Connection c, int registrationId) throws SQLException {
        String sql = "SELECT * FROM document WHERE registration_id = ? ORDER BY uploaded_at, document_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Document> documents = new ArrayList<>();
                while (rs.next()) documents.add(map(rs));
                return documents;
            }
        }
    }

    public Document findById(Connection c, int documentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM document WHERE document_id = ?")) {
            ps.setInt(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** The admin's verdict on one file: OK, or FLAGGED with a reason. */
    public int markReviewed(Connection c, int documentId, Document.Review review, String flagReason, int adminId,
                            LocalDateTime reviewedAt) throws SQLException {
        String sql = "UPDATE document SET review = ?, flag_reason = ?, reviewed_by = ?, reviewed_at = ? " +
                "WHERE document_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, review == null ? null : review.name());   // null = back to awaiting review (Undo)
            ps.setString(2, flagReason);
            ps.setInt(3, adminId);
            ps.setObject(4, reviewedAt);
            ps.setInt(5, documentId);
            return ps.executeUpdate();
        }
    }

    /** Verified documents in force (not replaced by a newer verified one) that end on or before the given date. */
    public List<ExpiringDocument> findEndingBy(Connection c, LocalDate lastDay) throws SQLException {
        String sql = """
                SELECT d.registration_id, d.doc_type, d.expires_on
                FROM document d
                WHERE d.review = 'OK' AND d.expires_on IS NOT NULL AND d.expires_on <= ?
                  AND NOT EXISTS (
                      SELECT 1 FROM document newer
                      WHERE newer.registration_id = d.registration_id AND newer.doc_type = d.doc_type
                        AND newer.review = 'OK' AND newer.uploaded_at > d.uploaded_at)
                ORDER BY d.expires_on
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, lastDay);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExpiringDocument> found = new ArrayList<>();
                while (rs.next()) {
                    found.add(new ExpiringDocument(rs.getInt("registration_id"),
                            Document.Type.fromDb(rs.getString("doc_type")), rs.getDate("expires_on").toLocalDate()));
                }
                return found;
            }
        }
    }
}
