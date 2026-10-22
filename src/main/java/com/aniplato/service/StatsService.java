package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.DocumentDao;
import com.aniplato.dao.OfferStatsDao;
import com.aniplato.dao.PostDao;
import com.aniplato.dao.RegistrationDao;
import com.aniplato.dao.ReportDao;
import com.aniplato.dao.SafetyNoticeDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Registration;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Works out every metric in spec section 3 for the admin dashboard and reports. Nothing here is stored (A9). */
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
    private final OfferStatsDao offerDao = new OfferStatsDao();
    private final ReportDao reportDao = new ReportDao();
    private final RegistrationDao registrationDao = new RegistrationDao();
    private final AccountDao accountDao = new AccountDao();
    private final DocumentDao documentDao = new DocumentDao();
    private final SafetyNoticeDao noticeDao = new SafetyNoticeDao();

    /** The six dashboard cards, for the month that contains {@code today}. */
    public AdminSummary getAdminSummary(LocalDate today) {
        Session.requireAdmin();
        YearMonth month = YearMonth.from(today);
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        try (Connection c = Database.getConnection()) {
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
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the dashboard numbers", e);
        }
    }

    /** Months that have posts, newest first; the current month is always offered. */
    public List<YearMonth> getReportMonths(LocalDate today) {
        Session.requireAdmin();
        try (Connection c = Database.getConnection()) {
            List<YearMonth> months = new ArrayList<>(postDao.findMonthsWithPosts(c));
            YearMonth current = YearMonth.from(today);
            if (!months.contains(current)) months.add(0, current);
            return months;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the report months", e);
        }
    }

    /** Every metric of section 3 for one month, and the By City rows. */
    public MonthlyReport getReport(YearMonth month, LocalDateTime now) {
        Session.requireAdmin();
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        try (Connection c = Database.getConnection()) {
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
                    offerDao.countCompleted(c, from, to),
                    reportDao.sumPeopleServed(c, from, to),
                    rate(postDao.countWithAcceptedOffer(c, from, to), postDao.countWithOffers(c, from, to)),
                    rate(offerDao.countLapsed(c, from, to), offerDao.countAccepted(c, from, to)),
                    rate(reportDao.countReportedOnTime(c, from, to, days, now),
                            reportDao.countHandoffsPastDeadline(c, from, to, days, now)),
                    cities);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the report", e);
        }
    }

    /** part / whole, or null when there is nothing to divide by. */
    private static Double rate(double part, double whole) {
        return whole == 0 ? null : part / whole;
    }

    private static double sum(List<PostDao.CityTotals> rows, java.util.function.ToDoubleFunction<PostDao.CityTotals> field) {
        return rows.stream().mapToDouble(field).sum();
    }
}
