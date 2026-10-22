package com.aniplato.service;

import com.aniplato.dao.OfferDao;
import com.aniplato.dao.PhotoDao;
import com.aniplato.dao.PostDao;
import com.aniplato.dao.ReportDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.model.Report;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.aniplato.service.DetailRules.requireText;

/** Stage 5, the food group's documentation report: save it, read it, and flag the ones that are overdue (spec 7.3). */
public class ReportService {

    private final ReportDao reportDao = new ReportDao();
    private final OfferDao offerDao = new OfferDao();
    private final PostDao postDao = new PostDao();
    private final PhotoDao photoDao = new PhotoDao();
    private final FileStorageService fileStorageService = new FileStorageService();
    private final SafetyService safetyService = new SafetyService();

    /** What the food group enters for a report (R5.3). */
    public record ReportForm(int offerId, String eventName, LocalDate eventDate, int peopleServed, String description,
                             boolean photoConsent, List<File> photos) { }

    /** The event name and date a report starts with: a request's own event, or what the group said on its offer (B10). The date may be empty. */
    public record ReportDefaults(String eventName, LocalDate eventDate) { }

    //---SAVE AND READ---

    /**
     * The food group submits the report for a Completed handoff, once (R5.3). One transaction: report and photos.
     * A Suspended group may still submit, and a late report is still accepted.
     */
    public int submitReport(ReportForm f) {
        Account me = Session.require();
        requireText(f.eventName(), "Enter the event or program name.", 150);
        if (f.eventDate() == null) throw new RuleViolationException("Enter the event date.");
        if (f.peopleServed() < 0) throw new RuleViolationException("People served cannot be negative.");
        requireText(f.description(), "Write a short description.", 1000);
        if (!f.photoConsent()) throw new RuleViolationException("Tick the photo consent statement to continue.");
        List<String> paths = fileStorageService.saveAllPhotos(f.photos());
        try {
            return Database.inTransaction("Could not save the report", c -> {
                Offer offer = offerDao.findById(c, f.offerId());
                if (offer == null) throw new RuleViolationException("That offer no longer exists.");
                Post post = postDao.findById(c, offer.postId);
                int receiver = post.isListing() ? offer.offererId : post.posterId;
                if (me.id != receiver) throw new RuleViolationException("Only the food group that received the food can submit the report.");
                if (offer.status != Offer.Status.COMPLETED) throw new RuleViolationException("A report can be submitted once the handoff is complete.");
                if (reportDao.findByOffer(c, offer.id) != null) throw new RuleViolationException("A report was already submitted for this handoff.");
                Report report = new Report();
                report.offerId = offer.id;
                report.eventName = f.eventName().trim();
                report.eventDate = f.eventDate();
                report.peopleServed = f.peopleServed();
                report.description = f.description().trim();
                report.photoConsentAt = LocalDateTime.now();
                report.submittedAt = report.photoConsentAt;
                int id = reportDao.insert(c, report);
                for (String path : paths) photoDao.insert(c, PhotoDao.Owner.REPORT, id, path);
                return id;
            });
        } catch (RuntimeException e) {
            fileStorageService.deleteAll(paths);
            throw e;
        }
    }

    /** The report of a handoff for its two sides or an admin, read-only, or null if none was submitted yet. */
    public Report getReport(int offerId) {
        Account me = Session.require();
        return Database.inTransaction("Could not load the report", c -> {
            Offer offer = offerDao.findById(c, offerId);
            if (offer == null) throw new RuleViolationException("That offer no longer exists.");
            Post post = postDao.findById(c, offer.postId);
            boolean side = me.id == offer.offererId || me.id == post.posterId;
            if (!side && me.role != Account.Role.ADMIN) throw new RuleViolationException("Only the two sides of a handoff can see its report.");
            Report report = reportDao.findByOffer(c, offerId);
            if (report != null) report.photos = photoDao.findPaths(c, PhotoDao.Owner.REPORT, report.id);
            return report;
        });
    }

    /** When the report of a Completed handoff is due: receipt plus the report period. Worked out, not stored (A9). */
    public LocalDateTime getDueAt(int offerId) {
        return Database.inTransaction("Could not load the handoff", c -> {
            Offer offer = offerDao.findById(c, offerId);
            if (offer == null || offer.receivedAt == null) throw new RuleViolationException("That handoff is not complete.");
            return offer.receivedAt.plusDays(Settings.REPORT_DEADLINE_DAYS);
        });
    }

    /** What to prefill on the report form for an offer. */
    public ReportDefaults getDefaults(int offerId) {
        return Database.inTransaction("Could not load the handoff", c -> {
            Offer offer = offerDao.findById(c, offerId);
            if (offer == null) throw new RuleViolationException("That offer no longer exists.");
            Post post = postDao.findById(c, offer.postId);
            var use = post.isListing() ? offer.use : post.use;
            return new ReportDefaults(use.useDescription, use.eventDate);
        });
    }

    //---AUTOMATIC CHECK (6.6, step 5)---

    /**
     * A Completed handoff whose report is past due, with no report, is flagged to the admin once. The notice itself is
     * raised through SafetyService (spec 7.3); the group is not suspended and nothing is blocked.
     */
    public void flagOverdueReports(LocalDateTime now) {
        Database.inTransaction("Could not flag overdue reports", c -> {
            for (int offerId : reportDao.findOverdueWithoutFlag(c, Settings.REPORT_DEADLINE_DAYS, now)) {
                safetyService.raiseOverdueReport(c, offerId);
            }
            return null;
        });
    }
}
