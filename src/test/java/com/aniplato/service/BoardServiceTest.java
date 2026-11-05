package com.aniplato.service;

import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.BoardService.BrowseFigures;
import com.aniplato.service.BoardService.OfferFigures;
import com.aniplato.service.BoardService.PostFigures;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The figures on the tiles of the list screens: worked out from what the screen loaded, never stored (A9). */
class BoardServiceTest extends DbTest {

    private final BoardService figures = new BoardService();
    private final PostService posts = new PostService();
    private final OfferService offers = new OfferService();

    @Test
    void browseCountsTheOpenPostsTheViewersCityAndTheOneDueSoonest() {
        loginAs(GROUP);
        List<Post> board = posts.getOpenListings();

        BrowseFigures f = figures.browse(board);

        assertEquals(board.size(), f.open());
        assertEquals(board.size(), f.inMyCity() + f.elsewhere());
        assertEquals("Quezon City", f.myCity());
        assertEquals(board.stream().mapToDouble(p -> p.kg).sum(), f.kg(), 0.001);
        assertNotNull(f.soonest());
        assertEquals(BoardService.dueAt(f.soonest()), f.soonestAt());
    }

    @Test
    void anEmptyBoardHasNothingDueSoonest() {
        loginAs(GROUP);
        BrowseFigures f = figures.browse(List.of());
        assertEquals(0, f.open());
        assertNull(f.soonest());
        assertNull(f.soonestAt());
    }

    @Test
    void myPostsCountOpenPostsAndTheOffersWaitingOnThem() {
        loginAs(BUSINESS);
        PostFigures before = figures.posts(posts.getMyPosts());
        assertEquals(0, before.offersToReview());

        Fixtures.offerOnSeedListing("OFFERED", 12.5);
        PostFigures after = figures.posts(posts.getMyPosts());

        assertEquals(before.open(), after.open());
        assertEquals(1, after.offersToReview());
        assertEquals(1, after.postsWithOffers());
    }

    @Test
    void anAcceptedPostIsCountedAsAwaitingPickupWithTheAcceptedKg() {
        Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        TestDb.exec("UPDATE post SET status = 'ACCEPTED' WHERE kind = 'LISTING'");
        loginAs(BUSINESS);

        PostFigures f = figures.posts(posts.getMyPosts());

        assertEquals(1, f.awaitingPickup());
        assertEquals(12.5, f.awaitingKg(), 0.001);
    }

    @Test
    void myOffersCountAcceptedOnesAndTheOnesTheOtherSideAlreadyReleased() {
        int offer = Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(GROUP);
        OfferFigures waiting = figures.offers(offers.getMyOffers());
        assertEquals(1, waiting.accepted());
        assertEquals(0, waiting.released());

        TestDb.exec("UPDATE offer SET released_at = NOW() WHERE offer_id = ?", offer);
        OfferFigures released = figures.offers(offers.getMyOffers());

        assertEquals(1, released.released());
        assertEquals(12.5, released.releasedKg(), 0.001);
        assertEquals(12.5, released.acceptedKg(), 0.001);
    }

    @Test
    void completedOffersAddUpAsKgRescued() {
        Fixtures.offerOnSeedListing("COMPLETED", 12.5);
        loginAs(GROUP);
        List<Offer> mine = offers.getMyOffers();
        assertEquals(12.5, figures.offers(mine).kgRescued(), 0.001);
    }
}
