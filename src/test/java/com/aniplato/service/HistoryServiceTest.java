package com.aniplato.service;

import com.aniplato.service.StatsService.Outcome;
import com.aniplato.service.StatsService.Row;
import com.aniplato.service.StatsService.View;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** History (spec 3): the cards, the table of closed posts and offers, the report column and the period filter. */
class HistoryServiceTest extends DbTest {

    private final StatsService history = new StatsService();

    /** The seed listing, rescued through a completed offer received the given days ago. Returns the offer id. */
    private int rescuedListing(int daysAgo) {
        int offer = Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        TestDb.exec("UPDATE offer SET released_at = DATE_SUB(NOW(), INTERVAL ? DAY), received_at = DATE_SUB(NOW(), INTERVAL ? DAY) WHERE offer_id = ?",
                daysAgo, daysAgo, offer);
        TestDb.exec("UPDATE post SET status = 'RESCUED' WHERE kind = 'LISTING'");
        return offer;
    }

    //---THE CARDS---

    @Test
    void theCardsCountEveryCompletedHandoffTheAccountWasASideOf() {
        rescuedListing(1);
        Fixtures.offerOnSeedRequest(BUSINESS, "COMPLETED", 10);   // the donor gave 10 kg to the same food group
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE status = 'COMPLETED' AND received_at IS NULL");
        loginAs(BUSINESS);

        View view = history.getHistory(null);

        assertEquals(22.5, view.kgRescued());
        assertEquals(2, view.handoffs());
        assertEquals(1, view.partners());   // both with the same organization

        loginAs(CASUAL);
        View none = history.getHistory(null);
        assertEquals(0, none.handoffs());
        assertTrue(none.rows().isEmpty());
    }

    //---THE TABLE---

    @Test
    void aRescuedListingShowsItsFoodTheOtherPartyAndALinkToTheRecord() {
        int offer = rescuedListing(1);
        loginAs(BUSINESS);

        List<Row> rows = history.getHistory(null).rows();

        assertEquals(1, rows.size());
        Row row = rows.get(0);
        assertEquals("Day-old pandesal and loaf bread", row.food());
        assertEquals("Kapwa Community Kitchen", row.otherParty());
        assertEquals(Outcome.RESCUED, row.outcome());
        assertEquals("Rescued", row.outcomeText());
        assertEquals(offer, row.recordOfferId());
        assertNull(row.report());   // the donor has no report column
    }

    @Test
    void aMissedPickupAndACancellationAreShownWithTheirOwnWords() {
        Fixtures.offerOnSeedListing("LAPSED", 12.5);
        TestDb.exec("UPDATE post SET status = 'EXPIRED' WHERE kind = 'LISTING'");
        Fixtures.offerOnSeedRequest(BUSINESS, "CANCELLED", 10);
        TestDb.exec("UPDATE post SET status = 'CANCELLED' WHERE kind = 'REQUEST'");

        loginAs(BUSINESS);   // its listing expired by a missed pickup; its offer on the request was cancelled by the group
        assertEquals(Set.of("Expired (missed pickup)", "Cancelled by the poster"),
                history.getHistory(null).rows().stream().map(Row::outcomeText).collect(Collectors.toSet()));

        loginAs(GROUP);   // its offer lapsed; its own request was cancelled
        List<Row> group = history.getHistory(null).rows();
        assertEquals(Set.of("Expired (missed pickup)", "Cancelled"), group.stream().map(Row::outcomeText).collect(Collectors.toSet()));
        assertTrue(group.stream().allMatch(r -> r.recordOfferId() == null));
    }

    @Test
    void anExpiredPostWithNoOffersHasNoOtherParty() {
        TestDb.exec("UPDATE post SET status = 'EXPIRED' WHERE kind = 'LISTING'");
        loginAs(BUSINESS);
        List<Row> rows = history.getHistory(null).rows();
        assertEquals(1, rows.size());
        assertNull(rows.get(0).otherParty());
        assertEquals(Outcome.EXPIRED, rows.get(0).outcome());
        assertEquals(1, history.getHistory(null).count(Outcome.EXPIRED));
        assertEquals(0, history.getHistory(null).count(Outcome.RESCUED));
    }

    @Test
    void offersAreNotShownUntilTheyCloseAndWithdrawnOrDeclinedOnesNeverAre() {
        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(GROUP);
        assertTrue(history.getHistory(null).rows().isEmpty());
        TestDb.exec("UPDATE offer SET status = 'DECLINED'");
        assertTrue(history.getHistory(null).rows().isEmpty());
        TestDb.exec("UPDATE offer SET status = 'WITHDRAWN'");
        assertTrue(history.getHistory(null).rows().isEmpty());
    }

    @Test
    void aDonorsOfferOnARequestIsListedWithTheFoodItGave() {
        int offer = Fixtures.offerOnSeedRequest(BUSINESS, "COMPLETED", 10);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", offer);
        loginAs(BUSINESS);

        Row row = history.getHistory(null).rows().get(0);

        assertEquals("Rice", row.food());
        assertEquals(10.0, row.kg());
        assertEquals("Kapwa Community Kitchen", row.otherParty());
        assertEquals(offer, row.recordOfferId());
    }

    //---THE REPORT COLUMN---

    @Test
    void theFoodGroupSeesWhetherItsReportIsDueSubmittedOrLate() {
        int offer = rescuedListing(2);
        loginAs(GROUP);
        assertTrue(history.getHistory(null).rows().get(0).report().startsWith("Due "));

        TestDb.exec("UPDATE offer SET received_at = DATE_SUB(NOW(), INTERVAL 9 DAY) WHERE offer_id = ?", offer);
        assertTrue(history.getHistory(null).rows().get(0).report().startsWith("Overdue since "));

        TestDb.exec("INSERT INTO report (offer_id, event_name, event_date, people_served, description, photo_consent_at, submitted_at) "
                + "VALUES (?, 'x', CURDATE(), 5, 'x', NOW(), NOW())", offer);
        assertEquals("Submitted late", history.getHistory(null).rows().get(0).report());

        TestDb.exec("UPDATE offer SET received_at = DATE_SUB(NOW(), INTERVAL 1 DAY) WHERE offer_id = ?", offer);
        assertEquals("Submitted", history.getHistory(null).rows().get(0).report());
    }

    //---THE PERIOD---

    @Test
    void thePeriodFiltersTheTableButNotTheCards() {
        rescuedListing(1);
        int old = Fixtures.offerOnSeedRequest(BUSINESS, "COMPLETED", 10);
        TestDb.exec("UPDATE offer SET released_at = NOW(), received_at = NOW() WHERE offer_id = ?", old);
        LocalDateTime longAgo = LocalDateTime.now().minusMonths(3);
        TestDb.exec("INSERT INTO status_change (entity_type, entity_id, from_status, to_status, changed_by, changed_at) "
                + "VALUES ('OFFER', ?, 'ACCEPTED', 'COMPLETED', NULL, ?)", old, longAgo);
        loginAs(BUSINESS);

        View all = history.getHistory(null);
        View thisMonth = history.getHistory(YearMonth.now());

        assertEquals(2, all.rows().size());
        assertEquals(1, thisMonth.rows().size());
        assertEquals(all.kgRescued(), thisMonth.kgRescued());   // the cards stay all-time (B6)
        assertEquals(List.of(YearMonth.now(), YearMonth.from(longAgo)), all.months());
        assertEquals(1, history.getHistory(YearMonth.from(longAgo)).rows().size());
    }
}
