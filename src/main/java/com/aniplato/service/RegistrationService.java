package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.LogDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.model.Account;
import com.aniplato.model.Document;
import com.aniplato.util.Database;
import com.aniplato.util.Settings;
import com.aniplato.util.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Submit, review, approve and reject registrations; document review. */
public class RegistrationService {

    private static final String ACCOUNT_ENTITY = "ACCOUNT";
    private static final String DOCUMENT_EXPIRED_REASON = "Document expired";

    private final AccountDao accountDao = new AccountDao();
    private final DocumentDao documentDao = new DocumentDao();
    private final LogDao logDao = new LogDao();

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
