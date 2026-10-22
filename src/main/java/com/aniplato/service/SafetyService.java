package com.aniplato.service;

import com.aniplato.dao.LogDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.SafetyNoticeDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
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
    private static final String SAVE_ERROR = "Could not save the safety notice";

    private final SafetyNoticeDao noticeDao = new SafetyNoticeDao();
    private final OfferDao offerDao = new OfferDao();
    private final LogDao logDao = new LogDao();
    private final AccountStatusService accountStatusService = new AccountStatusService();

    /**
     * R7.1: either side of an Accepted or Completed offer flags a concern about the other side.
     * A Suspended account may still flag (6.3). Returns the new notice id.
     */
    public int raise(int offerId, String concern) {
        Account me = Session.require();
        String text = requireText(concern, "Describe the concern.");

        return Database.inTransaction(SAVE_ERROR, c -> {
            OfferDao.Parties parties = offerDao.findParties(c, offerId);
            if (parties == null) throw new RuleViolationException("That offer no longer exists.");
            Offer.Status status = offerDao.findStatus(c, offerId);
            if (status != Offer.Status.ACCEPTED && status != Offer.Status.COMPLETED) {
                throw new RuleViolationException("A concern can be raised only on an accepted or completed offer.");
            }
            if (me.id != parties.offererId() && me.id != parties.posterId()) {
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

    /** Every notice with the organization names and listing text the admin's screen shows, newest first. */
    public List<SafetyNoticeDao.NoticeRow> getNoticeRows() {
        Session.requireAdmin();
        try (Connection c = Database.getConnection()) {
            return noticeDao.findRows(c);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load safety notices", e);
        }
    }

    /** Admin closes an Open notice without action. The note is required. */
    public void dismiss(int noticeId, String adminNote) {
        Account admin = Session.requireAdmin();
        String note = requireText(adminNote, "Write a note before dismissing the notice.");

        Database.inTransaction(SAVE_ERROR, c -> {
            SafetyNotice notice = requireOpen(c, noticeId);
            resolve(c, notice, SafetyNotice.Status.DISMISSED, note, admin.id);
            return null;
        });
    }

    /**
     * Admin suspends the flagged account (the other side of the offer) and closes the notice.
     * One transaction (A2); the suspension itself goes through AccountStatusService (R7.2).
     */
    public void suspendFlaggedAccount(int noticeId, String adminNote) {
        Account admin = Session.requireAdmin();
        String note = requireText(adminNote, "Write a note before suspending the account.");

        Database.inTransaction(SAVE_ERROR, c -> {
            SafetyNotice notice = requireOpen(c, noticeId);
            int flaggedId = flaggedAccountId(c, notice);
            resolve(c, notice, SafetyNotice.Status.ACCOUNT_SUSPENDED, note, admin.id);
            accountStatusService.suspend(c, flaggedId, note, admin.id);
            return null;
        });
    }

    /**
     * The overdue-report flag (6.6, R5.4): the system tells the admin a food group owes a report. Runs inside the
     * caller's transaction; the schema allows one per offer, and the caller only asks for offers without one.
     */
    public void raiseOverdueReport(Connection c, int offerId) throws SQLException {
        SafetyNotice notice = new SafetyNotice();
        notice.offerId = offerId;
        notice.kind = SafetyNotice.Kind.OVERDUE_REPORT;
        notice.status = SafetyNotice.Status.OPEN;
        notice.raisedAt = LocalDateTime.now();
        int id = noticeDao.insert(c, notice);
        logDao.logStatusChange(c, ENTITY, id, null, SafetyNotice.Status.OPEN.name(), null, null);   // A3, by the system
    }

    /** The account a notice is about: whichever side of the offer did not raise it; for an overdue report, the food group. */
    private int flaggedAccountId(Connection c, SafetyNotice notice) throws SQLException {
        OfferDao.Parties parties = offerDao.findParties(c, notice.offerId);
        if (notice.kind == SafetyNotice.Kind.OVERDUE_REPORT) {
            return offerDao.isOnListing(c, notice.offerId) ? parties.offererId() : parties.posterId();
        }
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

    private static String requireText(String text, String missingMessage) {
        if (text == null || text.isBlank()) throw new RuleViolationException(missingMessage);
        String trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new RuleViolationException("Keep it to " + MAX_TEXT_LENGTH + " characters or fewer.");
        }
        return trimmed;
    }
}
