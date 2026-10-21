package com.aniplato.service;

import com.aniplato.dao.ListingDao;
import com.aniplato.dao.LogDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.SafetyNoticeDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Listing;
import com.aniplato.model.Offer;
import com.aniplato.model.SafetyNotice;
import com.aniplato.util.Database;
import com.aniplato.util.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Raise and resolve safety notices (R7.1). */
public class SafetyService {

    private static final String ENTITY = "NOTICE";   // status_change.entity_type
    private static final int MAX_TEXT_LENGTH = 500;   // safety_notice.concern and admin_note

    private final SafetyNoticeDao noticeDao = new SafetyNoticeDao();
    private final OfferDao offerDao = new OfferDao();
    private final ListingDao listingDao = new ListingDao();
    private final LogDao logDao = new LogDao();
    private final RegistrationService registrationService = new RegistrationService();

    /**
     * R7.1: either side of an Accepted or Completed offer flags a concern about the other side.
     * A Suspended account may still flag (6.3). Returns the new notice id.
     */
    public int raise(int offerId, String concern) {
        Account me = Session.require();
        String text = requireText(concern, "Describe the concern.");

        return inTransaction(c -> {
            Offer offer = offerDao.findById(c, offerId);
            if (offer == null) throw new RuleViolationException("That offer no longer exists.");
            if (offer.status != Offer.Status.ACCEPTED && offer.status != Offer.Status.COMPLETED) {
                throw new RuleViolationException("A concern can be raised only on an accepted or completed offer.");
            }
            int posterId = listingDao.findById(c, offer.listingId).postedBy;
            if (me.id != offer.groupId && me.id != posterId) {
                throw new RuleViolationException("Only the donor or the food group of this offer can raise a concern.");
            }

            SafetyNotice notice = new SafetyNotice();
            notice.offerId = offerId;
            notice.raisedBy = me.id;
            notice.concern = text;
            notice.status = SafetyNotice.Status.OPEN;
            notice.raisedAt = LocalDateTime.now();
            int id = noticeDao.insert(c, notice);
            logDao.logStatusChange(c, ENTITY, id, null, SafetyNotice.Status.OPEN.name(), text, me.id);   // A3
            return id;
        });
    }

    /** Every notice for the admin's Safety Notices screen, newest first. */
    public List<SafetyNotice> getNotices() {
        requireAdmin();
        try (Connection c = Database.getConnection()) {
            return noticeDao.findAll(c);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load safety notices", e);
        }
    }

    /** Every notice with the organization names and listing text the admin's screen shows, newest first. */
    public List<SafetyNoticeDao.NoticeRow> getNoticeRows() {
        requireAdmin();
        try (Connection c = Database.getConnection()) {
            return noticeDao.findRows(c);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load safety notices", e);
        }
    }

    /** Admin closes an Open notice without action. The note is required. */
    public void dismiss(int noticeId, String adminNote) {
        Account admin = requireAdmin();
        String note = requireText(adminNote, "Write a note before dismissing the notice.");

        inTransaction(c -> {
            SafetyNotice notice = requireOpen(c, noticeId);
            resolve(c, notice, SafetyNotice.Status.DISMISSED, note, admin.id);
            return null;
        });
    }

    /**
     * Admin suspends the flagged account (the other side of the offer) and closes the notice.
     * One transaction (A2); the suspension itself goes through RegistrationService (R7.2).
     */
    public void suspendFlaggedAccount(int noticeId, String adminNote) {
        Account admin = requireAdmin();
        String note = requireText(adminNote, "Write a note before suspending the account.");

        inTransaction(c -> {
            SafetyNotice notice = requireOpen(c, noticeId);
            int flaggedId = flaggedAccountId(c, notice);
            resolve(c, notice, SafetyNotice.Status.ACCOUNT_SUSPENDED, note, admin.id);
            registrationService.suspend(c, flaggedId, note, admin.id);
            return null;
        });
    }

    /** The account a notice is about: whichever side of the offer did not raise it. */
    private int flaggedAccountId(Connection c, SafetyNotice notice) throws SQLException {
        OfferDao.Parties parties = offerDao.findParties(c, notice.offerId);
        // an overdue-report notice has no raiser (raisedBy reads as 0): the food group that owes the report is flagged
        return notice.raisedBy == parties.offererId() ? parties.posterId() : parties.offererId();
    }

    private void resolve(Connection c, SafetyNotice notice, SafetyNotice.Status to, String note, int adminId) throws SQLException {
        if (noticeDao.resolve(c, notice.id, to, note, adminId, LocalDateTime.now()) != 1) {
            throw new RuleViolationException("Notice #" + notice.id + " has already been resolved.");
        }
        logDao.logStatusChange(c, ENTITY, notice.id, notice.status.name(), to.name(), note, adminId);   // A3
    }

    private SafetyNotice requireOpen(Connection c, int noticeId) throws SQLException {
        SafetyNotice notice = noticeDao.findById(c, noticeId);
        if (notice == null) throw new RuleViolationException("That notice no longer exists.");
        if (notice.status != SafetyNotice.Status.OPEN) {
            throw new RuleViolationException("Notice #" + noticeId + " has already been resolved.");
        }
        return notice;
    }

    private Account requireAdmin() {
        Account me = Session.require();
        if (me.role != Account.Role.ADMIN) throw new RuleViolationException("Only an admin can do this.");
        return me;
    }

    private static String requireText(String text, String missingMessage) {
        if (text == null || text.isBlank()) throw new RuleViolationException(missingMessage);
        String trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new RuleViolationException("Keep it to " + MAX_TEXT_LENGTH + " characters or fewer.");
        }
        return trimmed;
    }

    /** Body of one transaction. */
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    /** Runs the work in one transaction (A2): commits if it finishes, rolls back on any exception. */
    private <T> T inTransaction(Work<T> work) {
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
            throw new DataAccessException("Could not save the safety notice", e);
        }
    }
}
