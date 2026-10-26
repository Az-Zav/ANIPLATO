package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.HistoryDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.PostDao;
import com.aniplato.dao.RegistrationDao;
import com.aniplato.dao.ReportDao;
import com.aniplato.dao.SafetyNoticeDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Document;
import com.aniplato.model.Post;
import com.aniplato.model.Registration;
import com.aniplato.model.Report;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Works out every figure the app shows (spec 3 and 7.3): the admin dashboard and reports, the donor's and food group's
 * dashboards, and History. Nothing here is stored (A9).
 */
public class StatsService {

    /** The cards on the admin dashboard. A rate is null when nothing has closed yet. */
    public record AdminSummary(int pendingRegistrations, double kgListedThisMonth, Double rescueRate,
                               int openSafetyNotices, int documentsEnding, int suspendedAccounts) { }

    /** One row of the By City table. */
    public record CityRow(String city, double kgPosted, double kgRescued, double kgExpired, Double rescueRate) { }

    /** One month of the Reports screen. A rate is null when its denominator is zero. */
    public record MonthlyReport(YearMonth month, double kgPosted, double kgRescued, double kgExpired,
                                Double rescueRate, int handoffsCompleted, int peopleServed,
                                Double offerAcceptanceRate, Double lapseRate, Double reportCompliance,
                                List<CityRow> cities) { }

    private final PostDao postDao = new PostDao();
    private final ReportDao reportDao = new ReportDao();
    private final RegistrationDao registrationDao = new RegistrationDao();
    private final AccountDao accountDao = new AccountDao();
    private final DocumentDao documentDao = new DocumentDao();
    private final SafetyNoticeDao noticeDao = new SafetyNoticeDao();
    private final HistoryDao historyDao = new HistoryDao();
    private final OfferDao offers = new OfferDao();
    private final PostService postService = new PostService();
    private final RegistrationService registrationService = new RegistrationService();

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final int ROWS_SHOWN = 5;

    /** The six dashboard cards, for the month that contains {@code today}. */
    public AdminSummary getAdminSummary(LocalDate today) {
        Session.requireAdmin();
        YearMonth month = YearMonth.from(today);
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return Database.read("Could not load the dashboard numbers", c -> {
            List<PostDao.CityTotals> cities = postDao.totalsByCity(c, from, to);
            int ending = (int) documentDao.findEndingBy(c, today.plusDays(Settings.DOCUMENT_WARNING_DAYS)).stream()
                    .filter(d -> !d.expiresOn().isBefore(today)).map(DocumentDao.ExpiringDocument::registrationId)
                    .distinct().count();
            return new AdminSummary(
                    registrationDao.countByStatus(c, Registration.Status.PENDING),
                    postDao.sumKgListed(c, from, to),
                    rate(sum(cities, PostDao.CityTotals::kgRescued), sum(cities, PostDao.CityTotals::kgClosed)),
                    noticeDao.countOpen(c),
                    ending,
                    accountDao.countSuspended(c));
        });
    }

    /** Months that have posts, newest first; the current month is always offered. */
    public List<YearMonth> getReportMonths(LocalDate today) {
        Session.requireAdmin();
        return Database.read("Could not load the report months", c -> {
            List<YearMonth> months = new ArrayList<>(postDao.findMonthsWithPosts(c));
            YearMonth current = YearMonth.from(today);
            if (!months.contains(current)) months.add(0, current);
            return months;
        });
    }

    /** Every metric of section 3 for one month, and the By City rows. */
    public MonthlyReport getReport(YearMonth month, LocalDateTime now) {
        Session.requireAdmin();
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return Database.read("Could not load the report", c -> {
            List<PostDao.CityTotals> totals = postDao.totalsByCity(c, from, to);
            List<CityRow> cities = new ArrayList<>();
            for (PostDao.CityTotals t : totals) {
                cities.add(new CityRow(t.city(), t.kgPosted(), t.kgRescued(), t.kgExpired(), rate(t.kgRescued(), t.kgClosed())));
            }
            int days = Settings.REPORT_DEADLINE_DAYS;
            return new MonthlyReport(month,
                    sum(totals, PostDao.CityTotals::kgPosted),
                    sum(totals, PostDao.CityTotals::kgRescued),
                    sum(totals, PostDao.CityTotals::kgExpired),
                    rate(sum(totals, PostDao.CityTotals::kgRescued), sum(totals, PostDao.CityTotals::kgClosed)),
                    offers.countCompleted(c, from, to),
                    reportDao.sumPeopleServed(c, from, to),
                    rate(postDao.countWithAcceptedOffer(c, from, to), postDao.countWithOffers(c, from, to)),
                    rate(offers.countLapsed(c, from, to), offers.countAccepted(c, from, to)),
                    rate(reportDao.countReportedOnTime(c, from, to, days, now),
                            reportDao.countHandoffsPastDeadline(c, from, to, days, now)),
                    cities);
        });
    }

    /** part / whole, or null when there is nothing to divide by. */
    private static Double rate(double part, double whole) {
        return whole == 0 ? null : part / whole;
    }

    private static double sum(List<PostDao.CityTotals> rows, java.util.function.ToDoubleFunction<PostDao.CityTotals> field) {
        return rows.stream().mapToDouble(field).sum();
    }

    //---DONOR AND FOOD GROUP DASHBOARDS (spec 3, Figma V2)---

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
                offers.countAwaitingDecision(c, me.id), historyDao.countAwaitingRelease(c, me.id),
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
                offers.countAcceptedFor(c, me.id), historyDao.totals(c, me.id).kgRescued(),
                listings.stream().limit(4).toList()));
    }

    //---BANNERS---

    private List<Banner> banners(Account me, Connection c) throws SQLException {
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
        List<Document> documents = registrationService.getCurrentDocumentAlerts(today);
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

    //---HISTORY (spec 3 and 7.3)---

    public enum Outcome { RESCUED, EXPIRED, CANCELLED }

    /**
     * One line of the table. {@code outcomeText} is what the screen shows ("Expired (missed pickup)"); {@code report}
     * is the food group's report status ("Submitted", "Submitted late", "Due Oct 12") or null when the line has none;
     * {@code recordOfferId} opens the handoff record and is null unless the handoff was completed.
     */
    public record Row(LocalDateTime when, String food, double kg, String otherParty, Outcome outcome, String outcomeText,
                      String report, Integer recordOfferId) { }

    /** The cards, the period choices and the table lines of one period, newest first. */
    public record View(double kgRescued, int handoffs, int partners, List<YearMonth> months, List<Row> rows) {

        /** How many lines of the table are of one outcome, for the tab labels. */
        public int count(Outcome outcome) {
            return (int) rows.stream().filter(r -> r.outcome() == outcome).count();
        }
    }

    /** History for the logged-in account; {@code period} null means all time. */
    public View getHistory(YearMonth period) {
        Account me = Session.require();
        return Database.inTransaction("Could not load History", c -> {
            HistoryDao.Totals totals = historyDao.totals(c, me.id);
            List<Row> all = new ArrayList<>();
            for (HistoryDao.Row row : historyDao.findClosed(c, me.id)) {
                Report report = row.receiver() && row.offerId() != null && row.receivedAt() != null
                        ? reportDao.findByOffer(c, row.offerId()) : null;
                all.add(toRow(row, report));
            }
            TreeSet<YearMonth> months = new TreeSet<>(Comparator.reverseOrder());
            all.forEach(r -> months.add(YearMonth.from(r.when())));
            List<Row> shown = all.stream().filter(r -> period == null || YearMonth.from(r.when()).equals(period))
                    .sorted(Comparator.comparing(Row::when).reversed()).toList();
            return new View(totals.kgRescued(), totals.handoffs(), totals.partners(), List.copyOf(months), shown);
        });
    }

    private static Row toRow(HistoryDao.Row r, Report report) {
        boolean completed = "COMPLETED".equals(r.offerStatus());
        Outcome outcome;
        String text;
        if (r.mine()) {
            outcome = switch (r.postStatus()) {
                case "RESCUED" -> Outcome.RESCUED;
                case "EXPIRED" -> Outcome.EXPIRED;
                default -> Outcome.CANCELLED;
            };
            text = outcome == Outcome.EXPIRED && "LAPSED".equals(r.offerStatus()) ? "Expired (missed pickup)"
                    : outcome == Outcome.CANCELLED ? "Cancelled" : label(outcome);
        } else {
            outcome = switch (r.offerStatus()) {
                case "COMPLETED" -> Outcome.RESCUED;
                case "CANCELLED" -> Outcome.CANCELLED;
                default -> Outcome.EXPIRED;
            };
            text = "LAPSED".equals(r.offerStatus()) ? "Expired (missed pickup)"
                    : outcome == Outcome.CANCELLED ? "Cancelled by the poster" : label(outcome);
        }
        return new Row(r.closedAt(), r.title(), r.kg(), r.otherParty(), outcome, text, reportText(r, report),
                completed ? r.offerId() : null);
    }

    private static String label(Outcome outcome) {
        return outcome == Outcome.RESCUED ? "Rescued" : "Expired";
    }

    /** The food group's report column: nothing for a line that is not a completed handoff or not its own to report. */
    private static String reportText(HistoryDao.Row r, Report report) {
        if (!r.receiver() || r.receivedAt() == null) return null;
        LocalDateTime due = r.receivedAt().plusDays(Settings.REPORT_DEADLINE_DAYS);
        if (report == null) return (due.isBefore(LocalDateTime.now()) ? "Overdue since " : "Due ") + due.format(DAY);
        return report.isLate(due) ? "Submitted late" : "Submitted";
    }
}
