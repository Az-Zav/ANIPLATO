package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 6.6: the automatic checks, in the spec order, each with its test. */
class MaintenanceServiceTest extends DbTest {

    private final MaintenanceService maintenance = new MaintenanceService();

    private String status(String email) {
        return TestDb.scalar("SELECT status FROM account WHERE email = ?", email);
    }

    private void expireBusinessPermit() {
        TestDb.exec("UPDATE document SET expires_on = ? WHERE doc_type = 'mayors_permit'", LocalDate.now().minusDays(1));
    }

    @Test
    void anExpiredVerifiedDocumentSuspendsItsAccountWithNobodyLoggedIn() {
        expireBusinessPermit();

        maintenance.runMaintenance();

        assertEquals("SUSPENDED", status(BUSINESS));
        assertEquals("Document expired", TestDb.scalar("SELECT suspend_reason FROM account WHERE email = ?", BUSINESS));
        assertEquals("ACTIVE", status(CASUAL));
        assertEquals("ACTIVE", status(GROUP));
    }

    @Test
    void runningItTwiceChangesNothingTheSecondTime() {
        expireBusinessPermit();
        maintenance.runMaintenance();
        long logged = TestDb.count("SELECT COUNT(*) FROM status_change");

        maintenance.runMaintenance();

        assertEquals(logged, TestDb.count("SELECT COUNT(*) FROM status_change"));
    }

    //---1. LAPSE---

    private void setOfferPickup(int offerId, String interval) {
        TestDb.exec("UPDATE use_detail ud JOIN offer o ON o.use_detail_id = ud.use_detail_id SET ud.due_at = DATE_SUB(NOW(), INTERVAL " + interval
                + ") WHERE o.offer_id = ?", offerId);
    }

    @Test
    void anAcceptedOfferTwoHoursPastItsPickupLapsesAndItsPostExpires() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        setOfferPickup(offer, "3 HOUR");

        maintenance.runMaintenance();

        assertEquals("LAPSED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", offer));
        assertEquals("EXPIRED", TestDb.scalar("SELECT status FROM post WHERE kind = 'LISTING'"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? "
                + "AND to_status = 'LAPSED' AND changed_by IS NULL", offer));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'POST' AND to_status = 'EXPIRED' AND changed_by IS NULL"));
    }

    @Test
    void anOfferWithinTheGraceOrAlreadyReleasedDoesNotLapse() {
        int inGrace = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        setOfferPickup(inGrace, "1 HOUR");
        maintenance.runMaintenance();
        assertEquals("ACCEPTED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", inGrace));

        setOfferPickup(inGrace, "5 HOUR");
        TestDb.exec("UPDATE offer SET released_at = NOW() WHERE offer_id = ?", inGrace);   // M1: released food does not lapse
        maintenance.runMaintenance();
        assertEquals("ACCEPTED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", inGrace));
    }

    @Test
    void aRequestOfferLapsesTwoHoursAfterItsPickupWindowEnds() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        TestDb.exec("UPDATE food_detail fd JOIN offer o ON o.food_detail_id = fd.food_detail_id SET fd.pickup_start = DATE_SUB(NOW(), INTERVAL 2 DAY), "
                + "fd.pickup_end = DATE_SUB(NOW(), INTERVAL 3 HOUR) WHERE o.offer_id = ?", offer);

        maintenance.runMaintenance();

        assertEquals("LAPSED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", offer));
    }

    //---2 AND 3. OPEN POSTS PAST THEIR DEADLINE---

    @Test
    void anOpenListingPastItsUseByExpiresWithItsOfferedOffers() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        TestDb.exec("UPDATE food_detail SET pickup_start = DATE_SUB(NOW(), INTERVAL 3 DAY), pickup_end = DATE_SUB(NOW(), INTERVAL 2 DAY), "
                + "use_by = DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE description = 'Day-old pandesal and loaf bread'");

        maintenance.runMaintenance();

        assertEquals("EXPIRED", TestDb.scalar("SELECT status FROM post WHERE kind = 'LISTING'"));
        assertEquals("EXPIRED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", offer));
        assertEquals("OPEN", TestDb.scalar("SELECT status FROM post WHERE kind = 'REQUEST'"));
    }

    @Test
    void anOpenRequestPastItsNeededByExpiresWithItsOfferedOffers() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        TestDb.exec("UPDATE use_detail ud JOIN post p ON p.use_detail_id = ud.use_detail_id SET ud.due_at = DATE_SUB(NOW(), INTERVAL 1 HOUR) "
                + "WHERE p.kind = 'REQUEST'");

        maintenance.runMaintenance();

        assertEquals("EXPIRED", TestDb.scalar("SELECT status FROM post WHERE kind = 'REQUEST'"));
        assertEquals("EXPIRED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", offer));
        assertEquals("OPEN", TestDb.scalar("SELECT status FROM post WHERE kind = 'LISTING'"));
    }

    @Test
    void anAcceptedPostIsNotExpiredByItsUseBy() {
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        TestDb.exec("UPDATE food_detail SET pickup_start = DATE_SUB(NOW(), INTERVAL 3 DAY), pickup_end = DATE_SUB(NOW(), INTERVAL 2 DAY), "
                + "use_by = DATE_SUB(NOW(), INTERVAL 1 DAY)");
        maintenance.runMaintenance();
        assertEquals("ACCEPTED", TestDb.scalar("SELECT status FROM post WHERE kind = 'LISTING'"));
    }

    //---4. OFFERED OFFERS PAST THEIR PICKUP---

    @Test
    void anOfferedOfferPastItsPickupTimeExpiresWhileThePostStaysOpen() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        setOfferPickup(offer, "1 HOUR");

        maintenance.runMaintenance();

        assertEquals("EXPIRED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", offer));
        assertEquals("OPEN", TestDb.scalar("SELECT status FROM post WHERE kind = 'LISTING'"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? "
                + "AND from_status = 'OFFERED' AND to_status = 'EXPIRED' AND changed_by IS NULL", offer));
    }

    //---5. OVERDUE REPORTS---

    @Test
    void aCompletedHandoffWithNoReportAfterSevenDaysIsFlaggedOnce() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = DATE_SUB(NOW(), INTERVAL 9 DAY), received_at = DATE_SUB(NOW(), INTERVAL 8 DAY) WHERE offer_id = ?", offer);

        maintenance.runMaintenance();
        maintenance.runMaintenance();

        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM safety_notice WHERE offer_id = ? AND kind = 'OVERDUE_REPORT'", offer));
    }

    @Test
    void whenNothingHasExpiredNothingChanges() {
        maintenance.runMaintenance();

        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM status_change"));
        assertEquals(3L, TestDb.count("SELECT COUNT(*) FROM account WHERE status = 'ACTIVE' AND role <> 'ADMIN'"));
    }
}
