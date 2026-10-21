package com.aniplato.service;

import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.LogDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Document;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Document verification by the admin (R1.4), and the document lists and expiry warnings the Profile screen shows. */
public class DocumentReviewService {

    private final DocumentDao documentDao = new DocumentDao();
    private final LogDao logDao = new LogDao();

    /** Returns expired and soon-to-expire verified documents for the signed-in organization. */
    public List<Document> getCurrentDocumentAlerts(LocalDate today) {
        Account current = Session.current();
        if (current == null || current.registration == null) return List.of();
        try (Connection c = Database.getConnection()) {
            return documentDao.findCurrentAlerts(c, current.registration.id, today,
                    Settings.DOCUMENT_WARNING_DAYS);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load document expiry warnings", e);
        }
    }

    /** The signed-in organization's documents for the Profile screen: the latest upload of each type. */
    public List<Document> getMyDocuments() {
        Account current = Session.require();
        if (current.registration == null) return List.of();
        try (Connection c = Database.getConnection()) {
            Map<Document.Type, Document> latest = new LinkedHashMap<>();
            for (Document d : documentDao.findByRegistration(c, current.registration.id)) latest.put(d.type, d);   // oldest first, so the newest wins
            return new ArrayList<>(latest.values());
        } catch (SQLException e) {
            throw new DataAccessException("Could not load your documents", e);
        }
    }

    /** Admin confirms a document matches the applicant's name and city (R1.4). */
    public void verifyDocument(int documentId) {
        reviewDocument(documentId, Document.Review.OK, null);
    }

    /** Admin takes a verification back, so the document awaits review again. */
    public void undoVerification(int documentId) {
        reviewDocument(documentId, null, null);
    }

    private void reviewDocument(int documentId, Document.Review review, String reason) {
        Account admin = Session.requireAdmin();
        Database.inTransaction("Could not save the document review", c -> {
            Document d = documentDao.findById(c, documentId);
            if (d == null) throw new RuleViolationException("This document no longer exists.");
            if (review == null && d.review != Document.Review.OK) throw new RuleViolationException("This document is not verified.");
            String from = d.review == null ? "AWAITING_REVIEW" : d.review.name();
            documentDao.markReviewed(c, documentId, review, reason, admin.id, LocalDateTime.now());
            logDao.logStatusChange(c, "DOCUMENT", documentId, from, review == null ? "AWAITING_REVIEW" : review.name(), reason, admin.id);   // A3
            return null;
        });
    }
}
