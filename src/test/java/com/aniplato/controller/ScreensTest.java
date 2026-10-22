package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.FoodDetail;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.Fx;
import com.aniplato.support.TestDb;
import com.aniplato.util.Selection;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The wired round 3 screens, loaded for real: they open, show what the services return, and the buttons call the services. */
class ScreensTest extends DbTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private static Parent open(String view) {
        return Fx.call(() -> {
            try {
                return new FXMLLoader(DialogController.class.getResource("/com/aniplato/view/" + view)).load();
            } catch (Exception e) {
                throw new IllegalStateException(view, e);
            }
        });
    }

    /** Every label and button text under the node. */
    private static List<String> texts(Node node) {
        List<String> found = new ArrayList<>();
        collect(node, found);
        return found;
    }

    private static void collect(Node node, List<String> found) {
        if (node instanceof Labeled labeled && labeled.getText() != null) found.add(labeled.getText());
        if (node instanceof ScrollPane pane && pane.getContent() != null) collect(pane.getContent(), found);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> collect(child, found));
    }

    /** Every node of the type under the node. A ScrollPane shows its content only once skinned, so it is walked by hand. */
    private static <T extends Node> List<T> all(Node node, Class<T> type) {
        List<T> found = new ArrayList<>();
        gather(node, type, found);
        return found;
    }

    private static <T extends Node> void gather(Node node, Class<T> type, List<T> found) {
        if (type.isInstance(node)) found.add(type.cast(node));
        if (node instanceof ScrollPane pane && pane.getContent() != null) gather(pane.getContent(), type, found);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> gather(child, type, found));
    }

    private static boolean mentions(Node screen, String text) {
        return texts(screen).stream().anyMatch(t -> t.contains(text));
    }

    private static Button button(Node screen, String text) {
        return all(screen, Button.class).stream().filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("no button " + text + " in " + texts(screen)));
    }

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

    //---DASHBOARDS AND HISTORY---

    @Test
    void theDonorDashboardShowsTheCardsTheActiveListingsAndABannerWhenSuspended() {
        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'Unsafe food reported' WHERE email = ?", BUSINESS);
        loginAs(BUSINESS);

        Parent screen = open("donor/dashboard.fxml");

        assertTrue(mentions(screen, "Welcome, Sunrise Bakery"));
        assertTrue(mentions(screen, "Quezon City"));
        assertTrue(mentions(screen, "1 offer to review"));
        assertTrue(mentions(screen, "30 kg"), "what food groups need");
        assertTrue(mentions(screen, "Day-old pandesal and loaf bread"));
        assertTrue(mentions(screen, "Unsafe food reported"), "the suspension banner");
    }

    @Test
    void theFoodGroupDashboardListsOpenListingsAndAReportBanner() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", offer);
        loginAs(GROUP);

        Parent screen = open("foodgroup/dashboard.fxml");

        assertTrue(mentions(screen, "Welcome, Kapwa Community Kitchen"));
        assertTrue(mentions(screen, "1 report due by"));
        assertTrue(mentions(screen, "12.5 kg waiting"));
        button(screen, "Make offer");
    }

    @Test
    void historyShowsTheClosedHandoffWithTabCountsAndSearch() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", offer);
        TestDb.exec("UPDATE post SET status = 'RESCUED' WHERE kind = 'LISTING'");
        loginAs(GROUP);

        Parent screen = open("shared/history.fxml");

        assertTrue(mentions(screen, "All (1)"));
        assertTrue(mentions(screen, "Rescued (1)"));
        assertTrue(mentions(screen, "Expired (0)"));
        assertTrue(mentions(screen, "Day-old pandesal and loaf bread"));
        assertTrue(mentions(screen, "Sunrise Bakery"));
        assertTrue(mentions(screen, "View record"));
        assertTrue(mentions(screen, "Due "), "the food group's report column");

        Fx.run(() -> button(screen, "Expired (0)").fire());
        assertFalse(mentions(screen, "Day-old pandesal and loaf bread"));
        Fx.run(() -> button(screen, "All (1)").fire());
        all(screen, javafx.scene.control.TextField.class).forEach(f -> Fx.run(() -> f.setText("nobody")));
        assertFalse(mentions(screen, "Day-old pandesal and loaf bread"));
    }

    @Test
    void theDonorsHistoryHasNoReportColumn() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", offer);
        TestDb.exec("UPDATE post SET status = 'RESCUED' WHERE kind = 'LISTING'");
        loginAs(BUSINESS);
        Parent screen = open("shared/history.fxml");
        assertTrue(mentions(screen, "Kapwa Community Kitchen"));
        assertFalse(mentions(screen, "REPORT"));
    }

    //---PROFILE: UPGRADE, EDIT, RENEWAL---

    @Test
    void onlyACasualDonorSeesTheUpgradeButtonOnTheProfile() {
        loginAs(CASUAL);
        assertTrue(all(open("profile.fxml"), Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged()
                && "Upgrade to Business donor".equals(b.getText())));
        loginAs(BUSINESS);
        assertFalse(all(open("profile.fxml"), Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged()
                && "Upgrade to Business donor".equals(b.getText())));
        loginAs(GROUP);
        assertFalse(all(open("profile.fxml"), Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged()
                && "Upgrade to Business donor".equals(b.getText())));
    }

    @Test
    void theAccountsListOffersReviewRenewalOnlyWhereAFileWaits() {
        loginAs(BUSINESS);
        new com.aniplato.service.DocumentReviewService().uploadRenewal(com.aniplato.model.Document.Type.MAYORS_PERMIT,
                Fixtures.photos(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")), 1).get(0), java.time.LocalDate.now().plusYears(1), false);
        loginAs(ADMIN);
        Parent screen = open("admin/accounts.fxml");
        assertEquals(1, all(screen, Button.class).stream().filter(b -> "Review renewal".equals(b.getText())).count());
    }

    //---FLAG A CONCERN---

    @Test
    void flagAConcernOnAnAcceptedOfferRaisesANoticeForTheAdmin() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        loginAs(BUSINESS);
        Parent screen = open("donor/my-listings.fxml");

        Fx.run(() -> button(screen, "Flag a concern").fire());
        all(screen, javafx.scene.control.TextArea.class).stream()
                .filter(t -> t.getPromptText() != null && t.getPromptText().startsWith("What happened"))
                .forEach(t -> Fx.run(() -> t.setText("They never came.")));
        Fx.run(() -> button(screen, "Send to admin").fire());

        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM safety_notice WHERE offer_id = ? AND kind = 'CONCERN' AND raised_by = ? "
                + "AND concern = 'They never came.' AND status = 'OPEN'", offer, accountId(BUSINESS)));
    }

    @Test
    void theFoodGroupCanFlagFromMyOffersToo() {
        Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(GROUP);
        button(open("foodgroup/my-offers.fxml"), "Flag a concern");
    }

    //---OFFERS---

    @Test
    void incomingOffersAcceptAndDeclineButtonsCallTheService() {
        int first = Fixtures.offerOnSeedRequest(BUSINESS, "OFFERED", 10);
        int second = Fixtures.offerOnSeedRequest(CASUAL, "OFFERED", 5);
        loginAs(GROUP);
        Parent screen = open("foodgroup/incoming-offers.fxml");
        assertTrue(mentions(screen, "Sunrise Bakery"));
        assertTrue(mentions(screen, "Santos Household"));

        Fx.run(() -> button(screen, "Decline").fire());   // newest first, so the first card is the later offer

        assertEquals("DECLINED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", second));
        assertEquals("OFFERED", TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", first));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM offer WHERE status = 'OFFERED'"));
    }

    @Test
    void myOffersShowsWithdrawForAnOfferedOfferAndTheRecordLinkForACompletedOne() {
        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(GROUP);
        button(open("foodgroup/my-offers.fxml"), "Withdraw");

        TestDb.exec("UPDATE offer SET status = 'COMPLETED', released_at = NOW(), received_at = NOW()");
        button(open("foodgroup/my-offers.fxml"), "View record");
    }

    @Test
    void theMakeOfferFormsOpenWithTheChosenPost() {
        loginAs(GROUP);
        Selection.selectPost(Fixtures.seedListingId());
        assertTrue(mentions(open("foodgroup/make-offer.fxml"), "Day-old pandesal and loaf bread"));

        loginAs(BUSINESS);
        Selection.selectPost(Fixtures.seedRequestId());
        assertTrue(mentions(open("donor/make-offer.fxml"), "Weekend feeding program"));
    }

    //---RECORD AND REPORT---

    @Test
    void theHandoffRecordShowsTheNumberTheDisclaimerAndOnlyTheGroupGetsTheReportButton() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", offer);
        Selection.selectOffer(offer);

        loginAs(GROUP);
        Parent asGroup = open("shared/handoff-record.fxml");
        assertTrue(mentions(asGroup, "HR-"));
        assertTrue(mentions(asGroup, "not a certificate"));
        assertTrue(all(asGroup, Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged() && "Submit report".equals(b.getText())));

        loginAs(BUSINESS);
        Parent asDonor = open("shared/handoff-record.fxml");
        assertFalse(all(asDonor, Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged() && "Submit report".equals(b.getText())));
    }

    @Test
    void theReportFormStartsWithTheEventTheOfferStated() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", offer);
        Selection.selectOffer(offer);
        loginAs(GROUP);

        Parent form = open("foodgroup/submit-report.fxml");

        assertTrue(all(form, javafx.scene.control.TextField.class).stream().anyMatch(f -> "Feeding program".equals(f.getText())));
        assertTrue(mentions(form, "Due"));
    }
}
