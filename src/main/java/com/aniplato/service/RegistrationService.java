package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.CityDao;
import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.LogDao;
import com.aniplato.dao.RegistrationDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.City;
import com.aniplato.model.Document;
import com.aniplato.model.Registration;
import com.aniplato.util.Database;
import com.aniplato.util.Settings;
import com.aniplato.util.Session;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Submit, review, approve and reject registrations; document review. */
public class RegistrationService {

    private static final String ACCOUNT_ENTITY = "ACCOUNT";
    private static final String REGISTRATION_ENTITY = "REGISTRATION";
    private static final String DOCUMENT_EXPIRED_REASON = "Document expired";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** One file chosen on the registration form: its expiry date, or the "No expiry" box ticked. */
    public record DocumentUpload(Document.Type type, File file, LocalDate expiresOn, boolean noExpiry) { }

    private final AccountDao accountDao = new AccountDao();
    private final DocumentDao documentDao = new DocumentDao();
    private final RegistrationDao registrationDao = new RegistrationDao();
    private final CityDao cityDao = new CityDao();
    private final FileStorageService fileStorageService = new FileStorageService();
    private final EmailService emailService = new EmailService();
    private final LogDao logDao = new LogDao();

    //---APPLYING (Stage 1)---

    /** Cities for the registration form. */
    public List<City> cities() {
        try (Connection c = Database.getConnection()) {
            return cityDao.findAll(c);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load cities", e);
        }
    }

    /** Step 1 rules: every detail present, the role chosen, and the email not already an account. */
    public void checkDetails(Registration f) {
        if (f.role == null) {
            throw new RuleViolationException("Say whether you have a Mayor's Permit and a Sanitary Permit.");
        }
        f.name = trim(f.name);
        f.contactPerson = trim(f.contactPerson);
        f.contactPhone = trim(f.contactPhone);
        f.email = trim(f.email);
        if (f.name.isEmpty()) throw new RuleViolationException("Enter your name or organization name.");
        if (f.city == null) throw new RuleViolationException("Choose your city.");
        if (f.contactPerson.isEmpty()) throw new RuleViolationException("Enter the contact person.");
        if (f.contactPhone.isEmpty()) throw new RuleViolationException("Enter a contact number.");
        if (!EMAIL.matcher(f.email).matches()) throw new RuleViolationException("Enter a valid email address.");
        try (Connection c = Database.getConnection()) {
            if (accountDao.existsByEmail(c, f.email)) {
                throw new RuleViolationException("This email already has an account. Log in, or use a different email.");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the email", e);
        }
    }

    /**
     * Saves a Pending registration with its documents (Stage 1) and sends the Received email.
     * The files, the registration, the documents and the status log are one transaction (A2).
     */
    public void submit(Registration f, List<DocumentUpload> uploads, boolean consent) {
        if (!consent) throw new RuleViolationException("Agree to the consent text to continue.");
        checkDetails(f);
        checkDocuments(f.role, uploads);
        LocalDateTime now = LocalDateTime.now();
        inTransaction("Could not save the registration", c -> {
            f.id = registrationDao.insert(c, f, now);
            for (DocumentUpload upload : uploads) {
                Document d = new Document();
                d.registrationId = f.id;
                d.type = upload.type();
                d.filePath = fileStorageService.save(upload.file());
                d.expiresOn = upload.expiresOn();
                d.uploadedAt = now;
                documentDao.insert(c, d);
            }
            logDao.logStatusChange(c, REGISTRATION_ENTITY, f.id, null, Registration.Status.PENDING.name(), null, null);
            return null;
        });
        emailService.sendReceived(f.email, f.name);   // after the commit: an email failure never undoes the registration
    }

    /** The documents each role must upload, one file each, and the expiry rules (Stage 1). */
    private void checkDocuments(Account.Role role, List<DocumentUpload> uploads) {
        List<Document.Type> required = new ArrayList<>();
        switch (role) {
            case BUSINESS_DONOR -> required.addAll(List.of(Document.Type.MAYORS_PERMIT, Document.Type.SANITARY_PERMIT));
            case CASUAL_DONOR -> required.add(Document.Type.BARANGAY_CERTIFICATE);
            case FOOD_GROUP -> {
                Document.Type credential = uploads.stream().map(DocumentUpload::type)
                        .filter(Document.Type.FOOD_GROUP_CREDENTIALS::contains).findFirst().orElse(null);
                if (credential == null) throw new RuleViolationException("Choose a credential and upload its file.");
                required.addAll(List.of(credential, Document.Type.AUTHORIZATION_LETTER));
            }
            default -> throw new IllegalArgumentException("Only donors and food groups register: " + role);
        }
        for (Document.Type type : required) {
            if (uploads.stream().noneMatch(u -> u.type() == type)) {
                throw new RuleViolationException("Upload your " + type.label() + ".");
            }
        }
        if (uploads.size() != required.size()) {
            throw new RuleViolationException("Upload only the documents required for your registration.");
        }
        LocalDate today = LocalDate.now();
        for (DocumentUpload upload : uploads) {
            fileStorageService.validate(upload.file());
            // "No expiry" only where the document has none; permits and the Barangay Certificate always need a date
            if (upload.expiresOn() == null && (!upload.noExpiry() || upload.type().requiresExpiry())) {
                throw new RuleViolationException("Enter the expiry date of your " + upload.type().label() + ".");
            }
            if (upload.expiresOn() != null && upload.expiresOn().isBefore(today)) {
                throw new RuleViolationException("Your " + upload.type().label() + " has expired. Upload a current one.");
            }
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Maintenance suspension for registrations with a current, verified expired document (6.6, A3). */
    public void suspendAccountsWithExpiredDocuments(LocalDate today) {
        inTransaction("Could not suspend accounts with expired documents", c -> {
            List<DocumentDao.ExpiredRegistration> expired = documentDao.findCurrentExpiredRegistrations(c, today);
            for (DocumentDao.ExpiredRegistration registration : expired) {
                if (accountDao.suspendIfActive(c, registration.accountId(), DOCUMENT_EXPIRED_REASON) > 0) {
                    logDao.logStatusChange(c, ACCOUNT_ENTITY, registration.accountId(),
                            Account.Status.ACTIVE.name(), Account.Status.SUSPENDED.name(),
                            DOCUMENT_EXPIRED_REASON, null);
                }
            }
            return null;
        });
    }

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

    /** Used by SafetyService so every suspension remains owned by this service (A3, A4). */
    public void suspend(Connection c, int accountId, String reason, int changedBy) throws SQLException {
        suspend(c, accountId, reason, Integer.valueOf(changedBy));
    }

    private void suspend(Connection c, int accountId, String reason, Integer changedBy) throws SQLException {
        if (accountDao.suspendIfActive(c, accountId, reason) > 0) {
            logDao.logStatusChange(c, ACCOUNT_ENTITY, accountId, Account.Status.ACTIVE.name(),
                    Account.Status.SUSPENDED.name(), reason, changedBy);
        }
    }

    private <T> T inTransaction(String message, Work<T> work) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(message, e);
        }
    }

    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }
}
