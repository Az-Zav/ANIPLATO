package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Stage 6: the poster cancels an Open or Accepted post with a reason, and its offers are settled (R6.1, M3). */
class CancelPostTest extends DbTest {

    private final PostService posts = new PostService();

    private String offerStatus(int id) {
        return TestDb.scalar("SELECT status FROM offer WHERE offer_id = ?", id);
    }

    @Test
    void cancellingAnOpenPostDeclinesItsWaitingOffersAndKeepsTheReason() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        int donorId = loginAs(BUSINESS).id;
        int listing = Fixtures.seedListingId();

        posts.cancelPost(listing, "  Oven broke down  ");

        assertEquals("CANCELLED", TestDb.scalar("SELECT status FROM post WHERE post_id = ?", listing));
        assertEquals("Oven broke down", TestDb.scalar("SELECT cancel_reason FROM post WHERE post_id = ?", listing));
        assertEquals("DECLINED", offerStatus(offer));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'POST' AND entity_id = ? "
                + "AND from_status = 'OPEN' AND to_status = 'CANCELLED' AND reason = 'Oven broke down' AND changed_by = ?", listing, donorId));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'OFFER' AND entity_id = ? "
                + "AND from_status = 'OFFERED' AND to_status = 'DECLINED'", offer));
    }

    @Test
    void cancellingAnAcceptedPostCancelsTheAcceptedOfferAndDeclinesTheRest() {
        int accepted = Fixtures.offerOnSeedRequest(BUSINESS, "ACCEPTED", 10);
        int other = Fixtures.offerOnSeedRequest(CASUAL, "OFFERED", 5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'REQUEST'");
        loginAs(GROUP);

        posts.cancelPost(Fixtures.seedRequestId(), "The event was called off");

        assertEquals("CANCELLED", offerStatus(accepted));
        assertEquals("DECLINED", offerStatus(other));
        assertEquals("CANCELLED", TestDb.scalar("SELECT status FROM post WHERE kind = 'REQUEST'"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'POST' AND from_status = 'ACCEPTED' "
                + "AND to_status = 'CANCELLED'"));
    }

    @Test
    void cancellingIsBlockedOnceTheFoodWasReleased() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        TestDb.exec("UPDATE offer SET released_at = NOW() WHERE offer_id = ?", offer);
        loginAs(BUSINESS);

        assertRule("The food has already been released, so this post can no longer be cancelled.",
                () -> posts.cancelPost(Fixtures.seedListingId(), "Changed my mind"));
        assertEquals("ACCEPTED", offerStatus(offer));
        assertEquals("ACCEPTED", TestDb.scalar("SELECT status FROM post WHERE kind = 'LISTING'"));
    }

    @Test
    void onlyThePosterCancelsWithAReasonAndOnlyOpenOrAcceptedPosts() {
        int listing = Fixtures.seedListingId();
        loginAs(GROUP);
        assertRule("Only the poster can cancel this post.", () -> posts.cancelPost(listing, "x"));
        loginAs(BUSINESS);
        assertRule("Write a reason for cancelling.", () -> posts.cancelPost(listing, "  "));
        assertRule("Keep it to 500 characters or fewer.", () -> posts.cancelPost(listing, "y".repeat(501)));
        assertRule("That post no longer exists.", () -> posts.cancelPost(99999, "x"));

        TestDb.exec("UPDATE post SET status = 'RESCUED' WHERE post_id = ?", listing);
        assertRule("Only an Open or Accepted post can be cancelled.", () -> posts.cancelPost(listing, "x"));
        TestDb.exec("UPDATE post SET status = 'EXPIRED' WHERE post_id = ?", listing);
        assertRule("Only an Open or Accepted post can be cancelled.", () -> posts.cancelPost(listing, "x"));
    }
}
