package com.aniplato.service;

import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.LogDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Document;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.io.File;
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
    private final AccountStatusService accountStatusService = new AccountStatusService();
    private final FileStorageService fileStorageService = new FileStorageService();

    /** Returns expired and soon-to-expire verified documents for the signed-in organization. */
    public List<Document> getCurrentDocumentAlerts(LocalDate today) {
        Account current = Session.current();
        if (current == null || current.registration == null) return List.of();
        return Database.read("Could not load document expiry warnings", c ->
                documentDao.findCurrentAlerts(c, current.registration.id, today, Settings.DOCUMENT_WARNING_DAYS));
    }

    /** The signed-in organization's documents for the Profile screen: the latest upload of each type. */
    public List<Document> getMyDocuments() {
        Account current = Session.require();
        if (current.registration == null) return List.of();
        return Database.read("Could not load your documents", c ->
                latestPerType(documentDao.findByRegistration(c, current.registration.id)));
    }

    /** The newest upload of each type, from a list oldest first: a renewal takes the place of the file it renews. */
    static List<Document> latestPerType(List<Document> oldestFirst) {
        Map<Document.Type, Document> latest = new LinkedHashMap<>();
        for (Document d : oldestFirst) latest.put(d.type, d);
        return new ArrayList<>(latest.values());
    }

    /**
     * An organization uploads a renewed document (6.3). It is a new row awaiting the admin's review; the old row is
     * untouched. Only a type the registration already has, with the same file and expiry rules as registering.
     * A Suspended account may renew (that is how a document-expiry suspension ends).
     */
    public void uploadRenewal(Document.Type type, File file, LocalDate expiresOn, boolean noExpiry) {
        Account me = Session.require();
        if (me.registration == null) throw new RuleViolationException("Only an organization can upload a renewal.");
        if (type == null) throw new RuleViolationException("Choose the document to renew.");
        fileStorageService.validate(file);
        if (expiresOn == null && (!noExpiry || type.requiresExpiry())) {
            throw new RuleViolationException("Enter the expiry date of your " + type.label() + ".");
        }
        if (expiresOn != null && expiresOn.isBefore(LocalDate.now())) {
            throw new RuleViolationException("Your " + type.label() + " has expired. Upload a current one.");
        }
        String path = fileStorageService.save(file);
        try {
            Database.inTransaction("Could not save the renewal", c -> {
                if (documentDao.findByRegistration(c, me.registration.id).stream().noneMatch(d -> d.type == type)) {
                    throw new RuleViolationException("Your registration has no " + type.label() + " to renew.");
                }
                Document d = new Document();
                d.registrationId = me.registration.id;
                d.type = type;
                d.filePath = path;
                d.expiresOn = expiresOn;
                d.uploadedAt = LocalDateTime.now();
                int id = documentDao.insert(c, d);
                logDao.logStatusChange(c, "DOCUMENT", id, null, "AWAITING_REVIEW", "Renewal uploaded", me.id);   // A3
                return null;
            });
        } catch (RuntimeException e) {
            fileStorageService.deleteAll(List.of(path));   // the database never pointed at this file
            throw e;
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
            if (review == Document.Review.OK) accountStatusService.reinstateIfRenewed(c, d.registrationId, LocalDate.now());   // 6.3
            logDao.logStatusChange(c, "DOCUMENT", documentId, from, review == null ? "AWAITING_REVIEW" : review.name(), reason, admin.id);   // A3
            return null;
        });
    }
}
