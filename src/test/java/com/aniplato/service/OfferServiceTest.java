package com.aniplato.service;

import com.aniplato.model.FoodDetail;
import com.aniplato.model.Offer;
import com.aniplato.service.OfferService.ListingOfferForm;
import com.aniplato.service.OfferService.RequestOfferForm;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 3: sending and withdrawing offers on both routes (R3.1 to R3.3), and the offerer's own list. */
class OfferServiceTest extends DbTest {

    private final OfferService offers = new OfferService();

    @TempDir
    Path tmp;

    //---FORM BUILDERS---

    private ListingOfferForm listingOffer(int postId, LocalDateTime pickupAt) {
        return new ListingOfferForm(postId, "Weekend feeding", null, "Street children", 40, "Quezon City hall",
                pickupAt, "  See you  ", true, Fixtures.photos(tmp, 2));
    }

    /** A pickup time inside the seed listing's window (tomorrow to the day after). */
    private LocalDateTime insideWindow() {
        return LocalDateTime.now().plusHours(36);
    }

    private RequestOfferForm requestOffer(int postId, FoodDetail.Category category, double kg) {
        LocalDateTime now = LocalDateTime.now();
        return new RequestOfferForm(postId, "Rice sacks", category, FoodDetail.Storage.AMBIENT_DRY, Set.of(), kg,
                now.plusDays(10), now.plusDays(1), now.plusDays(2), "  Gate B  ", true, Fixtures.photos(tmp, 1));
    }

    private String offerStatus(int id) {
        return TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", id);
    }

    //---ROUTE 1: A FOOD GROUP OFFERS ON A LISTING---

    @Test
    void aFoodGroupOffersForTheWholeListingWithItsDetailPhotosAndLog() {
        int groupId = loginAs(GROUP).id;
        int listing = Fixtures.seedListingId();

        int id = offers.sendListingOffer(listingOffer(listing, insideWindow()));

        assertEquals("OFFERED", offerStatus(id));
        assertEquals(12.5, ((Number) TestDb.scalar("SELECT kg FROM offer WHERE offer_id = ?", id)).doubleValue());
        assertEquals((long) groupId, TestDb.count("SELECT offerer_id FROM offer WHERE offer_id = ?", id));
        assertEquals("See you", TestDb.scalar("SELECT message FROM use_detail ud JOIN offer o ON o.use_detail_id = ud.use_detail_id WHERE o.offer_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM use_detail ud JOIN offer o ON o.use_detail_id = ud.use_detail_id "
                + "WHERE o.offer_id = ? AND ud.attested_at IS NOT NULL", id));
        assertEquals(2L, TestDb.count("SELECT COUNT(*) FROM photo WHERE offer_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? "
                + "AND from_status IS NULL AND to_status = 'OFFERED' AND changed_by = ?", id, groupId));
    }

    @Test
    void thePickupTimeMustBeInsideTheListingsWindowAndBeforeTheUseBy() {
        loginAs(GROUP);
        int listing = Fixtures.seedListingId();
        String message = "Pick a pickup time inside the listing's pickup window and before its use-by.";

        assertRule(message, () -> offers.sendListingOffer(listingOffer(listing, LocalDateTime.now().plusHours(1))));
        assertRule(message, () -> offers.sendListingOffer(listingOffer(listing, LocalDateTime.now().plusDays(2).plusHours(5))));
        assertRule(message, () -> offers.sendListingOffer(listingOffer(listing, null)));
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM offer"));
    }

    @Test
    void anOfferNeedsItsFactsPhotosAndTheAttestation() {
        loginAs(GROUP);
        int listing = Fixtures.seedListingId();
        LocalDateTime at = insideWindow();

        assertRule("Say how the food will be used.", () -> offers.sendListingOffer(new ListingOfferForm(listing, " ", null, "x", 1, "x", at, null, true, Fixtures.photos(tmp, 1))));
        assertRule("How many people it feeds must be more than 0.", () -> offers.sendListingOffer(new ListingOfferForm(listing, "x", null, "x", 0, "x", at, null, true, Fixtures.photos(tmp, 1))));
        assertRule("Tick the attestation to continue.", () -> offers.sendListingOffer(new ListingOfferForm(listing, "x", null, "x", 1, "x", at, null, false, Fixtures.photos(tmp, 1))));
        assertRule("Add 1 to 5 photos.", () -> offers.sendListingOffer(new ListingOfferForm(listing, "x", null, "x", 1, "x", at, null, true, List.of())));
    }

    @Test
    void aGroupOffersOnceOnAListing() {
        loginAs(GROUP);
        int listing = Fixtures.seedListingId();
        offers.sendListingOffer(listingOffer(listing, insideWindow()));

        assertRule("You have already offered on this listing.", () -> offers.sendListingOffer(listingOffer(listing, insideWindow())));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM offer"));
    }

    @Test
    void aDeclinedOfferCannotBeSentAgain() {
        loginAs(GROUP);
        int listing = Fixtures.seedListingId();
        int id = offers.sendListingOffer(listingOffer(listing, insideWindow()));
        TestDb.exec("UPDATE offer SET status = 'DECLINED' WHERE offer_id = ?", id);

        assertRule("Your offer on this listing was declined, so you cannot offer again.",
                () -> offers.sendListingOffer(listingOffer(listing, insideWindow())));
    }

    @Test
    void onlyAnActiveFoodGroupOffersOnAnOpenListing() {
        int listing = Fixtures.seedListingId();
        loginAs(BUSINESS);
        assertRule("Only a food group can offer on a listing.", () -> offers.sendListingOffer(listingOffer(listing, insideWindow())));

        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", GROUP);
        loginAs(GROUP);
        assertRule("A suspended account cannot send offers.", () -> offers.sendListingOffer(listingOffer(listing, insideWindow())));

        TestDb.exec("UPDATE account SET status = 'ACTIVE', suspend_reason = NULL WHERE email = ?", GROUP);
        loginAs(GROUP);
        assertRule("That listing no longer exists.", () -> offers.sendListingOffer(listingOffer(Fixtures.seedRequestId(), insideWindow())));
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE post_id = ?", listing);
        assertRule("This listing is no longer open.", () -> offers.sendListingOffer(listingOffer(listing, insideWindow())));
    }

    //---ROUTE 2: A DONOR OFFERS ON A REQUEST---

    @Test
    void aDonorOffersOnARequestWithFoodDetailPhotosAndLog() {
        int donorId = loginAs(BUSINESS).id;
        int request = Fixtures.seedRequestId();

        int id = offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.BAKED, 10));

        assertEquals("OFFERED", offerStatus(id));
        assertEquals(10.0, ((Number) TestDb.scalar("SELECT kg FROM offer WHERE offer_id = ?", id)).doubleValue());
        assertEquals("Gate B", TestDb.scalar("SELECT handover_instructions FROM food_detail fd JOIN offer o ON o.food_detail_id = fd.food_detail_id WHERE o.offer_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM photo WHERE offer_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? AND changed_by = ?", id, donorId));
    }

    @Test
    void aRequestOfferMayBeSmallerButNotLargerThanRequested() {
        loginAs(BUSINESS);
        int request = Fixtures.seedRequestId();

        assertRule("You cannot offer more than the 30.0 kg requested.", () -> offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.RAW, 31)));
        assertRule("The weight must be more than 0 kg.", () -> offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.RAW, 0)));
        assertTrue(offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.RAW, 5)) > 0);
    }

    @Test
    void theCategoryMustBeOneTheRequestAccepts() {
        loginAs(BUSINESS);
        int request = Fixtures.seedRequestId();
        assertRule("This request does not accept cooked food.", () -> offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.COOKED, 5)));
    }

    @Test
    void aCasualDonorOffersRawOrPackagedOnly() {
        loginAs(CASUAL);
        int request = Fixtures.seedRequestId();
        assertRule("Casual donors can give raw or sealed-packaged food only.", () -> offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.BAKED, 5)));
        assertTrue(offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.PACKAGED, 5)) > 0);
    }

    @Test
    void thePickupWindowMustEndBeforeTheNeededByAndTheUseBy() {
        loginAs(BUSINESS);
        int request = Fixtures.seedRequestId();
        LocalDateTime now = LocalDateTime.now();
        FoodDetail.Category raw = FoodDetail.Category.RAW;

        assertRule("The pickup window must end before the request's needed-by time.", () -> offers.sendRequestOffer(new RequestOfferForm(
                request, "x", raw, FoodDetail.Storage.CHILLED, Set.of(), 5, now.plusDays(20), now.plusDays(4), now.plusDays(6), null, true, Fixtures.photos(tmp, 1))));
        assertRule("The pickup window must end before the use-by.", () -> offers.sendRequestOffer(new RequestOfferForm(
                request, "x", raw, FoodDetail.Storage.CHILLED, Set.of(), 5, now.plusDays(1), now.plusHours(2), now.plusDays(2), null, true, Fixtures.photos(tmp, 1))));
        assertRule("Pick a storage condition.", () -> offers.sendRequestOffer(new RequestOfferForm(
                request, "x", raw, null, Set.of(), 5, now.plusDays(3), now.plusHours(2), now.plusDays(1), null, true, Fixtures.photos(tmp, 1))));
    }

    @Test
    void aDonorOffersOnceOnARequestAndOnlyDonorsDo() {
        int request = Fixtures.seedRequestId();
        loginAs(BUSINESS);
        offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.RAW, 5));
        assertRule("You have already offered on this request.", () -> offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.RAW, 5)));
        loginAs(GROUP);
        assertRule("Only a donor can offer on a request.", () -> offers.sendRequestOffer(requestOffer(request, FoodDetail.Category.RAW, 5)));
    }

    //---WITHDRAW---

    @Test
    void theOffererWithdrawsAndItIsLogged() {
        int groupId = loginAs(GROUP).id;
        int id = offers.sendListingOffer(listingOffer(Fixtures.seedListingId(), insideWindow()));

        offers.withdraw(id);

        assertEquals("WITHDRAWN", offerStatus(id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? "
                + "AND from_status = 'OFFERED' AND to_status = 'WITHDRAWN' AND changed_by = ?", id, groupId));
        assertRule("This offer can no longer be withdrawn.", () -> offers.withdraw(id));
    }

    @Test
    void onlyTheOffererWithdrawsAndNotOnceAccepted() {
        loginAs(GROUP);
        int id = offers.sendListingOffer(listingOffer(Fixtures.seedListingId(), insideWindow()));

        loginAs(BUSINESS);
        assertRule("You can only withdraw your own offer.", () -> offers.withdraw(id));
        loginAs(GROUP);
        TestDb.exec("UPDATE offer SET status = 'ACCEPTED' WHERE offer_id = ?", id);
        assertRule("Once accepted, an offer cannot be withdrawn. Only the poster can cancel.", () -> offers.withdraw(id));
        assertRule("That offer no longer exists.", () -> offers.withdraw(99999));
        assertEquals("ACCEPTED", offerStatus(id));
    }

    @Test
    void aWithdrawnOfferIsReopenedOnTheSameRowWithNewDetailsAndPhotos() {
        int groupId = loginAs(GROUP).id;
        int listing = Fixtures.seedListingId();
        int first = offers.sendListingOffer(listingOffer(listing, insideWindow()));
        offers.withdraw(first);
        long detailsBefore = TestDb.count("SELECT COUNT(*) FROM use_detail");

        int second = offers.sendListingOffer(new ListingOfferForm(listing, "New plan", null, "Elderly", 10, "Hall", insideWindow(),
                null, true, Fixtures.photos(tmp, 1)));

        assertEquals(first, second);
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM offer"));
        assertEquals("OFFERED", offerStatus(first));
        assertEquals("New plan", TestDb.scalar("SELECT use_description FROM use_detail ud JOIN offer o ON o.use_detail_id = ud.use_detail_id WHERE o.offer_id = ?", first));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM photo WHERE offer_id = ?", first));
        assertEquals(detailsBefore, TestDb.count("SELECT COUNT(*) FROM use_detail"));   // the old detail was replaced, not kept
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? "
                + "AND from_status = 'WITHDRAWN' AND to_status = 'OFFERED' AND changed_by = ?", first, groupId));
    }

    //---MY OFFERS---

    @Test
    void myOffersAreMyOwnWithThePostAndTheHandoverTextHiddenUntilAccepted() {
        TestDb.exec("UPDATE food_detail SET handover_instructions = 'Ring the bell'");
        loginAs(GROUP);
        int id = offers.sendListingOffer(listingOffer(Fixtures.seedListingId(), insideWindow()));

        List<Offer> mine = offers.getMyOffers();

        assertEquals(1, mine.size());
        assertEquals(id, mine.get(0).id);
        assertEquals("Sunrise Bakery", mine.get(0).post.posterName);
        assertEquals(2, mine.get(0).photos.size());
        assertEquals(1, mine.get(0).post.photos.size());
        assertNull(mine.get(0).post.food.handoverInstructions);

        TestDb.exec("UPDATE offer SET status = 'ACCEPTED' WHERE offer_id = ?", id);
        assertEquals("Ring the bell", offers.getMyOffers().get(0).post.food.handoverInstructions);

        loginAs(BUSINESS);
        assertTrue(offers.getMyOffers().isEmpty());
    }
}
