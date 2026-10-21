package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** A9: every number is worked out from stored facts; a rate with nothing to divide by is null. */
class StatsServiceTest extends DbTest {

    private final StatsService stats = new StatsService();

    @Test
    void theDashboardCardsStartFromTheSeed() {
        loginAs(ADMIN);

        var summary = stats.getAdminSummary(LocalDate.now());

        assertEquals(1, summary.pendingRegistrations());
        assertEquals(12.5, summary.kgListedThisMonth(), 0.001, "only the listing counts, not the request");
        assertNull(summary.rescueRate(), "nothing has closed yet");
        assertEquals(0, summary.openSafetyNotices());
        assertEquals(0, summary.documentsEnding());
        assertEquals(0, summary.suspendedAccounts());
    }

    @Test
    void theRescueRateIsRescuedKgOverClosedKg() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 10);
        TestDb.exec("UPDATE offer SET received_at = NOW() WHERE offer_id = ?", offer);
        TestDb.exec("UPDATE post SET status = 'RESCUED' WHERE post_id = ?", Fixtures.seedListingId());
        loginAs(ADMIN);

        var summary = stats.getAdminSummary(LocalDate.now());

        assertEquals(0.8, summary.rescueRate(), 0.001);   // 10 kg rescued of 12.5 kg closed
    }

    @Test
    void theCardsCountOpenNoticesSuspendedAccountsAndEndingDocuments() {
        Fixtures.concern(Fixtures.offerOnSeedListing("ACCEPTED", 5), GROUP, "x");
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", CASUAL);
        TestDb.exec("UPDATE document SET expires_on = ? WHERE doc_type = 'sanitary_permit'", LocalDate.now().plusDays(10));
        loginAs(ADMIN);

        var summary = stats.getAdminSummary(LocalDate.now());

        assertEquals(1, summary.openSafetyNotices());
        assertEquals(1, summary.suspendedAccounts());
        assertEquals(1, summary.documentsEnding());
    }

    @Test
    void theReportMonthsAlwaysOfferTheCurrentMonthFirst() {
        loginAs(ADMIN);
        assertEquals(YearMonth.now(), stats.getReportMonths(LocalDate.now()).get(0));
        assertEquals(YearMonth.now().plusMonths(1), stats.getReportMonths(LocalDate.now().plusMonths(1)).get(0));
    }

    @Test
    void theMonthlyReportWorksOutEveryMetric() {
        int completed = Fixtures.offerOnSeedListing("COMPLETED", 10);
        TestDb.exec("UPDATE offer SET received_at = NOW() WHERE offer_id = ?", completed);
        loginAs(ADMIN);

        var report = stats.getReport(YearMonth.now(), LocalDateTime.now());

        assertEquals(42.5, report.kgPosted(), 0.001, "listing 12.5 kg and request 30 kg");
        assertEquals(10.0, report.kgRescued(), 0.001);
        assertEquals(1, report.handoffsCompleted());
        assertEquals(0, report.peopleServed());
        assertEquals(1.0, report.offerAcceptanceRate(), 0.001, "the one post with an offer had it accepted");
        assertEquals(0.0, report.lapseRate(), 0.001);
        assertEquals(1, report.cities().size());
        assertEquals("Quezon City", report.cities().get(0).city());
    }

    @Test
    void ratesWithNothingToDivideByAreNull() {
        loginAs(ADMIN);

        var report = stats.getReport(YearMonth.now().minusMonths(6), LocalDateTime.now());

        assertEquals(0.0, report.kgPosted(), 0.001);
        assertNull(report.rescueRate());
        assertNull(report.offerAcceptanceRate());
        assertNull(report.lapseRate());
        assertNull(report.reportCompliance());
    }

    @Test
    void onlyAnAdminSeesTheNumbers() {
        loginAs(BUSINESS);
        assertRule("Only an admin can do this.", () -> stats.getAdminSummary(LocalDate.now()));
        assertRule("Only an admin can do this.", () -> stats.getReport(YearMonth.now(), LocalDateTime.now()));
        assertRule("Only an admin can do this.", () -> stats.getReportMonths(LocalDate.now()));
    }
}
