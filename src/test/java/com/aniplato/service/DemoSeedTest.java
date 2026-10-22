package com.aniplato.service;

import com.aniplato.service.HistoryService.Outcome;
import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** sql/seed-demo.sql: it loads on top of the seed, and the story it tells works through the real services. */
class DemoSeedTest extends DbTest {

    @BeforeEach
    void loadTheDemo() {
        TestDb.loadScript("sql/seed-demo.sql");
    }

    private long posts(String kind, String status) {
        return TestDb.count("SELECT COUNT(*) FROM post WHERE kind = ? AND status = ?", kind, status);
    }

    @Test
    void everyStatusOfTheStoryIsThere() {
        assertEquals(2L, posts("LISTING", "OPEN"));      // the seed listing and the vegetables
        assertEquals(2L, posts("LISTING", "ACCEPTED"));
        assertEquals(2L, posts("LISTING", "RESCUED"));
        assertEquals(1L, posts("LISTING", "EXPIRED"));
        assertEquals(1L, posts("LISTING", "CANCELLED"));
        assertEquals(2L, posts("REQUEST", "OPEN"));      // the seed request and the school program
        assertEquals(1L, posts("REQUEST", "ACCEPTED"));
        assertEquals(1L, posts("REQUEST", "RESCUED"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM offer WHERE status = 'LAPSED'"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM report"));
        assertEquals("SUSPENDED", TestDb.scalar("SELECT status FROM account WHERE email = 'suspended@aniplato.test'"));
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM photo WHERE file_path LIKE 'seed/%' AND file_path NOT IN "
                + "('seed/bread.jpg','seed/venue.jpg','seed/vegetables.png','seed/rice.png','seed/event.png')"));
    }

    @Test
    void theDonorHasAnOfferToDecideAndOneHandoffToRelease() {
        loginAs(BUSINESS);
        var dashboard = new DashboardService().getDonorDashboard();
        assertEquals(1, dashboard.offersToReview());   // the group's offer on the vegetables
        assertEquals(1, dashboard.awaitingRelease());  // the rice meals; the bread and the typhoon packs are already released
        assertTrue(dashboard.kgRescued() > 0);
        assertEquals(4, dashboard.activeListings().size());   // the seed listing, the vegetables, the rice meals, the bread
    }

    @Test
    void theFoodGroupHasThreeAcceptedHandoffsAndTwoReportsDue() {
        loginAs(GROUP);
        var dashboard = new DashboardService().getFoodGroupDashboard();
        assertEquals(3, dashboard.acceptedOffers());   // the rice meals, the bread, the typhoon packs
        var banner = dashboard.banners().get(0);
        assertEquals(DashboardService.BannerKind.REPORT_DUE, banner.kind());
        assertTrue(banner.text().startsWith("2 reports are due"));   // the pastries, and the overdue community pantry
        assertTrue(banner.text().contains("was due") && banner.text().contains("Community pantry restock"));
    }

    @Test
    void historyShowsRescuedExpiredAndCancelledForTheDonor() {
        loginAs(BUSINESS);
        var rows = new HistoryService().getHistory(null).rows();
        Set<Outcome> outcomes = rows.stream().map(HistoryService.Row::outcome).collect(Collectors.toSet());
        assertEquals(Set.of(Outcome.RESCUED, Outcome.EXPIRED, Outcome.CANCELLED), outcomes);
        assertTrue(rows.stream().anyMatch(r -> r.outcomeText().equals("Expired (missed pickup)")));
    }

    @Test
    void theFirstScreenLoadFlagsTheOverdueReportAndTheOtherStatusesStayPut() {
        new MaintenanceService().runMaintenance();

        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM safety_notice WHERE kind = 'OVERDUE_REPORT' AND status = 'OPEN'"));
        assertEquals(2L, posts("LISTING", "ACCEPTED"), "nothing lapsed: the pickups are still ahead");
        assertEquals(2L, posts("LISTING", "OPEN"));

        loginAs(ADMIN);
        var notice = new SafetyService().getNoticeRows().stream()
                .filter(n -> n.kind() == com.aniplato.model.SafetyNotice.Kind.OVERDUE_REPORT).findFirst().orElseThrow();
        assertEquals("Kapwa Community Kitchen", notice.flaggedName());
    }
}
