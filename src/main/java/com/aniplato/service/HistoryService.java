package com.aniplato.service;

import com.aniplato.dao.HistoryDao;
import com.aniplato.dao.ReportDao;
import com.aniplato.model.Account;
import com.aniplato.model.Report;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * History (spec 3 and 7.3): the cards count everything the account has done, the table lists every post or offer of
 * its own that has closed, and the period only filters the table (B6). Shared screen: it asks for "my" history.
 */
public class HistoryService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d");

    private final HistoryDao historyDao = new HistoryDao();
    private final ReportDao reportDao = new ReportDao();

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
