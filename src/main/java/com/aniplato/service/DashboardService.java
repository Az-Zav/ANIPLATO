package com.aniplato.service;

import com.aniplato.dao.HistoryDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.ReportDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Document;
import com.aniplato.model.Post;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** The donor's and the food group's dashboards (spec 3, Figma V2). Every figure is worked out when asked, never stored (A9). */
public class DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final int ROWS_SHOWN = 5;

    private final HistoryDao historyDao = new HistoryDao();
    private final OfferDao offerDao = new OfferDao();
    private final ReportDao reportDao = new ReportDao();
    private final PostService postService = new PostService();
    private final DocumentReviewService documentReviewService = new DocumentReviewService();

    public enum BannerKind { SUSPENDED, DOCUMENTS, REPORT_DUE }

    /** A banner above the cards. {@code offerId} is the handoff a REPORT_DUE banner is about, otherwise 0. */
    public record Banner(BannerKind kind, String text, int offerId) { }

    public record DonorDashboard(String name, String city, List<Banner> banners, int openListings, int offersToReview,
                                 int awaitingRelease, double kgRescued, double kgNeeded, int openRequests,
                                 List<Post> activeListings) { }

    public record FoodGroupDashboard(String name, String city, List<Banner> banners, int openListings, double kgWaiting,
                                     int inMyCity, int acceptedOffers, double kgRescued, List<Post> nearListings) { }

    //---DONOR---

    /**
     * The donor's dashboard. "Needed by food groups" counts the requests the donor can see on Browse Requests, so a
     * Casual donor's number matches its board (B7).
     */
    public DonorDashboard getDonorDashboard() {
        Account me = Session.require();
        if (me.role != Account.Role.BUSINESS_DONOR && me.role != Account.Role.CASUAL_DONOR) {
            throw new RuleViolationException("Only a donor has this dashboard.");
        }
        List<Post> mine = postService.getMyPosts();
        List<Post> requests = postService.getOpenRequests();
        return Database.inTransaction("Could not load the dashboard", c -> new DonorDashboard(me.displayName(),
                me.registration.city.name, banners(me, c), (int) mine.stream().filter(p -> p.isListing() && p.status == Post.Status.OPEN).count(),
                offerDao.countAwaitingDecision(c, me.id), historyDao.countAwaitingRelease(c, me.id),
                historyDao.totals(c, me.id).kgRescued(), requests.stream().mapToDouble(p -> p.kg).sum(), requests.size(),
                mine.stream().filter(p -> p.status == Post.Status.OPEN || p.status == Post.Status.ACCEPTED).limit(ROWS_SHOWN).toList()));
    }

    //---FOOD GROUP---

    /** The food group's dashboard: the open listings it can offer on, its accepted handoffs, and any report it owes. */
    public FoodGroupDashboard getFoodGroupDashboard() {
        Account me = Session.require();
        if (me.role != Account.Role.FOOD_GROUP) throw new RuleViolationException("Only a food group has this dashboard.");
        List<Post> listings = postService.getOpenListings();
        return Database.inTransaction("Could not load the dashboard", c -> new FoodGroupDashboard(me.displayName(),
                me.registration.city.name, banners(me, c), listings.size(), listings.stream().mapToDouble(p -> p.kg).sum(),
                (int) listings.stream().filter(p -> p.city.equals(me.registration.city.name)).count(),
                offerDao.countAcceptedFor(c, me.id), historyDao.totals(c, me.id).kgRescued(),
                listings.stream().limit(4).toList()));
    }

    //---BANNERS---

    private List<Banner> banners(Account me, java.sql.Connection c) throws SQLException {
        List<Banner> banners = new ArrayList<>();
        if (me.isSuspended()) {
            banners.add(new Banner(BannerKind.SUSPENDED, "Your account is suspended" + (me.suspendReason == null ? "" : ": " + me.suspendReason)
                    + ". You can still finish accepted handoffs and submit reports.", 0));
        }
        banners.addAll(documentBanner());
        if (me.role == Account.Role.FOOD_GROUP) banners.addAll(reportBanner(reportDao.findPending(c, me.id)));
        return banners;
    }

    private List<Banner> documentBanner() {
        LocalDate today = LocalDate.now();
        List<Document> documents = documentReviewService.getCurrentDocumentAlerts(today);
        if (documents.isEmpty()) return List.of();
        long expired = documents.stream().filter(d -> d.isExpired(today)).count();
        String names = documents.stream().map(d -> d.type.label() + " (" + d.expiresOn.format(DAY) + ")").collect(Collectors.joining(" and "));
        String text = expired > 0 ? expired + (expired == 1 ? " document has" : " documents have") + " expired: " + names + "."
                : documents.size() + (documents.size() == 1 ? " document expires" : " documents expire") + " within "
                + Settings.DOCUMENT_WARNING_DAYS + " days: " + names + ".";
        return List.of(new Banner(BannerKind.DOCUMENTS, text, 0));
    }

    private List<Banner> reportBanner(List<ReportDao.Pending> pending) {
        if (pending.isEmpty()) return List.of();
        ReportDao.Pending next = pending.get(0);
        LocalDateTime due = next.receivedAt().plusDays(Settings.REPORT_DEADLINE_DAYS);
        String when = due.isBefore(LocalDateTime.now()) ? "was due " : "due by ";
        String lead = pending.size() == 1 ? "1 report " : pending.size() + " reports are due; the first ";
        return List.of(new Banner(BannerKind.REPORT_DUE, lead + when + due.format(FULL_DAY) + ": " + next.title() + ".", next.offerId()));
    }
}
