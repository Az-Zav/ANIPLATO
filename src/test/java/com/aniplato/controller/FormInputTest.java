package com.aniplato.controller;

import com.aniplato.model.FoodDetail;
import com.aniplato.service.OfferService;
import com.aniplato.service.OfferService.ListingOfferForm;
import com.aniplato.service.OfferService.RequestOfferForm;
import com.aniplato.service.PostService;
import com.aniplato.service.PostService.ListingForm;
import com.aniplato.service.PostService.RequestForm;
import com.aniplato.service.ReportService;
import com.aniplato.service.ReportService.ReportForm;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A form's unreadable number or time becomes a placeholder; the services must refuse each placeholder with exactly the
 * message FormInput maps to the helpful one. If a service changes its words, this test fails instead of the screen
 * silently going back to showing the wrong error first.
 */
class FormInputTest extends DbTest {

    @TempDir
    Path tmp;

    private final PostService posts = new PostService();
    private final OfferService offers = new OfferService();

    private final LocalDateTime now = LocalDateTime.now();

    // ---------- the mapping itself ----------

    @Test
    void anUnreadableNumberIsReportedByNameInPlaceOfTheServicesGenericText() {
        FormInput input = new FormInput();
        assertTrue(Double.isNaN(input.number("25kg", "the weight", FormInput.WEIGHT)));
        assertEquals("Enter a number for the weight.", input.say(FormInput.WEIGHT));
        assertEquals("Describe the food.", input.say("Describe the food."), "any other refusal is shown as it is");
    }

    @Test
    void aReadableNumberAddsNothing() {
        FormInput input = new FormInput();
        assertEquals(12.5, input.number(" 12.5 ", "the weight", FormInput.WEIGHT));
        assertEquals(7, input.whole("7", "how many people", FormInput.HEADCOUNT, 0));
        assertEquals(FormInput.WEIGHT, input.say(FormInput.WEIGHT));
    }

    @Test
    void aWholeNumberThatIsNotOneGetsItsPlaceholderAndItsOwnMessage() {
        FormInput input = new FormInput();
        assertEquals(0, input.whole("about 80", "how many people", FormInput.HEADCOUNT, 0));
        assertEquals("Enter a whole number for how many people.", input.say(FormInput.HEADCOUNT));
    }

    // ---------- the services still say what FormInput expects ----------

    private ListingForm listing(double kg, LocalDateTime useBy, LocalDateTime from, LocalDateTime to, String description) {
        return new ListingForm(description, FoodDetail.Category.BAKED, FoodDetail.Storage.AMBIENT_DRY, Set.of(), kg, useBy, from, to,
                null, true, Fixtures.photos(tmp, 1));
    }

    @Test
    void aListingRefusesEachPlaceholderWithTheExpectedMessageInFormOrder() {
        loginAs(BUSINESS);
        assertRule(FormInput.WEIGHT, () -> posts.createListing(listing(Double.NaN, now.plusDays(2), now.plusHours(2), now.plusHours(5), "Bread")));
        assertRule(FormInput.USE_BY, () -> posts.createListing(listing(5, null, now.plusHours(2), now.plusHours(5), "Bread")));
        assertRule(FormInput.PICKUP_WINDOW, () -> posts.createListing(listing(5, now.plusDays(2), null, now.plusHours(5), "Bread")));
        assertRule(FormInput.PICKUP_WINDOW, () -> posts.createListing(listing(5, now.plusDays(2), now.plusHours(2), null, "Bread")));
        // the description is checked before any of them: an empty form says "Describe the food." first
        assertRule("Describe the food.", () -> posts.createListing(listing(Double.NaN, null, null, null, "")));
    }

    @Test
    void aRequestRefusesAnUnreadableHeadcountAndNeededBy() {
        loginAs(GROUP);
        assertRule(FormInput.HEADCOUNT, () -> posts.createRequest(new RequestForm("Rice", "Event", LocalDate.now().plusDays(9), "Kids", 0,
                "Hall", now.plusDays(3), null, Set.of(FoodDetail.Category.RAW), 10, true, Fixtures.photos(tmp, 1))));
        assertRule(FormInput.NEEDED_BY, () -> posts.createRequest(new RequestForm("Rice", "Event", LocalDate.now().plusDays(9), "Kids", 20,
                "Hall", null, null, Set.of(FoodDetail.Category.RAW), 10, true, Fixtures.photos(tmp, 1))));
    }

    @Test
    void aListingOfferRefusesAnUnreadableHeadcountAndPickupTime() {
        loginAs(GROUP);
        int post = Fixtures.seedListingId();
        assertRule(FormInput.HEADCOUNT, () -> offers.sendListingOffer(new ListingOfferForm(post, "Use", null, "Kids", 0, "Hall",
                now.plusHours(36), null, true, Fixtures.photos(tmp, 1))));
        assertRule(FormInput.PICKUP_TIME, () -> offers.sendListingOffer(new ListingOfferForm(post, "Use", null, "Kids", 20, "Hall",
                null, null, true, Fixtures.photos(tmp, 1))));
    }

    @Test
    void aRequestOfferRefusesAnUnreadableWeightAndTimes() {
        loginAs(BUSINESS);
        int post = Fixtures.seedRequestId();
        assertRule(FormInput.WEIGHT, () -> offers.sendRequestOffer(new RequestOfferForm(post, "Rice", FoodDetail.Category.RAW,
                FoodDetail.Storage.AMBIENT_DRY, Set.of(), Double.NaN, now.plusDays(10), now.plusDays(1), now.plusDays(2), null, true, Fixtures.photos(tmp, 1))));
        assertRule(FormInput.USE_BY, () -> offers.sendRequestOffer(new RequestOfferForm(post, "Rice", FoodDetail.Category.RAW,
                FoodDetail.Storage.AMBIENT_DRY, Set.of(), 5, null, now.plusDays(1), now.plusDays(2), null, true, Fixtures.photos(tmp, 1))));
        assertRule(FormInput.PICKUP_WINDOW, () -> offers.sendRequestOffer(new RequestOfferForm(post, "Rice", FoodDetail.Category.RAW,
                FoodDetail.Storage.AMBIENT_DRY, Set.of(), 5, now.plusDays(10), null, now.plusDays(2), null, true, Fixtures.photos(tmp, 1))));
    }

    @Test
    void aReportRefusesAnUnreadablePeopleServed() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", offer);
        loginAs(GROUP);
        assertRule(FormInput.PEOPLE_SERVED, () -> new ReportService().submitReport(new ReportForm(offer, "Event", LocalDate.now(), -1,
                "Served", true, Fixtures.photos(tmp, 1))));
    }
}
