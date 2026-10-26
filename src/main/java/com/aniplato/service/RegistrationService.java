package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.CityDao;
import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.LogDao;
import com.aniplato.dao.RegistrationDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.City;
import com.aniplato.model.Document;
import com.aniplato.model.DocumentUpload;
import com.aniplato.model.Registration;
import com.aniplato.util.Database;
import com.aniplato.util.PasswordHasher;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An organization's account from application to suspension (spec 7.3): applying and the admin's decision (stage 1),
 * then what happens to the account after it exists: suspend and reinstate, document verification and renewal, the
 * contact edit and the Casual-to-Business upgrade (6.3).
 */
public class RegistrationService {

    private static final String ACCOUNT_ENTITY = "ACCOUNT";
    private static final String REGISTRATION_ENTITY = "REGISTRATION";
    private static final String DOCUMENT_ENTITY = "DOCUMENT";
    private static final String DOCUMENT_EXPIRED_REASON = "Document expired";
    private static final int MAX_REASON_LENGTH = 500;   // registration.rejection_reason
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CONTACT_NUMBER = Pattern.compile("^\\d{" + Settings.CONTACT_NUMBER_DIGITS + "}$");

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
        return Database.read("Could not load cities", cityDao::findAll);
    }

    /** The contact number rule (Stage 1, and the profile edit): present and exactly the right number of digits. */
    static void requireContactNumber(String phone) {
        if (phone.isEmpty()) throw new RuleViolationException("Enter a contact number.");
        if (!CONTACT_NUMBER.matcher(phone).matches()) {
            throw new RuleViolationException("The contact number must be exactly " + Settings.CONTACT_NUMBER_DIGITS + " digits.");
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
        requireContactNumber(f.contactPhone);
        if (!EMAIL.matcher(f.email).matches()) throw new RuleViolationException("Enter a valid email address.");
        if (Database.read("Could not check the email", c -> accountDao.existsByEmail(c, f.email))) {
            throw new RuleViolationException("This email already has an account. Log in, or use a different email.");
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
        save(f, uploads, null);
        emailService.sendReceived(f.email, f.name);   // after the commit: an email failure never undoes the registration
    }

    /**
     * Saves a Pending registration with its documents and the status log in one transaction. {@code changedBy} is the
     * account of an upgrade, null for a new applicant who has no account yet.
     */
    void save(Registration f, List<DocumentUpload> uploads, Integer changedBy) {
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
            logDao.logStatusChange(c, REGISTRATION_ENTITY, f.id, null, Registration.Status.PENDING.name(), null, changedBy);
            return null;
        });
    }

    /** The documents each role must upload, one file each, and the expiry rules (Stage 1). */
    void checkDocuments(Account.Role role, List<DocumentUpload> uploads) {
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
            requireApprovable(documents, today);
            for (Document d : documents) {
                if (documentDao.markOk(c, d.id, admin.id, now) > 0) {
                    logDao.logStatusChange(c, DOCUMENT_ENTITY, d.id, null, Document.Review.OK.name(), null, admin.id);
                }
            }
            registrationDao.approve(c, r.id, admin.id, now);
            logDao.logStatusChange(c, REGISTRATION_ENTITY, r.id, Registration.Status.PENDING.name(),
                    Registration.Status.APPROVED.name(), null, admin.id);
            return new Approval(r, createOrUpgradeAccount(c, r, PasswordHasher.hash(temporaryPassword), admin.id, now));
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

    /** Every file must be verified (R1.4), not flagged and not ended before the registration can be approved. */
    private static void requireApprovable(List<Document> documents, LocalDate today) {
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
    }

    /** Opens the account for an approved registration, or moves a Casual Donor's account to a Business registration. True if the account is new. */
    private boolean createOrUpgradeAccount(Connection c, Registration r, String passwordHash, int adminId, LocalDateTime now)
            throws SQLException {
        // the email is unique to accounts, and checkDetails blocks it on a new registration, so a match is an upgrade
        Account existing = accountDao.findByEmail(c, r.email);
        if (existing == null) {
            int accountId = accountDao.insert(c, r.email, passwordHash, r.role, r.id, now);
            logDao.logStatusChange(c, ACCOUNT_ENTITY, accountId, null, Account.Status.ACTIVE.name(), null, adminId);
        } else if (existing.role == Account.Role.CASUAL_DONOR && r.role == Account.Role.BUSINESS_DONOR) {
            accountDao.moveToRegistration(c, existing.id, r.id, r.role);
        } else {
            // e.g. a second registration sent while the first was pending, then the first approved
            throw new RuleViolationException("This email already has an account. Reject this registration.");
        }
        return existing == null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
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
        return Database.read("Could not load pending registrations", c -> {
            List<PendingRow> rows = new ArrayList<>();
            for (Registration r : registrationDao.findByStatus(c, Registration.Status.PENDING)) {
                boolean again = registrationDao.countEarlierRejected(c, r.email, r.submittedAt) > 0;
                rows.add(new PendingRow(r.id, r.name, r.role, r.city.name, r.submittedAt, again));
            }
            return rows;
        });
    }

    /** One registration with all its documents. */
    public ReviewDetail getForReview(int registrationId) {
        Session.requireAdmin();
        return Database.read("Could not load the registration", c -> {
            Registration r = registrationDao.findById(c, registrationId);
            if (r == null) throw new RuleViolationException("This registration no longer exists.");
            r.documents = documentDao.findByRegistration(c, registrationId);
            // an approved registration may have renewals: show the newest file of each type, so the admin verifies the renewal
            if (r.status == Registration.Status.APPROVED) r.documents = RegistrationService.latestPerType(r.documents);
            return new ReviewDetail(r, registrationDao.countEarlierRejected(c, r.email, r.submittedAt) > 0);
        });
    }

    //---SUSPEND, REINSTATE AND EXPIRED DOCUMENTS (6.3)---

    //---SUSPEND AND REINSTATE---

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

    /**
     * Spec 6.3: a verified renewal brings back an account suspended for an expired document, once no verified
     * document of that registration is still expired. Runs inside the verifier's transaction; logged by the system (A3).
     */
    public void reinstateIfRenewed(Connection c, int registrationId, LocalDate today) throws SQLException {
        if (documentDao.countCurrentExpired(c, registrationId, today) > 0) return;
        Integer accountId = accountDao.findSuspendedBy(c, registrationId, DOCUMENT_EXPIRED_REASON);
        if (accountId != null && accountDao.reinstateIfSuspended(c, accountId) > 0) {
            logDao.logStatusChange(c, ACCOUNT_ENTITY, accountId, Account.Status.SUSPENDED.name(),
                    Account.Status.ACTIVE.name(), "Renewal verified", null);
        }
    }

    //---ADMIN: ALL ACCOUNTS---

    /** One line of the All Accounts list. */
    public record AccountRow(int accountId, String name, Account.Role role, String city, Account.Status status,
                             String statusNote, String documentNote, boolean documentEnding, int registrationId,
                             boolean renewalToReview) { }

    /** The list with the numbers above it. Counts are worked out from the rows (A9). */
    public record AccountsOverview(List<AccountRow> rows, int approved, int suspended, int documentsEnding) { }

    /** Every organization account, with the next document that ends within the warning period. */
    public AccountsOverview getAccounts(LocalDate today) {
        Session.requireAdmin();
        return Database.read("Could not load accounts", c -> {
            Set<Integer> awaiting = documentDao.findRegistrationIdsAwaitingReview(c);
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
                        a.suspendReason, documentNote(doc, today), doc != null, a.registration.id,
                        awaiting.contains(a.registration.id)));
            }
            return new AccountsOverview(rows, rows.size(), suspended, ending);
        });
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

    //---DOCUMENTS: VERIFY AND RENEW (6.3)---

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
            if (review == Document.Review.OK) reinstateIfRenewed(c, d.registrationId, LocalDate.now());   // 6.3
            logDao.logStatusChange(c, "DOCUMENT", documentId, from, review == null ? "AWAITING_REVIEW" : review.name(), reason, admin.id);   // A3
            return null;
        });
    }

    //---PROFILE: CONTACT EDIT AND UPGRADE (6.3)---

    /** The logged-in organization's own contact number, to prefill the edit form. Only the owner asks (A7). */
    public String getMyPhone() {
        Account me = Session.require();
        return Database.inTransaction("Could not read your details", c -> accountDao.findContactPhone(c, me.id));
    }

    /** Changes the contact person and phone of the logged-in organization. The phone follows the registration rule (A7). */
    public void updateContact(String contactPerson, String contactPhone) {
        Account me = Session.require();
        if (me.registration == null) throw new RuleViolationException("Only an organization has contact details.");
        String person = contactPerson == null ? "" : contactPerson.trim();
        String phone = contactPhone == null ? "" : contactPhone.trim();
        if (person.isEmpty()) throw new RuleViolationException("Enter the contact person.");
        RegistrationService.requireContactNumber(phone);
        Database.inTransaction("Could not save your contact details", c -> {
            registrationDao.updateContact(c, me.registration.id, person, phone);
            return null;
        });
        me.registration.contactPerson = person;   // the session shows the saved name; it never holds the phone (A7)
    }

    /**
     * A Casual donor uploads both permits to become a Business donor (6.2 Stage 1). It creates a new Pending
     * registration for the same email and details, reviewed like any other; approval changes the role. Until then
     * nothing changes, and a rejected upgrade changes nothing.
     */
    public void submitUpgrade(List<DocumentUpload> uploads, boolean consent) {
        Account me = Session.require();
        if (!me.canUpgradeToBusiness()) throw new RuleViolationException("Only a Casual donor can upgrade to Business.");
        if (!consent) throw new RuleViolationException("Agree to the consent text to continue.");
        checkDocuments(Account.Role.BUSINESS_DONOR, uploads);
        boolean waiting = Database.inTransaction("Could not check for an earlier upgrade", c ->
                registrationDao.findByStatus(c, Registration.Status.PENDING).stream().anyMatch(r -> r.email.equalsIgnoreCase(me.email)));
        if (waiting) throw new RuleViolationException("Your upgrade is already waiting for review.");
        Registration upgrade = new Registration();
        upgrade.role = Account.Role.BUSINESS_DONOR;
        upgrade.name = me.registration.name;
        upgrade.city = me.registration.city;
        upgrade.contactPerson = me.registration.contactPerson;
        upgrade.contactPhone = Database.inTransaction("Could not read your details", c -> accountDao.findContactPhone(c, me.id));   // the owner may read its own number (A7)
        upgrade.email = me.email;
        save(upgrade, uploads, me.id);
        emailService.sendReceived(upgrade.email, upgrade.name);   // after the commit, like a new registration
    }
}
