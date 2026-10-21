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
import com.aniplato.util.PasswordHasher;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.format.DateTimeFormatter;

/** Submit, review, approve and reject registrations; document review. */
public class RegistrationService {

    private static final String ACCOUNT_ENTITY = "ACCOUNT";
    private static final String REGISTRATION_ENTITY = "REGISTRATION";
    private static final String DOCUMENT_ENTITY = "DOCUMENT";
    private static final int MAX_REASON_LENGTH = 500;   // registration.rejection_reason
    private static final String DOCUMENT_EXPIRED_REASON = "Document expired";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CONTACT_NUMBER = Pattern.compile("^\\d{" + Settings.CONTACT_NUMBER_DIGITS + "}$");

    /** One file chosen on the registration form: its expiry date, or the "No expiry" box ticked. */
    public record DocumentUpload(Document.Type type, File file, LocalDate expiresOn, boolean noExpiry) { }

    private final AccountDao accountDao = new AccountDao();
    private final DocumentDao documentDao = new DocumentDao();
    private final RegistrationDao registrationDao = new RegistrationDao();
    private final CityDao cityDao = new CityDao();
    private final FileStorageService fileStorageService = new FileStorageService();
    private final EmailService emailService = new EmailService();
    private final AuthenticationService authenticationService = new AuthenticationService();
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
        if (!CONTACT_NUMBER.matcher(f.contactPhone).matches()) {
            throw new RuleViolationException("The contact number must be exactly " + Settings.CONTACT_NUMBER_DIGITS + " digits.");
        }
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
        Database.inTransaction("Could not save the registration", c -> {
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

    //---REVIEWING (admin)---

    /**
     * Approves a Pending registration (6.2 Stage 1, 6.3): every document must be unexpired and not flagged.
     * Awaiting documents become OK; a new registration gets an Active account with a temporary password (R1.5),
     * an upgrade moves the existing Casual account to it and changes its role. One transaction (A2).
     * The Approved email goes out after the commit. Returns false only if that email failed.
     */
    public boolean approve(int registrationId) {
        Account admin = Session.requireAdmin();
        String temporaryPassword = authenticationService.generateTemporaryPassword();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        Approval approval = Database.inTransaction("Could not approve the registration", c -> {
            Registration r = requirePending(c, registrationId);
            List<Document> documents = documentDao.findByRegistration(c, r.id);
            for (Document d : documents) {
                if (d.review == Document.Review.FLAGGED) {
                    throw new RuleViolationException("The " + d.type.label() + " is flagged. Reject the registration instead.");
                }
                if (d.isExpired(today)) {
                    throw new RuleViolationException("The " + d.type.label() + " has expired. Reject the registration instead.");
                }
                if (d.review != Document.Review.OK) {
                    throw new RuleViolationException("Verify the " + d.type.label() + " before approving.");   // R1.4: the admin verifies each file first
                }
            }
            for (Document d : documents) {
                if (documentDao.markOk(c, d.id, admin.id, now) > 0) {
                    logDao.logStatusChange(c, DOCUMENT_ENTITY, d.id, null, Document.Review.OK.name(), null, admin.id);
                }
            }
            registrationDao.approve(c, r.id, admin.id, now);
            logDao.logStatusChange(c, REGISTRATION_ENTITY, r.id, Registration.Status.PENDING.name(),
                    Registration.Status.APPROVED.name(), null, admin.id);

            // the email is unique to accounts, and checkDetails blocks it on a new registration, so a match is an upgrade
            Account existing = accountDao.findByEmail(c, r.email);
            if (existing == null) {
                int accountId = accountDao.insert(c, r.email, PasswordHasher.hash(temporaryPassword), r.role, r.id, now);
                logDao.logStatusChange(c, ACCOUNT_ENTITY, accountId, null, Account.Status.ACTIVE.name(), null, admin.id);
            } else if (existing.role == Account.Role.CASUAL_DONOR && r.role == Account.Role.BUSINESS_DONOR) {
                accountDao.moveToRegistration(c, existing.id, r.id, r.role);
            } else {
                // e.g. a second registration sent while the first was pending, then the first approved
                throw new RuleViolationException("This email already has an account. Reject this registration.");
            }
            return new Approval(r, existing == null);
        });
        if (!approval.newAccount()) return true;   // an upgrade keeps its password, so there is nothing to send
        // after the commit: a failed send is logged in email_log and the user can use Forgot password
        return emailService.sendApproved(approval.registration().email, approval.registration().name, temporaryPassword);
    }

    /**
     * Rejects a Pending registration with the admin's reason (6.2 Stage 1, 6.3). No account is created, and a
     * rejected upgrade changes nothing. The Not approved email, with the reason and any document flags, goes out
     * after the commit. Returns whether it was sent.
     */
    public boolean reject(int registrationId, String reason) {
        Account admin = Session.requireAdmin();
        String trimmed = trim(reason);
        if (trimmed.isEmpty()) throw new RuleViolationException("Write the reason for rejecting.");
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new RuleViolationException("Keep the reason under " + MAX_REASON_LENGTH + " characters.");
        }
        LocalDateTime now = LocalDateTime.now();
        Rejection rejection = Database.inTransaction("Could not reject the registration", c -> {
            Registration r = requirePending(c, registrationId);
            registrationDao.reject(c, r.id, trimmed, admin.id, now);
            logDao.logStatusChange(c, REGISTRATION_ENTITY, r.id, Registration.Status.PENDING.name(),
                    Registration.Status.REJECTED.name(), trimmed, admin.id);
            StringBuilder reasons = new StringBuilder(trimmed);
            for (Document d : documentDao.findByRegistration(c, r.id)) {
                if (d.review == Document.Review.FLAGGED && d.flagReason != null) {
                    reasons.append("\n").append(d.type.label()).append(": ").append(d.flagReason);
                }
            }
            return new Rejection(r, reasons.toString());
        });
        return emailService.sendNotApproved(rejection.registration().email, rejection.registration().name,
                rejection.reasons());
    }

    private record Approval(Registration registration, boolean newAccount) { }

    private record Rejection(Registration registration, String reasons) { }

    /** The registration, still Pending; another admin action may have reviewed it already. */
    private Registration requirePending(Connection c, int registrationId) throws SQLException {
        Registration r = registrationDao.findById(c, registrationId);
        if (r == null) throw new RuleViolationException("This registration no longer exists.");
        if (r.status != Registration.Status.PENDING) {
            throw new RuleViolationException("This registration was already reviewed.");
        }
        return r;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Maintenance suspension for registrations with a current, verified expired document (6.6, A3). */
    public void suspendAccountsWithExpiredDocuments(LocalDate today) {
        Database.inTransaction("Could not suspend accounts with expired documents", c -> {
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

    //---ADMIN: REVIEW QUEUE (Stage 1 review)---

    /** One line of the Pending Accounts list. */
    public record PendingRow(int registrationId, String name, Account.Role role, String city,
                             LocalDateTime submittedAt, boolean registeredAgain) { }

    /** A registration with its documents, for the Review screen. */
    public record ReviewDetail(Registration registration, boolean registeredAgain) { }

    /** Pending registrations, oldest first. */
    public List<PendingRow> getPending() {
        Session.requireAdmin();
        try (Connection c = Database.getConnection()) {
            List<PendingRow> rows = new ArrayList<>();
            for (Registration r : registrationDao.findByStatus(c, Registration.Status.PENDING)) {
                boolean again = registrationDao.countEarlierRejected(c, r.email, r.submittedAt) > 0;
                rows.add(new PendingRow(r.id, r.name, r.role, r.city.name, r.submittedAt, again));
            }
            return rows;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load pending registrations", e);
        }
    }

    /** One registration with all its documents. */
    public ReviewDetail getForReview(int registrationId) {
        Session.requireAdmin();
        try (Connection c = Database.getConnection()) {
            Registration r = registrationDao.findById(c, registrationId);
            if (r == null) throw new RuleViolationException("This registration no longer exists.");
            r.documents = documentDao.findByRegistration(c, registrationId);
            return new ReviewDetail(r, registrationDao.countEarlierRejected(c, r.email, r.submittedAt) > 0);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the registration", e);
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
    //---ADMIN: ALL ACCOUNTS---

    /** One line of the All Accounts list. */
    public record AccountRow(int accountId, String name, Account.Role role, String city, Account.Status status,
                             String statusNote, String documentNote, boolean documentEnding) { }

    /** The list with the numbers above it. Counts are worked out from the rows (A9). */
    public record AccountsOverview(List<AccountRow> rows, int approved, int suspended, int documentsEnding) { }

    /** Every organization account, with the next document that ends within the warning period. */
    public AccountsOverview getAccounts(LocalDate today) {
        Session.requireAdmin();
        try (Connection c = Database.getConnection()) {
            Map<Integer, DocumentDao.ExpiringDocument> next = new HashMap<>();
            for (DocumentDao.ExpiringDocument d : documentDao.findEndingBy(c, today.plusDays(Settings.DOCUMENT_WARNING_DAYS))) {
                next.putIfAbsent(d.registrationId(), d);   // the list is soonest first
            }
            List<AccountRow> rows = new ArrayList<>();
            int suspended = 0;
            int ending = 0;
            for (Account a : accountDao.findAllOrganizations(c)) {
                DocumentDao.ExpiringDocument doc = next.get(a.registration.id);
                if (doc != null) ending++;
                boolean isSuspended = a.status == Account.Status.SUSPENDED;
                if (isSuspended) suspended++;
                rows.add(new AccountRow(a.id, a.registration.name, a.role, a.registration.city.name, a.status,
                        a.suspendReason, documentNote(doc, today), doc != null));
            }
            return new AccountsOverview(rows, rows.size(), suspended, ending);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load accounts", e);
        }
    }

    private static String documentNote(DocumentDao.ExpiringDocument doc, LocalDate today) {
        if (doc == null) return "Documents current";
        String date = doc.expiresOn().format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
        return doc.type().label() + (doc.expiresOn().isBefore(today) ? " ended " : " ends ") + date;
    }

    /** Admin suspends an Active account; the reason is shown to the organization (6.3). */
    public void suspendAccount(int accountId, String reason) {
        Account admin = Session.requireAdmin();
        if (reason == null || reason.isBlank()) throw new RuleViolationException("Write the reason for the suspension.");
        String why = reason.trim();
        Database.inTransaction("Could not suspend the account", c -> {
            if (accountDao.suspendIfActive(c, accountId, why) == 0) {
                throw new RuleViolationException("This account is not Active.");
            }
            logDao.logStatusChange(c, ACCOUNT_ENTITY, accountId, Account.Status.ACTIVE.name(),
                    Account.Status.SUSPENDED.name(), why, admin.id);   // A3
            return null;
        });
    }

    /** Admin gives a Suspended account its full access back. */
    public void reinstateAccount(int accountId) {
        Account admin = Session.requireAdmin();
        Database.inTransaction("Could not reinstate the account", c -> {
            if (accountDao.reinstateIfSuspended(c, accountId) == 0) {
                throw new RuleViolationException("This account is not Suspended.");
            }
            logDao.logStatusChange(c, ACCOUNT_ENTITY, accountId, Account.Status.SUSPENDED.name(),
                    Account.Status.ACTIVE.name(), null, admin.id);   // A3
            return null;
        });
    }
}