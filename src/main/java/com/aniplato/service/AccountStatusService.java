package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.LogDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Suspend and reinstate accounts (6.3), the document-expiry suspension (6.6), and the admin's All Accounts list. */
public class AccountStatusService {

    private static final String ACCOUNT_ENTITY = "ACCOUNT";
    private static final String DOCUMENT_EXPIRED_REASON = "Document expired";

    private final AccountDao accountDao = new AccountDao();
    private final DocumentDao documentDao = new DocumentDao();
    private final LogDao logDao = new LogDao();

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
}
