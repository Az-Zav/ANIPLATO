package com.aniplato.service;

import com.aniplato.service.DashboardService.BannerKind;
import com.aniplato.service.DashboardService.DonorDashboard;
import com.aniplato.service.DashboardService.FoodGroupDashboard;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two dashboards (spec 3, Figma V2): every card is worked out from the data, and the banners say what needs attention. */
class DashboardServiceTest extends DbTest {

    private final DashboardService dashboards = new DashboardService();

    //---DONOR---

    @Test
    void theDonorSeesItsListingsOffersToReviewAndTheKgFoodGroupsNeed() {
        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(BUSINESS);

        DonorDashboard d = dashboards.getDonorDashboard();

        assertEquals("Sunrise Bakery", d.name());
        assertEquals("Quezon City", d.city());
        assertEquals(1, d.openListings());
        assertEquals(1, d.offersToReview());
        assertEquals(0, d.awaitingRelease());
        assertEquals(0.0, d.kgRescued());
        assertEquals(30.0, d.kgNeeded());
        assertEquals(1, d.openRequests());
        assertEquals(1, d.activeListings().size());
        assertTrue(d.banners().isEmpty());
    }

    @Test
    void anAcceptedOfferWaitsForReleaseAndACompletedOneCountsAsRescued() {
        int accepted = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        loginAs(BUSINESS);
        DonorDashboard before = dashboards.getDonorDashboard();
        assertEquals(1, before.awaitingRelease());
        assertEquals(0, before.openListings());
        assertEquals(1, before.activeListings().size());   // an accepted listing is still active

        TestDb.exec("UPDATE offer SET status = 'COMPLETED', released_at = NOW(), received_at = NOW() WHERE offer_id = ?", accepted);
        TestDb.exec("UPDATE post SET status = 'RESCUED' WHERE kind = 'LISTING'");
        DonorDashboard after = dashboards.getDonorDashboard();
        assertEquals(0, after.awaitingRelease());
        assertEquals(12.5, after.kgRescued());
        assertTrue(after.activeListings().isEmpty());
    }

    @Test
    void aDonorOfferOnARequestAlsoWaitsForRelease() {
        Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        loginAs(BUSINESS);
        assertEquals(1, dashboards.getDonorDashboard().awaitingRelease());
    }

    @Test
    void aCasualDonorCountsOnlyTheRequestsItsBoardShows() {
        loginAs(CASUAL);
        assertEquals(1, dashboards.getDonorDashboard().openRequests());
        TestDb.exec("UPDATE use_detail SET accepted_categories = 'COOKED,BAKED'");
        DonorDashboard d = dashboards.getDonorDashboard();
        assertEquals(0, d.openRequests());
        assertEquals(0.0, d.kgNeeded());
    }

    @Test
    void aSuspendedDonorAndAnExpiringDocumentGetBanners() {
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'Unsafe food reported' WHERE email = ?", BUSINESS);
        TestDb.exec("UPDATE document SET expires_on = DATE_ADD(CURDATE(), INTERVAL 10 DAY) WHERE doc_type = 'mayors_permit'");
        loginAs(BUSINESS);

        var banners = dashboards.getDonorDashboard().banners();

        assertEquals(BannerKind.SUSPENDED, banners.get(0).kind());
        assertTrue(banners.get(0).text().contains("Unsafe food reported"));
        assertEquals(BannerKind.DOCUMENTS, banners.get(1).kind());
        assertTrue(banners.get(1).text().contains("expires within 30 days"));
    }

    @Test
    void anExpiredDocumentIsSaidToHaveExpired() {
        TestDb.exec("UPDATE document SET expires_on = DATE_SUB(CURDATE(), INTERVAL 2 DAY) WHERE doc_type = 'mayors_permit'");
        loginAs(BUSINESS);
        assertTrue(dashboards.getDonorDashboard().banners().get(0).text().contains("has expired"));
    }

    //---FOOD GROUP---

    @Test
    void theFoodGroupSeesTheOpenListingsAndItsAcceptedHandoffs() {
        Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(GROUP);

        FoodGroupDashboard d = dashboards.getFoodGroupDashboard();

        assertEquals("Kapwa Community Kitchen", d.name());
        assertEquals(1, d.openListings());
        assertEquals(12.5, d.kgWaiting());
        assertEquals(1, d.inMyCity());
        assertEquals(1, d.acceptedOffers());
        assertEquals(0.0, d.kgRescued());
        assertEquals(1, d.nearListings().size());
        assertTrue(d.banners().isEmpty());
    }

    @Test
    void aSuspendedDonorsListingIsNotCounted() {
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", BUSINESS);
        loginAs(GROUP);
        assertEquals(0, dashboards.getFoodGroupDashboard().openListings());
    }

    @Test
    void anUnreportedHandoffGetsAReportBannerThatNamesTheDueDate() {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = DATE_SUB(NOW(), INTERVAL 1 DAY), received_at = DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE offer_id = ?", offer);
        loginAs(GROUP);

        var banner = dashboards.getFoodGroupDashboard().banners().get(0);

        assertEquals(BannerKind.REPORT_DUE, banner.kind());
        assertEquals(offer, banner.offerId());
        assertTrue(banner.text().startsWith("1 report due by "));
        assertTrue(banner.text().endsWith("Day-old pandesal and loaf bread."));

        TestDb.exec("UPDATE offer SET received_at = DATE_SUB(NOW(), INTERVAL 9 DAY) WHERE offer_id = ?", offer);
        assertTrue(dashboards.getFoodGroupDashboard().banners().get(0).text().contains("was due"));

        TestDb.exec("INSERT INTO report (offer_id, event_name, event_date, people_served, description, photo_consent_at, submitted_at) "
                + "VALUES (?, 'x', CURDATE(), 5, 'x', NOW(), NOW())", offer);
        assertTrue(dashboards.getFoodGroupDashboard().banners().isEmpty());
    }

    //---ROLES---

    @Test
    void eachDashboardIsForItsOwnRole() {
        loginAs(GROUP);
        assertRule("Only a donor has this dashboard.", dashboards::getDonorDashboard);
        loginAs(BUSINESS);
        assertRule("Only a food group has this dashboard.", dashboards::getFoodGroupDashboard);
    }
}
