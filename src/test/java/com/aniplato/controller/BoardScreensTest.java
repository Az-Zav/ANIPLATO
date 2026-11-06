package com.aniplato.controller;

import com.aniplato.model.FoodDetail;
import com.aniplato.util.Selection;
import com.aniplato.support.Fx;
import javafx.scene.Node;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The boards and the donor's and food group's own post lists, loaded for real: they open, show what the services return, and the buttons call the services. */
class BoardScreensTest extends ScreenTestBase {

    //---BOARDS---

    @Test
    void theFoodGroupSeesTheOpenListingWithAMakeOfferButton() {
        loginAs(GROUP);
        Parent screen = open("foodgroup/browse-listings.fxml");
        assertTrue(mentions(screen, "Day-old pandesal and loaf bread"));
        assertTrue(mentions(screen, "Sunrise Bakery"));
        button(screen, "Make offer");
    }

    @Test
    void anOfferedListingShowsADisabledOfferedButton() {
        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(GROUP);
        Parent screen = open("foodgroup/browse-listings.fxml");
        assertTrue(button(screen, "Offered").isDisabled());
    }

    @Test
    void aCasualDonorSeesOnlyRawAndPackagedInTheCategoryChips() {
        loginAs(CASUAL);
        Parent form = open("donor/new-listing.fxml");
        @SuppressWarnings("unchecked")
        ChipChoice<FoodDetail.Category> categories = (ChipChoice<FoodDetail.Category>) all(form, ChipChoice.class).get(0);
        assertEquals(List.of(FoodDetail.Category.RAW, FoodDetail.Category.PACKAGED), categories.getItems());
        loginAs(BUSINESS);
        @SuppressWarnings("unchecked")
        ChipChoice<FoodDetail.Category> every = (ChipChoice<FoodDetail.Category>) all(open("donor/new-listing.fxml"), ChipChoice.class).get(0);
        assertEquals(4, every.getItems().size());
    }

    @Test
    void theListingFormHasStorageCardsAllergenChipsAndPhotoTiles() {
        loginAs(BUSINESS);
        Parent form = open("donor/new-listing.fxml");
        assertEquals(3, all(form, CardChoice.class).get(0).getItems().size());
        assertEquals("Room temperature (22–28 °C), sealed containers.", CardChoice.storageHint(FoodDetail.Storage.AMBIENT_DRY));
        assertEquals(3, all(form, javafx.scene.control.ToggleButton.class).stream().filter(b -> b.getStyleClass().contains("choice-card")).count());        assertTrue(mentions(form, "Quantity & timing"));
        assertEquals(1, all(form, javafx.scene.control.Button.class).stream().filter(b -> b.getStyleClass().contains("photo-add")).count(), "the Add photo tile");
        long chips = all(form, javafx.scene.control.CheckBox.class).stream().filter(c -> c.getStyleClass().contains("chip-check")).count();
        assertEquals(5, chips, "one chip per allergen");
    }

    @Test
    void theMakeOfferFormsStartWithASummaryOfThePostBeingOfferedOn() {
        loginAs(GROUP);
        Selection.selectPost(Fixtures.seedListingId());
        Parent group = open("foodgroup/make-offer.fxml");
        assertTrue(mentions(group, "Day-old pandesal and loaf bread"));
        assertTrue(mentions(group, "QUANTITY"));
        assertTrue(mentions(group, "USE-BY"));
        assertTrue(mentions(group, "Open"));

        loginAs(BUSINESS);
        Selection.selectPost(Fixtures.seedRequestId());
        Parent donor = open("donor/make-offer.fxml");
        assertTrue(mentions(donor, "NEEDED-BY"));
        assertTrue(mentions(donor, "EVENT"));
        assertEquals(1, all(donor, ChipChoice.class).size());
    }

    //---MY POSTS---

    @Test
    void myListingsShowsTheDonorsPostAndAnAcceptedOneShowsContactAndRelease() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        loginAs(BUSINESS);

        Parent screen = open("donor/my-listings.fxml");

        assertTrue(mentions(screen, "Day-old pandesal and loaf bread"));
        assertTrue(mentions(screen, "09181234567"), "the group's number is shown on an accepted offer");
        button(screen, "Confirm Release");
        button(screen, "Cancel listing");
        assertTrue(all(screen, javafx.scene.control.ButtonBase.class).stream()
                .noneMatch(b -> b.getStyleClass().contains("row-button") && "Confirm Receipt".equals(b.getText())), "a donor never confirms receipt");
        assertEquals("ACCEPTED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", offer));
    }

    @Test
    void myRequestsOffersConfirmReceiptOnlyAfterRelease() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'REQUEST'");
        loginAs(GROUP);
        assertTrue(button(open("foodgroup/my-requests.fxml"), "Confirm Receipt").isDisabled(), "waiting for the donor's release");

        TestDb.exec("UPDATE offer SET released_at = NOW() WHERE offer_id = ?", offer);
        assertFalse(button(open("foodgroup/my-requests.fxml"), "Confirm Receipt").isDisabled());
    }

    @Test
    void myListingsIsABoardWithTilesTabsAndAnOpenRow() {
        loginAs(BUSINESS);
        Parent screen = open("donor/my-listings.fxml");
        assertTrue(mentions(screen, "OFFERS TO REVIEW"));
        assertTrue(mentions(screen, "OPEN ON BOARD"));
        assertTrue(mentions(screen, "All (1)"));
        assertTrue(mentions(screen, "Open (1)"));
        assertTrue(mentions(screen, "No offers yet"));
        assertTrue(mentions(screen, "Rescued, expired and cancelled listings move to History."));
        button(screen, "Cancel listing");

        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        Parent withOffer = open("donor/my-listings.fxml");
        assertTrue(mentions(withOffer, "1 offer"));
        button(withOffer, "Review offers");
    }

    @Test
    void myOffersShowsTheReleaseColumnAndWaitsForTheDonor() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        loginAs(GROUP);
        Parent waiting = open("foodgroup/my-offers.fxml");
        assertTrue(mentions(waiting, "DONOR RELEASE"));
        assertTrue(mentions(waiting, "Waiting for donor release"));
        assertTrue(button(waiting, "Confirm Receipt").isDisabled());

        TestDb.exec("UPDATE offer SET released_at = NOW() WHERE offer_id = ?", offer);
        Parent released = open("foodgroup/my-offers.fxml");
        assertTrue(mentions(released, "Confirmed"));
        assertFalse(button(released, "Confirm Receipt").isDisabled());
    }

    //---PHOTOS---

    @Test
    void aStoredPhotoShowsAsAPictureAndAMissingOneAsANote() throws java.io.IOException {
        Fx.start();
        Node missing = Fx.call(() -> PostCards.thumbnails(java.util.List.of("seed/bread.jpg")));
        assertTrue(mentions((Parent) missing, "photo not found: seed/bread.jpg"));
        assertEquals(0, all(missing, javafx.scene.image.ImageView.class).size());

        java.nio.file.Path seed = java.nio.file.Path.of(System.getProperty("aniplato.uploads.dir"), "seed");
        java.nio.file.Files.createDirectories(seed);
        java.nio.file.Files.copy(java.nio.file.Path.of("sql", "demo-photos", "bread.jpg"), seed.resolve("bread.jpg"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        Node shown = Fx.call(() -> PostCards.thumbnails(java.util.List.of("seed/bread.jpg")));
        assertEquals(1, all(shown, javafx.scene.image.ImageView.class).size());
        assertFalse(mentions((Parent) shown, "photo not found"));
    }

    //---THE BROWSE BOARDS---

    @Test
    void theBrowseBoardShowsTilesTabsAndTheCityBand() {
        loginAs(GROUP);
        Parent screen = open("foodgroup/browse-listings.fxml");
        assertTrue(mentions(screen, "OPEN SURPLUS"));
        assertTrue(mentions(screen, "KG AVAILABLE"));
        assertTrue(mentions(screen, "All (1)"));
        assertTrue(mentions(screen, "IN QUEZON CITY · 1 LISTING"));
        assertTrue(mentions(screen, "Refresh"));
    }

    @Test
    void aDonorsBrowseBoardListsTheOpenRequestsWithTheirGroupAndNeededBy() {
        Fixtures.seedRequestId();
        loginAs(BUSINESS);
        Parent screen = open("donor/browse-request.fxml");
        assertTrue(mentions(screen, "OPEN REQUESTS"));
        assertTrue(mentions(screen, "NEEDED-BY"));
        button(screen, "Make offer");
    }
}
