package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.LogDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.PhotoDao;
import com.aniplato.dao.PostDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.util.Database;
import com.aniplato.util.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stage 4: the poster looks at the offers on their posts, accepts one or declines one, and the two sides get each
 * other's contact number. (Spec 7.3 lists this under OfferService; it is its own class to keep each file short.)
 */
public class AcceptanceService {

    private static final String OFFER_ENTITY = "OFFER";

    private final OfferDao offerDao = new OfferDao();
    private final PostDao postDao = new PostDao();
    private final PhotoDao photoDao = new PhotoDao();
    private final AccountDao accountDao = new AccountDao();
    private final LogDao logDao = new LogDao();
    private final PostService postService = new PostService();

    //---THE POSTER'S LIST---

    /**
     * Offers on the logged-in account's posts, newest first, each with its post and photos. Withdrawn offers and
     * Offered offers from a Suspended offerer are left out. A donor's handover text on an offer to a request
     * stays hidden until the offer is accepted (R2.1).
     */
    public List<Offer> getIncomingOffers() {
        Account me = Session.require();
        return Database.inTransaction("Could not load the offers", c -> {
            List<Offer> offers = offerDao.findIncoming(c, me.id);
            for (Offer offer : offers) {
                offer.photos = photoDao.findPaths(c, PhotoDao.Owner.OFFER, offer.id);
                offer.post = postDao.findById(c, offer.postId);
                offer.post.photos = photoDao.findPaths(c, PhotoDao.Owner.POST, offer.postId);
                boolean accepted = offer.status == Offer.Status.ACCEPTED || offer.status == Offer.Status.COMPLETED;
                if (offer.food != null && !accepted) offer.food.handoverInstructions = null;
            }
            return offers;
        });
    }

    /** How many Offered offers are waiting for the logged-in poster's decision: the donor's sidebar badge. */
    public int countOffersAwaitingDecision() {
        Account me = Session.require();
        return Database.inTransaction("Could not count the offers", c -> offerDao.countAwaitingDecision(c, me.id));
    }

    //---ACCEPT AND DECLINE---

    /**
     * The poster accepts one Offered offer (R4.1). In one transaction: that offer is Accepted, every other Offered
     * offer on the post is Declined, and the post is Accepted. If any part fails, nothing changes (A2).
     */
    public void accept(int offerId) {
        Account me = Session.require();
        if (!me.canPostOrOffer()) throw new RuleViolationException("A suspended account cannot accept offers.");
        Database.inTransaction("Could not accept the offer", c -> {
            Offer offer = requireOwnOfferOnPost(c, offerId, me);
            Post post = postDao.findForUpdate(c, offer.postId);
            if (post.status != Post.Status.OPEN) throw new RuleViolationException("This " + noun(post) + " is no longer open.");
            offer = offerDao.findById(c, offerId);   // read again now that the post is locked
            if (offer.status != Offer.Status.OFFERED) throw new RuleViolationException("Only an Offered offer can be accepted.");
            if (offer.offererSuspended) throw new RuleViolationException("This offer is not available right now.");
            if (!offer.pickupDeadline().isAfter(LocalDateTime.now())) throw new RuleViolationException("This offer has expired.");
            changeOfferStatus(c, offerId, Offer.Status.OFFERED, Offer.Status.ACCEPTED, me.id);
            for (int other : offerDao.findOfferedIdsExcept(c, post.id, offerId)) {
                changeOfferStatus(c, other, Offer.Status.OFFERED, Offer.Status.DECLINED, me.id);
            }
            postService.changeStatus(c, post.id, Post.Status.OPEN, Post.Status.ACCEPTED, null, me.id);
            return null;
        });
    }

    /** The poster declines one Offered offer (R4.2). The offerer sees Declined and cannot offer on this post again. */
    public void decline(int offerId) {
        Account me = Session.require();
        Database.inTransaction("Could not decline the offer", c -> {
            requireOwnOfferOnPost(c, offerId, me);
            if (offerDao.updateStatusIf(c, offerId, Offer.Status.OFFERED, Offer.Status.DECLINED) == 0) {
                throw new RuleViolationException("Only an Offered offer can be declined.");
            }
            logDao.logStatusChange(c, OFFER_ENTITY, offerId, Offer.Status.OFFERED.name(), Offer.Status.DECLINED.name(), null, me.id);   // A3
            return null;
        });
    }

    //---CONTACT NUMBERS (A7)---

    /**
     * The one place a phone number leaves the database (A7). The owner and an admin may read any number they ask
     * for; otherwise only the other side of an Accepted offer, until the handoff closes (R4.3).
     */
    public String getContactPhone(int accountId, int offerId) {
        Account me = Session.require();
        return Database.inTransaction("Could not load the contact number", c -> {
            boolean allowed = me.id == accountId || me.role == Account.Role.ADMIN;
            if (!allowed) {
                OfferDao.Parties parties = offerDao.findParties(c, offerId);
                boolean otherSide = parties != null
                        && ((me.id == parties.offererId() && accountId == parties.posterId())
                        || (me.id == parties.posterId() && accountId == parties.offererId()));
                allowed = otherSide && offerDao.findStatus(c, offerId) == Offer.Status.ACCEPTED;
            }
            if (!allowed) throw new RuleViolationException("You can see this contact number only on an accepted offer.");
            return accountDao.findContactPhone(c, accountId);
        });
    }

    //---HELPERS---

    /** The offer, if it exists and is on a post the account made. */
    private Offer requireOwnOfferOnPost(Connection c, int offerId, Account me) throws SQLException {
        Offer offer = offerDao.findById(c, offerId);
        if (offer == null) throw new RuleViolationException("That offer no longer exists.");
        Post post = postDao.findById(c, offer.postId);
        if (post.posterId != me.id) throw new RuleViolationException("Only the poster can decide on this offer.");
        return offer;
    }

    private void changeOfferStatus(Connection c, int offerId, Offer.Status from, Offer.Status to, int changedBy)
            throws SQLException {
        if (offerDao.updateStatusIf(c, offerId, from, to) == 0) throw new RuleViolationException("This offer has already changed.");
        logDao.logStatusChange(c, OFFER_ENTITY, offerId, from.name(), to.name(), null, changedBy);   // A3
    }

    private static String noun(Post post) {
        return post.isListing() ? "listing" : "request";
    }
}
