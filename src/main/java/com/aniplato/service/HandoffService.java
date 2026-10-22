package com.aniplato.service;

import com.aniplato.dao.LogDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.PostDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.HandoffRecord;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Stage 5: the donor confirms release, then the food group confirms receipt, and the handoff record is built. The
 * donor is the supplier on both routes (the poster of a listing, the offerer on a request); the food group is the receiver.
 */
public class HandoffService {

    private static final String OFFER_ENTITY = "OFFER";

    private final OfferDao offerDao = new OfferDao();
    private final PostDao postDao = new PostDao();
    private final LogDao logDao = new LogDao();
    private final PostService postService = new PostService();

    //---RELEASE AND RECEIPT---

    /** At pickup the donor confirms the food was handed over (R5.1). Only on an Accepted offer, once. */
    public void confirmRelease(int offerId) {
        Account me = Session.require();
        Database.inTransaction("Could not confirm the release", c -> {
            Offer offer = requireOffer(c, offerId);
            Post post = postDao.findById(c, offer.postId);
            if (me.id != supplierId(offer, post)) throw new RuleViolationException("Only the donor can confirm release.");
            if (offer.status != Offer.Status.ACCEPTED) throw new RuleViolationException("Only an Accepted offer can be released.");
            if (offerDao.markReleasedIf(c, offerId, LocalDateTime.now()) == 0) {
                throw new RuleViolationException("Release was already confirmed.");
            }
            return null;   // no status changes yet, so nothing to log (A3)
        });
    }

    /**
     * The food group confirms it received the food (R5.2). Only after the donor confirmed release. One transaction:
     * the offer is Completed with the receipt time, the post is Rescued, both logged (A2, A3).
     */
    public void confirmReceipt(int offerId) {
        Account me = Session.require();
        Database.inTransaction("Could not confirm the receipt", c -> {
            Offer offer = requireOffer(c, offerId);
            Post post = postDao.findForUpdate(c, offer.postId);
            if (me.id != receiverId(offer, post)) throw new RuleViolationException("Only the food group can confirm receipt.");
            if (offer.status == Offer.Status.COMPLETED) throw new RuleViolationException("This handoff is already complete.");
            if (offer.status != Offer.Status.ACCEPTED) throw new RuleViolationException("Only an Accepted offer can be received.");
            if (offerDao.completeIfReleased(c, offerId, LocalDateTime.now()) == 0) {
                throw new RuleViolationException("Receipt can be confirmed only after the donor confirms release.");
            }
            logDao.logStatusChange(c, OFFER_ENTITY, offerId, Offer.Status.ACCEPTED.name(), Offer.Status.COMPLETED.name(), null, me.id);
            postService.changeStatus(c, post.id, Post.Status.ACCEPTED, Post.Status.RESCUED, null, me.id);
            return null;
        });
    }

    //---THE RECORD---

    /** The handoff record of a Completed offer, for either side of it. Worked out, not stored (A9). */
    public HandoffRecord getHandoffRecord(int offerId) {
        Account me = Session.require();
        return Database.inTransaction("Could not load the handoff record", c -> {
            Offer offer = requireOffer(c, offerId);
            Post post = postDao.findById(c, offer.postId);
            boolean side = me.id == offer.offererId || me.id == post.posterId;
            if (!side && me.role != Account.Role.ADMIN) throw new RuleViolationException("Only the two sides of a handoff can see its record.");
            if (offer.status != Offer.Status.COMPLETED) throw new RuleViolationException("A record exists once the handoff is complete.");
            boolean listing = post.isListing();
            String food = listing ? post.food.description : offer.food.description;
            var category = listing ? post.food.category : offer.food.category;
            var use = listing ? offer.use : post.use;
            String number = "HR-" + offer.receivedAt.getYear() + "-" + String.format("%04d", offer.id);
            return new HandoffRecord(number, offer.id, listing ? post.posterName : offer.offererName,
                    listing ? offer.offererName : post.posterName, food, category, offer.kg, offer.releasedAt,
                    offer.receivedAt, use.useDescription, use.beneficiaries, use.headcount, use.place,
                    offer.receivedAt.plusDays(Settings.REPORT_DEADLINE_DAYS));
        });
    }

    /** Accepted offers waiting for pickup or receipt that the logged-in account is a side of: the food group's sidebar badge. */
    public int countAwaitingHandoff() {
        Account me = Session.require();
        return Database.inTransaction("Could not count the handoffs", c -> offerDao.countAcceptedFor(c, me.id));
    }

    //---HELPERS---

    private Offer requireOffer(Connection c, int offerId) throws SQLException {
        Offer offer = offerDao.findById(c, offerId);
        if (offer == null) throw new RuleViolationException("That offer no longer exists.");
        return offer;
    }

    /** The donor: whoever posted a listing, or whoever offered on a request. */
    private static int supplierId(Offer offer, Post post) {
        return post.isListing() ? post.posterId : offer.offererId;
    }

    /** The food group: whoever offered on a listing, or whoever posted a request. */
    private static int receiverId(Offer offer, Post post) {
        return post.isListing() ? offer.offererId : post.posterId;
    }
}
