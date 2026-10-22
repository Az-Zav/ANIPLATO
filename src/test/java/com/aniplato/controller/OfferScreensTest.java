package com.aniplato.controller;

import com.aniplato.support.Fixtures;
import com.aniplato.support.Fx;
import com.aniplato.support.TestDb;
import com.aniplato.util.Selection;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The offer screens, the handoff record and the report form, loaded for real: they open and the buttons call the services. */
class OfferScreensTest extends ScreenTestBase {

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
