package com.aniplato.controller;

import com.aniplato.model.FoodDetail;
import com.aniplato.support.Fx;
import javafx.scene.Node;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
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
    void aCasualDonorSeesOnlyRawAndPackagedInTheCategoryChoices() {
        loginAs(CASUAL);
        Parent form = open("donor/new-listing.fxml");
        @SuppressWarnings("unchecked")
        ComboBox<FoodDetail.Category> categories = (ComboBox<FoodDetail.Category>) all(form, ComboBox.class).get(0);
        assertEquals(List.of(FoodDetail.Category.RAW, FoodDetail.Category.PACKAGED), categories.getItems());
        loginAs(BUSINESS);
        @SuppressWarnings("unchecked")
        ComboBox<FoodDetail.Category> every = (ComboBox<FoodDetail.Category>) all(open("donor/new-listing.fxml"), ComboBox.class).get(0);
        assertEquals(4, every.getItems().size());
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
        button(screen, "Confirm release");
        button(screen, "Cancel");
        assertFalse(texts(screen).contains("Confirm receipt"));
        assertEquals("ACCEPTED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", offer));
    }

    @Test
    void myRequestsOffersConfirmReceiptOnlyAfterRelease() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'REQUEST'");
        loginAs(GROUP);
        assertFalse(texts(open("foodgroup/my-requests.fxml")).contains("Confirm receipt"));

        TestDb.exec("UPDATE offer SET released_at = NOW() WHERE offer_id = ?", offer);
        button(open("foodgroup/my-requests.fxml"), "Confirm receipt");
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
