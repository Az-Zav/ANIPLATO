package com.aniplato.service;

import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.util.Session;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The figures on the summary tiles of the list screens (Browse, My Listings, My Requests, My Offers). Nothing is
 * stored: every figure is worked out from the posts and offers the screen already loaded (A9). Spec 7.3 has no
 * class for this; it is its own so PostService and OfferService stay under 300 lines.
 */
public class BoardService {

    /** Browse: how many are open, how many are in the viewer's city, the total kg, and the one due soonest. */
    public record BrowseFigures(int open, int inMyCity, int elsewhere, String myCity, double kg, Post soonest, LocalDateTime soonestAt) { }

    /** My Listings and My Requests: open posts, accepted-and-waiting posts, and the offers waiting for a decision. */
    public record PostFigures(int open, double openKg, int awaitingPickup, double awaitingKg, int offersToReview, int postsWithOffers) { }

    /** My Offers: accepted offers, the ones the other side already released, and the kg rescued so far. */
    public record OfferFigures(int accepted, double acceptedKg, int released, double releasedKg, double kgRescued) { }

    /** The time a post is due: the use-by of a listing, the needed-by of a request. */
    public static LocalDateTime dueAt(Post post) {
        return post.isListing() ? post.food.useBy : post.use.dueAt;
    }

    public BrowseFigures browse(List<Post> board) {
        String city = Session.require().registration.city.name;
        int inCity = 0;
        double kg = 0;
        Post soonest = null;
        for (Post post : board) {
            if (post.city.equals(city)) inCity++;
            kg += post.kg;
            if (soonest == null || dueAt(post).isBefore(dueAt(soonest))) soonest = post;
        }
        return new BrowseFigures(board.size(), inCity, board.size() - inCity, city, kg, soonest, soonest == null ? null : dueAt(soonest));
    }

    public PostFigures posts(List<Post> mine) {
        int open = 0;
        int awaiting = 0;
        int toReview = 0;
        int withOffers = 0;
        double openKg = 0;
        double awaitingKg = 0;
        for (Post post : mine) {
            if (post.status == Post.Status.OPEN) {
                open++;
                openKg += post.kg;
                toReview += post.offerCount;
                if (post.offerCount > 0) withOffers++;
            } else if (post.status == Post.Status.ACCEPTED) {
                awaiting++;
                awaitingKg += post.acceptedOffer == null ? 0 : post.acceptedOffer.kg;
            }
        }
        return new PostFigures(open, openKg, awaiting, awaitingKg, toReview, withOffers);
    }

    public OfferFigures offers(List<Offer> mine) {
        int accepted = 0;
        int released = 0;
        double acceptedKg = 0;
        double releasedKg = 0;
        double rescued = 0;
        for (Offer offer : mine) {
            if (offer.status == Offer.Status.ACCEPTED) {
                accepted++;
                acceptedKg += offer.kg;
                if (offer.releasedAt != null) {
                    released++;
                    releasedKg += offer.kg;
                }
            } else if (offer.status == Offer.Status.COMPLETED) {
                rescued += offer.kg;
            }
        }
        return new OfferFigures(accepted, acceptedKg, released, releasedKg, rescued);
    }
}
