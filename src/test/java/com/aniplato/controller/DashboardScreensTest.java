package com.aniplato.controller;

import com.aniplato.support.Fixtures;
import com.aniplato.support.Fx;
import com.aniplato.support.TestDb;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two dashboards and History, loaded for real. */
class DashboardScreensTest extends ScreenTestBase {

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
}
