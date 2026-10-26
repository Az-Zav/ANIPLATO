package com.aniplato.service;

import com.aniplato.model.FoodDetail;
import com.aniplato.model.HandoffRecord;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Stage 5: release, then receipt, and the handoff record (R5.1, R5.2). The donor releases and the food group receives on both routes. */
class HandoffServiceTest extends DbTest {

    private final OfferService handoff = new OfferService();

    private String offerStatus(int id) {
        return TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", id);
    }

    private String postStatus(int id) {
        return TestDb.scalar("SELECT status FROM post WHERE post_id = ?", id);
    }

    //---RELEASE---

    @Test
    void theDonorConfirmsReleaseOnAListing() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(BUSINESS);

        handoff.confirmRelease(offer);

        assertNotNull(TestDb.scalar("SELECT released_at FROM offer WHERE offer_id = ?", offer));
        assertNull(TestDb.scalar("SELECT received_at FROM offer WHERE offer_id = ?", offer));
        assertEquals("ACCEPTED", offerStatus(offer));
        assertRule("Release was already confirmed.", () -> handoff.confirmRelease(offer));
    }

    @Test
    void theDonorWhoOfferedOnARequestConfirmsRelease() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        loginAs(GROUP);
        assertRule("Only the donor can confirm release.", () -> handoff.confirmRelease(offer));
        loginAs(BUSINESS);
        handoff.confirmRelease(offer);
        assertNotNull(TestDb.scalar("SELECT released_at FROM offer WHERE offer_id = ?", offer));
    }

    @Test
    void releaseNeedsAnAcceptedOfferAndTheDonor() {
        int offered = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(BUSINESS);
        assertRule("Only an Accepted offer can be released.", () -> handoff.confirmRelease(offered));
        loginAs(GROUP);
        assertRule("Only the donor can confirm release.", () -> handoff.confirmRelease(offered));
        loginAs(CASUAL);
        assertRule("Only the donor can confirm release.", () -> handoff.confirmRelease(offered));
        assertRule("That offer no longer exists.", () -> handoff.confirmRelease(99999));
    }

    @Test
    void aSuspendedDonorCanStillFinishAHandoff() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", BUSINESS);
        loginAs(BUSINESS);
        handoff.confirmRelease(offer);
        assertNotNull(TestDb.scalar("SELECT released_at FROM offer WHERE offer_id = ?", offer));
    }

    //---RECEIPT---

    @Test
    void receiptNeedsReleaseFirst() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(GROUP);

        assertRule("Receipt can be confirmed only after the donor confirms release.", () -> handoff.confirmReceipt(offer));
        assertEquals("ACCEPTED", offerStatus(offer));
        assertEquals("OPEN", postStatus(Fixtures.seedListingId()));
    }

    @Test
    void receiptCompletesTheOfferRescuesThePostAndLogsBoth() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        loginAs(BUSINESS);
        handoff.confirmRelease(offer);
        int groupId = loginAs(GROUP).id;

        handoff.confirmReceipt(offer);

        assertEquals("COMPLETED", offerStatus(offer));
        assertEquals("RESCUED", postStatus(Fixtures.seedListingId()));
        assertNotNull(TestDb.scalar("SELECT received_at FROM offer WHERE offer_id = ?", offer));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? "
                + "AND from_status = 'ACCEPTED' AND to_status = 'COMPLETED' AND changed_by = ?", offer, groupId));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'POST' AND entity_id = ? "
                + "AND from_status = 'ACCEPTED' AND to_status = 'RESCUED'", Fixtures.seedListingId()));
        assertRule("This handoff is already complete.", () -> handoff.confirmReceipt(offer));
    }

    @Test
    void onlyTheFoodGroupConfirmsReceiptOnEitherRoute() {
        int onListing = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        int onRequest = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        TestDb.exec("UPDATE offer SET released_at = NOW()");
        loginAs(BUSINESS);
        assertRule("Only the food group can confirm receipt.", () -> handoff.confirmReceipt(onListing));
        assertRule("Only the food group can confirm receipt.", () -> handoff.confirmReceipt(onRequest));
        loginAs(GROUP);
        handoff.confirmReceipt(onRequest);   // the group posted the request, so it receives
        assertEquals("COMPLETED", offerStatus(onRequest));
    }

    //---THE RECORD---

    @Test
    void theRecordIsWorkedOutFromTheCompletedOfferForBothSides() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(BUSINESS);
        handoff.confirmRelease(offer);
        loginAs(GROUP);
        handoff.confirmReceipt(offer);

        HandoffRecord record = handoff.getHandoffRecord(offer);

        assertEquals("HR-" + LocalDateTime.now().getYear() + "-" + String.format("%04d", offer), record.number());
        assertEquals("Sunrise Bakery", record.donorName());
        assertEquals("Kapwa Community Kitchen", record.groupName());
        assertEquals("Day-old pandesal and loaf bread", record.foodDescription());
        assertEquals(FoodDetail.Category.BAKED, record.category());
        assertEquals(12.5, record.agreedKg());
        assertEquals("Feeding program", record.useDescription());
        assertEquals(record.receivedAt().plusDays(7), record.reportDueAt());

        loginAs(BUSINESS);
        assertEquals(record.number(), handoff.getHandoffRecord(offer).number());
        loginAs(ADMIN);
        assertEquals(record.number(), handoff.getHandoffRecord(offer).number());
        loginAs(CASUAL);
        assertRule("Only the two sides of a handoff can see its record.", () -> handoff.getHandoffRecord(offer));
    }

    @Test
    void thereIsNoRecordUntilTheHandoffIsComplete() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(GROUP);
        assertRule("A record exists once the handoff is complete.", () -> handoff.getHandoffRecord(offer));
    }

    @Test
    void theRecordOnARequestTakesTheFoodFromTheOffer() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        loginAs(BUSINESS);
        handoff.confirmRelease(offer);
        loginAs(GROUP);
        handoff.confirmReceipt(offer);

        HandoffRecord record = handoff.getHandoffRecord(offer);
        assertEquals("Rice", record.foodDescription());
        assertEquals(10.0, record.agreedKg());   // the accepted offer's kg, not the 30 requested
        assertEquals("Weekend feeding program", record.useDescription());
        assertEquals("Sunrise Bakery", record.donorName());
    }

    @Test
    void theBadgeCountsAcceptedOffersEitherSideIsWaitingOn() {
        Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(GROUP);
        assertEquals(1, handoff.countAwaitingHandoff());
        loginAs(CASUAL);
        assertEquals(0, handoff.countAwaitingHandoff());
    }
}
