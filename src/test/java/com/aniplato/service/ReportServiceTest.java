package com.aniplato.service;

import com.aniplato.model.Report;
import com.aniplato.service.ReportService.ReportDefaults;
import com.aniplato.service.ReportService.ReportForm;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 5: the food group's report (R5.3), who may see it, and the overdue flag (R5.4). */
class ReportServiceTest extends DbTest {

    private final ReportService reports = new ReportService();

    @TempDir
    Path tmp;

    /** A completed handoff on the seed listing, received the given number of days ago. */
    private int completedHandoff(int daysAgo) {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = DATE_SUB(NOW(), INTERVAL ? DAY), received_at = DATE_SUB(NOW(), INTERVAL ? DAY) WHERE offer_id = ?",
                daysAgo, daysAgo, offer);
        return offer;
    }

    private ReportForm form(int offer) {
        return new ReportForm(offer, "  Weekend feeding  ", LocalDate.now(), 55, "  We fed 55 people.  ", true, Fixtures.photos(tmp, 2));
    }

    //---SUBMIT---

    @Test
    void theFoodGroupSubmitsOneReportWithItsPhotos() {
        int offer = completedHandoff(1);
        loginAs(GROUP);

        int id = reports.submitReport(form(offer));

        assertEquals("Weekend feeding", TestDb.scalar("SELECT event_name FROM report WHERE report_id = ?", id));
        assertEquals(55L, TestDb.count("SELECT people_served FROM report WHERE report_id = ?", id));
        assertEquals(2L, TestDb.count("SELECT COUNT(*) FROM photo WHERE report_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM report WHERE photo_consent_at IS NOT NULL AND offer_id = ?", offer));
        assertRule("A report was already submitted for this handoff.", () -> reports.submitReport(form(offer)));
    }

    @Test
    void aReportNeedsItsFactsPhotosAndTheConsent() {
        int offer = completedHandoff(1);
        loginAs(GROUP);
        List<java.io.File> photos = Fixtures.photos(tmp, 1);

        assertRule("Enter the event or program name.", () -> reports.submitReport(new ReportForm(offer, " ", LocalDate.now(), 1, "x", true, photos)));
        assertRule("Enter the event date.", () -> reports.submitReport(new ReportForm(offer, "x", null, 1, "x", true, photos)));
        assertRule("People served cannot be negative.", () -> reports.submitReport(new ReportForm(offer, "x", LocalDate.now(), -1, "x", true, photos)));
        assertRule("Write a short description.", () -> reports.submitReport(new ReportForm(offer, "x", LocalDate.now(), 1, "", true, photos)));
        assertRule("Tick the photo consent statement to continue.", () -> reports.submitReport(new ReportForm(offer, "x", LocalDate.now(), 1, "x", false, photos)));
        assertRule("Add 1 to 5 photos.", () -> reports.submitReport(new ReportForm(offer, "x", LocalDate.now(), 1, "x", true, List.of())));
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM report"));
    }

    @Test
    void onlyTheReceivingGroupReportsAndOnlyOnACompletedHandoff() {
        int offer = completedHandoff(1);
        loginAs(BUSINESS);
        assertRule("Only the food group that received the food can submit the report.", () -> reports.submitReport(form(offer)));

        int accepted = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 5);
        loginAs(GROUP);
        assertRule("A report can be submitted once the handoff is complete.", () -> reports.submitReport(form(accepted)));
        assertRule("That offer no longer exists.", () -> reports.submitReport(form(99999)));
    }

    @Test
    void aSuspendedGroupAndALateReportAreStillAccepted() {
        int offer = completedHandoff(10);
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", GROUP);
        loginAs(GROUP);

        reports.submitReport(form(offer));

        Report report = reports.getReport(offer);
        assertTrue(report.isLate(reports.getDueAt(offer)));
    }

    //---READ---

    @Test
    void bothSidesAndTheAdminReadTheReportAndOthersDoNot() {
        int offer = completedHandoff(1);
        loginAs(GROUP);
        assertNull(reports.getReport(offer));   // nothing submitted yet
        reports.submitReport(form(offer));

        Report mine = reports.getReport(offer);
        assertEquals(2, mine.photos.size());
        assertFalse(mine.isLate(reports.getDueAt(offer)));
        loginAs(BUSINESS);
        assertEquals("Weekend feeding", reports.getReport(offer).eventName);
        loginAs(ADMIN);
        assertEquals(55, reports.getReport(offer).peopleServed);
        loginAs(CASUAL);
        assertRule("Only the two sides of a handoff can see its report.", () -> reports.getReport(offer));
    }

    @Test
    void theReportIsDueSevenDaysAfterReceipt() {
        int offer = completedHandoff(2);
        loginAs(GROUP);
        LocalDateTime received = TestDb.scalar("SELECT received_at FROM offer WHERE offer_id = ?", offer);
        assertEquals(received.plusDays(7), reports.getDueAt(offer));
    }

    @Test
    void theFormStartsWithTheEventTheOfferStatedOrTheRequestNamed() {
        int onListing = completedHandoff(1);
        int onRequest = Fixtures.offerOnSeedRequest(BUSINESS, "COMPLETED", 10);
        loginAs(GROUP);

        ReportDefaults fromOffer = reports.getDefaults(onListing);
        assertEquals("Feeding program", fromOffer.eventName());
        assertNull(fromOffer.eventDate());   // the offer did not state a date, so the group must enter one (B10)

        ReportDefaults fromRequest = reports.getDefaults(onRequest);
        assertEquals("Weekend feeding program", fromRequest.eventName());
        assertEquals(LocalDate.now().plusDays(6), fromRequest.eventDate());
    }

    //---OVERDUE FLAG---

    @Test
    void anOverdueHandoffIsFlaggedToTheAdminOnceAndNobodyIsSuspended() {
        int offer = completedHandoff(8);

        reports.flagOverdueReports(LocalDateTime.now());
        reports.flagOverdueReports(LocalDateTime.now());

        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM safety_notice WHERE offer_id = ? AND kind = 'OVERDUE_REPORT' AND raised_by IS NULL "
                + "AND concern IS NULL AND status = 'OPEN'", offer));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'NOTICE' AND from_status IS NULL "
                + "AND to_status = 'OPEN' AND changed_by IS NULL"));
        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", GROUP));
    }

    @Test
    void handoffsWithinTheDeadlineOrWithAReportAreNotFlagged() {
        completedHandoff(3);
        int reported = Fixtures.offerOnSeedRequest(BUSINESS, "COMPLETED", 5);   // on the request route, so the same group can have both
        TestDb.exec("UPDATE offer SET released_at = DATE_SUB(NOW(), INTERVAL 9 DAY), received_at = DATE_SUB(NOW(), INTERVAL 9 DAY) WHERE offer_id = ?", reported);
        loginAs(GROUP);
        reports.submitReport(form(reported));

        reports.flagOverdueReports(LocalDateTime.now());

        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM safety_notice"));
    }

    @Test
    void theAdminCanSuspendTheFoodGroupFromAnOverdueNoticeOnEitherRoute() {
        int onListing = completedHandoff(8);
        reports.flagOverdueReports(LocalDateTime.now());
        int notice = ((Number) TestDb.scalar("SELECT notice_id FROM safety_notice WHERE offer_id = ?", onListing)).intValue();
        loginAs(ADMIN);

        var row = new SafetyService().getNoticeRows().get(0);
        assertEquals("Kapwa Community Kitchen", row.flaggedName());
        new SafetyService().suspendFlaggedAccount(notice, "No report");
        assertEquals("SUSPENDED", TestDb.scalar("SELECT status FROM account WHERE email = ?", GROUP));
        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", BUSINESS));

        TestDb.exec("UPDATE account SET status = 'ACTIVE', suspend_reason = NULL");
        int onRequest = Fixtures.offerOnSeedRequest(CASUAL, "COMPLETED", 5);   // the group posted the request, so the group owes the report
        TestDb.exec("UPDATE offer SET released_at = DATE_SUB(NOW(), INTERVAL 9 DAY), received_at = DATE_SUB(NOW(), INTERVAL 9 DAY) WHERE offer_id = ?", onRequest);
        reports.flagOverdueReports(LocalDateTime.now());
        int second = ((Number) TestDb.scalar("SELECT notice_id FROM safety_notice WHERE offer_id = ?", onRequest)).intValue();
        assertEquals("Kapwa Community Kitchen", new SafetyService().getNoticeRows().stream()
                .filter(r -> r.id() == second).findFirst().orElseThrow().flaggedName());
        new SafetyService().suspendFlaggedAccount(second, "No report");
        assertEquals("SUSPENDED", TestDb.scalar("SELECT status FROM account WHERE email = ?", GROUP));
        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", CASUAL));
    }
}
