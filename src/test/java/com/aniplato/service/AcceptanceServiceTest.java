package com.aniplato.service;

import com.aniplato.model.Offer;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 4: the poster's list of offers, accept, decline, and who sees a contact number (R4.1 to R4.3, A7). */
class AcceptanceServiceTest extends DbTest {

    private final OfferService acceptance = new OfferService();

    private String offerStatus(int id) {
        return TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", id);
    }

    private String postStatus(int id) {
        return TestDb.scalar("SELECT status FROM post WHERE post_id = ?", id);
    }

    private long logged(String entity, int id, String from, String to) {
        return TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = ? AND entity_id = ? AND from_status = ? AND to_status = ?",
                entity, id, from, to);
    }

    //---ACCEPT---

    @Test
    void acceptingOneOfferDeclinesTheOthersAcceptsThePostAndLogsAll() {
        int first = Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        int second = Fixtures.offerOnSeedRequest(CASUAL, "OFFERED", 5);
        int groupId = loginAs(GROUP).id;

        acceptance.accept(first);

        assertEquals("ACCEPTED", offerStatus(first));
        assertEquals("DECLINED", offerStatus(second));
        assertEquals("ACCEPTED", postStatus(Fixtures.seedRequestId()));
        assertEquals(1L, logged("OFFER", first, "OFFERED", "ACCEPTED"));
        assertEquals(1L, logged("OFFER", second, "OFFERED", "DECLINED"));
        assertEquals(1L, logged("POST", Fixtures.seedRequestId(), "OPEN", "ACCEPTED"));
        assertEquals(3L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE changed_by = ?", groupId));
    }

    @Test
    void aDonorAcceptsAFoodGroupsOfferOnAListing() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(BUSINESS);

        acceptance.accept(offer);

        assertEquals("ACCEPTED", offerStatus(offer));
        assertEquals("ACCEPTED", postStatus(Fixtures.seedListingId()));
    }

    @Test
    void onlyThePosterAcceptsOrDeclines() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(GROUP);
        assertRule("Only the poster can decide on this offer.", () -> acceptance.accept(offer));
        assertRule("Only the poster can decide on this offer.", () -> acceptance.decline(offer));
        loginAs(CASUAL);
        assertRule("Only the poster can decide on this offer.", () -> acceptance.accept(offer));
        assertRule("That offer no longer exists.", () -> acceptance.accept(99999));
        assertEquals("OFFERED", offerStatus(offer));
    }

    @Test
    void anAcceptedPostTakesNoSecondAcceptAndOnlyOfferedOffersCanBeAccepted() {
        int first = Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        int second = Fixtures.offerOnSeedRequest(CASUAL, "OFFERED", 5);
        loginAs(GROUP);
        acceptance.accept(first);

        assertRule("This request is no longer open.", () -> acceptance.accept(second));
        assertEquals("DECLINED", offerStatus(second));
    }

    @Test
    void aWithdrawnOfferCannotBeAccepted() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "WITHDRAWN", 10);
        loginAs(GROUP);
        assertRule("Only an Offered offer can be accepted.", () -> acceptance.accept(offer));
        assertEquals("OPEN", postStatus(Fixtures.seedRequestId()));
    }

    @Test
    void aSuspendedPosterCannotAccept() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", BUSINESS);
        loginAs(BUSINESS);
        assertRule("A suspended account cannot accept offers.", () -> acceptance.accept(offer));
        assertEquals("OFFERED", offerStatus(offer));
    }

    @Test
    void anOfferFromASuspendedOffererOrPastItsPickupCannotBeAccepted() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(BUSINESS);

        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", GROUP);
        assertRule("This offer is not available right now.", () -> acceptance.accept(offer));

        TestDb.exec("UPDATE account SET status = 'ACTIVE', suspend_reason = NULL WHERE email = ?", GROUP);
        TestDb.exec("UPDATE use_detail SET due_at = DATE_SUB(NOW(), INTERVAL 1 HOUR)");
        assertRule("This offer has expired.", () -> acceptance.accept(offer));
        assertEquals("OPEN", postStatus(Fixtures.seedListingId()));
    }

    //---DECLINE---

    @Test
    void decliningOneOfferLeavesThePostOpenAndLogsIt() {
        int first = Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        int second = Fixtures.offerOnSeedRequest(CASUAL, "OFFERED", 5);
        loginAs(GROUP);

        acceptance.decline(first);

        assertEquals("DECLINED", offerStatus(first));
        assertEquals("OFFERED", offerStatus(second));
        assertEquals("OPEN", postStatus(Fixtures.seedRequestId()));
        assertEquals(1L, logged("OFFER", first, "OFFERED", "DECLINED"));
        assertRule("Only an Offered offer can be declined.", () -> acceptance.decline(first));
    }

    //---THE POSTER'S LIST---

    @Test
    void thePosterSeesItsOffersWithoutWithdrawnOnesOrSuspendedOfferers() {
        int shown = Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        Fixtures.offerOnSeedRequest(CASUAL, "WITHDRAWN", 5);
        loginAs(GROUP);

        List<Offer> offers = acceptance.getIncomingOffers();
        assertEquals(1, offers.size());
        assertEquals(shown, offers.get(0).id);
        assertEquals("Sunrise Bakery", offers.get(0).offererName);
        assertEquals("Kapwa Community Kitchen", offers.get(0).post.posterName);

        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", BUSINESS);
        assertTrue(acceptance.getIncomingOffers().isEmpty());
        TestDb.exec("UPDATE account SET status = 'ACTIVE', suspend_reason = NULL WHERE email = ?", BUSINESS);
        assertEquals(1, acceptance.getIncomingOffers().size());
    }

    @Test
    void aDonorsHandoverTextIsHiddenFromTheGroupUntilItAcceptsTheOffer() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        TestDb.exec("UPDATE food_detail SET handover_instructions = 'Gate B' WHERE description = 'Rice'");
        loginAs(GROUP);
        assertNull(acceptance.getIncomingOffers().get(0).food.handoverInstructions);
        acceptance.accept(offer);
        assertEquals("Gate B", acceptance.getIncomingOffers().get(0).food.handoverInstructions);
    }

    @Test
    void nobodyButThePosterSeesTheOffersOnItsPost() {
        Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        loginAs(CASUAL);
        assertTrue(acceptance.getIncomingOffers().isEmpty());
        loginAs(BUSINESS);
        assertTrue(acceptance.getIncomingOffers().isEmpty());
    }

    @Test
    void theBadgeCountsOfferedOffersOnOpenPostsFromActiveOfferers() {
        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(BUSINESS);
        assertEquals(1, acceptance.countOffersAwaitingDecision());
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", GROUP);
        assertEquals(0, acceptance.countOffersAwaitingDecision());
    }

    //---CONTACT NUMBERS---

    @Test
    void theOtherSideSeesTheNumberOnlyWhileTheOfferIsAccepted() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        int donor = accountId(BUSINESS);
        int group = accountId(GROUP);
        String message = "You can see this contact number only on an accepted offer.";

        loginAs(BUSINESS);
        assertRule(message, () -> acceptance.getContactPhone(group, offer));   // not accepted yet
        acceptance.accept(offer);
        assertEquals("09181234567", acceptance.getContactPhone(group, offer));

        loginAs(GROUP);
        assertEquals("09171234567", acceptance.getContactPhone(donor, offer));

        TestDb.exec("UPDATE offer SET status = 'COMPLETED' WHERE offer_id = ?", offer);
        assertRule(message, () -> acceptance.getContactPhone(donor, offer));   // the handoff has closed
        assertEquals("09181234567", acceptance.getContactPhone(group, offer));   // your own number is always yours
    }

    @Test
    void aThirdPartyNeverSeesANumberButAnAdminMay() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        int donor = accountId(BUSINESS);
        loginAs(CASUAL);
        assertRule("You can see this contact number only on an accepted offer.", () -> acceptance.getContactPhone(donor, offer));
        loginAs(ADMIN);
        assertEquals("09171234567", acceptance.getContactPhone(donor, offer));
    }
}
