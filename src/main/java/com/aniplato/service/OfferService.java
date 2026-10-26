package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.DetailDao;
import com.aniplato.dao.LogDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.PhotoDao;
import com.aniplato.dao.PostDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.FoodDetail;
import com.aniplato.model.HandoffRecord;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.model.UseDetail;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.aniplato.service.DetailRules.blankToNull;
import static com.aniplato.service.DetailRules.foodDetail;
import static com.aniplato.service.DetailRules.requireAttestation;
import static com.aniplato.service.DetailRules.requireKg;
import static com.aniplato.service.DetailRules.requireText;

/**
 * Offers on both routes (spec 7.3): send and withdraw (stage 3); the poster's list, accept, decline and the contact
 * numbers (stage 4); release, receipt and the handoff record (stage 5); and the two automatic checks that act on offers.
 */
public class OfferService {

    private static final String OFFER_ENTITY = "OFFER";

    private final OfferDao offerDao = new OfferDao();
    private final PostDao postDao = new PostDao();
    private final DetailDao detailDao = new DetailDao();
    private final PhotoDao photoDao = new PhotoDao();
    private final AccountDao accountDao = new AccountDao();
    private final LogDao logDao = new LogDao();
    private final FileStorageService fileStorageService = new FileStorageService();
    private final PostService postService = new PostService();

    /** What a food group enters to offer on a listing (3A). The kg is the listing's whole quantity. */
    public record ListingOfferForm(int postId, String useDescription, LocalDate eventDate, String beneficiaries,
                                   int headcount, String place, LocalDateTime pickupAt, String message,
                                   boolean attested, List<File> photos) { }

    /** What a donor enters to offer on a request (3B). */
    public record RequestOfferForm(int postId, String description, FoodDetail.Category category,
                                   FoodDetail.Storage storage, Set<FoodDetail.Allergen> allergens, double kg,
                                   LocalDateTime useBy, LocalDateTime pickupStart, LocalDateTime pickupEnd,
                                   String handoverInstructions, boolean attested, List<File> photos) { }

    //---SEND---

    /** A food group offers on a listing (R3.1). One transaction; a Withdrawn offer on the same listing is re-opened (M2). Returns the offer id. */
    public int sendListingOffer(ListingOfferForm f) {
        Account me = requireOfferer("Only a food group can offer on a listing.", Account.Role.FOOD_GROUP);
        Post post = requirePost(f.postId(), Post.Kind.LISTING);
        FoodDetail listed = post.food;
        requireText(f.useDescription(), "Say how the food will be used.", 300);
        requireText(f.beneficiaries(), "Say who benefits.", 200);
        requireText(f.place(), "Say where the food will go.", 200);
        if (f.headcount() <= 0) throw new RuleViolationException("How many people it feeds must be more than 0.");
        if (f.message() != null && f.message().length() > 500) throw new RuleViolationException("Keep the message to 500 characters or fewer.");
        if (f.pickupAt() == null || f.pickupAt().isBefore(listed.pickupStart) || f.pickupAt().isAfter(listed.pickupEnd)
                || !f.pickupAt().isBefore(listed.useBy)) {
            throw new RuleViolationException("Pick a pickup time inside the listing's pickup window and before its use-by.");
        }
        requireAttestation(f.attested());
        LocalDateTime now = LocalDateTime.now();
        UseDetail use = new UseDetail();
        use.useDescription = f.useDescription().trim();
        use.eventDate = f.eventDate();
        use.beneficiaries = f.beneficiaries().trim();
        use.headcount = f.headcount();
        use.place = f.place().trim();
        use.dueAt = f.pickupAt();
        use.message = blankToNull(f.message());
        use.attestedAt = now;
        return submit(me, post, post.kg, null, use, f.photos(), now);
    }

    /** A donor offers on a request (R3.2). One transaction; a Withdrawn offer on the same request is re-opened (M2). Returns the offer id. */
    public int sendRequestOffer(RequestOfferForm f) {
        Account me = requireOfferer("Only a donor can offer on a request.", Account.Role.BUSINESS_DONOR, Account.Role.CASUAL_DONOR);
        Post post = requirePost(f.postId(), Post.Kind.REQUEST);
        requireText(f.description(), "Describe the food.", 200);
        if (f.category() == null) throw new RuleViolationException("Pick a category.");
        if (!me.mayGive(f.category())) {
            throw new RuleViolationException("Casual donors can give raw or sealed-packaged food only.");
        }
        if (!post.use.acceptedCategories.contains(f.category())) {
            throw new RuleViolationException("This request does not accept " + f.category().label().toLowerCase() + " food.");
        }
        if (f.storage() == null) throw new RuleViolationException("Pick a storage condition.");
        requireKg(f.kg());
        if (f.kg() > post.kg) throw new RuleViolationException("You cannot offer more than the " + post.kg + " kg requested.");
        LocalDateTime now = LocalDateTime.now();
        FoodDetail food = foodDetail(f.description(), f.category(), f.storage(), f.allergens(), f.useBy(),
                f.pickupStart(), f.pickupEnd(), f.handoverInstructions(), f.attested(), now);
        if (!f.pickupEnd().isBefore(post.use.dueAt)) {
            throw new RuleViolationException("The pickup window must end before the request's needed-by time.");
        }
        return submit(me, post, f.kg(), food, null, f.photos(), now);
    }

    //---WITHDRAW---

    /** The offerer takes back an Offered offer (R3.3). Only the offerer, only while it is still Offered. */
    public void withdraw(int offerId) {
        Account me = Session.require();
        Database.inTransaction("Could not withdraw the offer", c -> {
            Offer offer = offerDao.findById(c, offerId);
            if (offer == null) throw new RuleViolationException("That offer no longer exists.");
            if (offer.offererId != me.id) throw new RuleViolationException("You can only withdraw your own offer.");
            if (offer.status == Offer.Status.ACCEPTED) {
                throw new RuleViolationException("Once accepted, an offer cannot be withdrawn. Only the poster can cancel.");
            }
            if (offerDao.updateStatusIf(c, offerId, Offer.Status.OFFERED, Offer.Status.WITHDRAWN) == 0) {
                throw new RuleViolationException("This offer can no longer be withdrawn.");
            }
            logDao.logStatusChange(c, OFFER_ENTITY, offerId, Offer.Status.OFFERED.name(), Offer.Status.WITHDRAWN.name(), null, me.id);   // A3
            return null;
        });
    }

    //---THE OFFERER'S LIST---

    /**
     * The logged-in account's own offers, newest first, each with the post it is on and its photos. The listing's
     * handover instructions are shown only once the offer is accepted (R2.1).
     */
    public List<Offer> getMyOffers() {
        Account me = Session.require();
        return Database.inTransaction("Could not load your offers", c -> {
            List<Offer> offers = offerDao.findByOfferer(c, me.id);
            for (Offer offer : offers) {
                offer.photos = photoDao.findPaths(c, PhotoDao.Owner.OFFER, offer.id);
                offer.post = postDao.findById(c, offer.postId);
                offer.post.photos = photoDao.findPaths(c, PhotoDao.Owner.POST, offer.postId);
                boolean accepted = offer.status == Offer.Status.ACCEPTED || offer.status == Offer.Status.COMPLETED;
                if (offer.post.food != null && !accepted) offer.post.food.handoverInstructions = null;
            }
            return offers;
        });
    }

    //---AUTOMATIC CHECK (6.6, step 4)---

    /** An Offered offer nobody answered by its pickup time (Route 1) or window end (Route 2) becomes Expired. Logged by the system (A3). */
    public void expireUnansweredOffers(LocalDateTime now) {
        Database.inTransaction("Could not expire unanswered offers", c -> {
            for (int offerId : offerDao.findUnansweredPastPickup(c, now)) {
                if (offerDao.updateStatusIf(c, offerId, Offer.Status.OFFERED, Offer.Status.EXPIRED) > 0) {
                    logDao.logStatusChange(c, OFFER_ENTITY, offerId, Offer.Status.OFFERED.name(), Offer.Status.EXPIRED.name(),
                            "Pickup time passed", null);
                }
            }
            return null;
        });
    }

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
            if (post.status != Post.Status.OPEN) throw new RuleViolationException("This " + post.noun() + " is no longer open.");
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

    //---AUTOMATIC CHECK (6.6, step 1)---

    /**
     * Lapse: an Accepted offer with no release 2 hours after its pickup time (Route 1) or window end (Route 2) becomes
     * Lapsed and its post Expired. Once the donor confirmed release the offer no longer lapses (M1). Logged by the system (A3).
     */
    public void lapseMissedPickups(LocalDateTime now) {
        LocalDateTime cutoff = now.minusHours(Settings.PICKUP_GRACE_HOURS);
        Database.inTransaction("Could not lapse missed pickups", c -> {
            for (OfferDao.MissedPickup missed : offerDao.findMissedPickups(c, cutoff)) {
                if (offerDao.updateStatusIf(c, missed.offerId(), Offer.Status.ACCEPTED, Offer.Status.LAPSED) > 0) {
                    logDao.logStatusChange(c, OFFER_ENTITY, missed.offerId(), Offer.Status.ACCEPTED.name(),
                            Offer.Status.LAPSED.name(), "Pickup missed", null);
                    postService.changeStatus(c, missed.postId(), Post.Status.ACCEPTED, Post.Status.EXPIRED, "Pickup missed", null);
                }
            }
            return null;
        });
    }

    //---HELPERS---

    /** Saves the photos, then the offer, its detail and photos in one transaction; removes the files if it fails (M4). */
    private int submit(Account me, Post post, double kg, FoodDetail food, UseDetail use, List<File> photos, LocalDateTime now) {
        List<String> paths = fileStorageService.saveAllPhotos(photos);
        List<String> replaced = new ArrayList<>();
        try {
            int id = Database.inTransaction("Could not send the offer", c -> {
                if (postDao.findOpenForUpdate(c, post.id) == null) {
                    throw new RuleViolationException("This " + post.noun() + " is no longer open.");
                }
                Integer foodId = food == null ? null : detailDao.insertFood(c, food);
                Integer useId = use == null ? null : detailDao.insertUse(c, use);
                Offer existing = offerDao.findByPostAndOfferer(c, post.id, me.id);
                int offerId;
                if (existing == null) {
                    offerId = offerDao.insert(c, post.id, me.id, kg, foodId, useId, now);
                    logDao.logStatusChange(c, OFFER_ENTITY, offerId, null, Offer.Status.OFFERED.name(), null, me.id);   // A3
                } else if (existing.status == Offer.Status.WITHDRAWN) {
                    offerId = existing.id;
                    replaced.addAll(photoDao.findPaths(c, PhotoDao.Owner.OFFER, offerId));
                    offerDao.reopenIfWithdrawn(c, offerId, kg, foodId, useId, now);
                    photoDao.deleteFor(c, PhotoDao.Owner.OFFER, offerId);
                    if (existing.food != null) detailDao.deleteFood(c, existing.food.id);
                    if (existing.use != null) detailDao.deleteUse(c, existing.use.id);
                    logDao.logStatusChange(c, OFFER_ENTITY, offerId, Offer.Status.WITHDRAWN.name(), Offer.Status.OFFERED.name(), null, me.id);
                } else {
                    throw new RuleViolationException(existing.status == Offer.Status.DECLINED
                            ? "Your offer on this " + post.noun() + " was declined, so you cannot offer again."
                            : "You have already offered on this " + post.noun() + ".");
                }
                for (String path : paths) photoDao.insert(c, PhotoDao.Owner.OFFER, offerId, path);
                return offerId;
            });
            fileStorageService.deleteAll(replaced);   // the re-opened offer no longer uses its old photos
            return id;
        } catch (RuntimeException e) {
            fileStorageService.deleteAll(paths);
            throw e;
        }
    }

    /** The post an offer is meant for, read for validation; it must exist, be the right kind, be Open and have an Active poster. */
    private Post requirePost(int postId, Post.Kind kind) {
        Post post = Database.inTransaction("Could not load the post", c -> postDao.findById(c, postId));
        if (post == null || post.kind != kind) throw new RuleViolationException("That " + kind.name().toLowerCase() + " no longer exists.");
        if (post.status != Post.Status.OPEN) throw new RuleViolationException("This " + post.noun() + " is no longer open.");
        return post;
    }

    private static Account requireOfferer(String wrongRoleMessage, Account.Role... allowed) {
        Account me = Session.require();
        if (!List.of(allowed).contains(me.role)) throw new RuleViolationException(wrongRoleMessage);
        if (!me.canPostOrOffer()) throw new RuleViolationException("A suspended account cannot send offers.");
        return me;
    }


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
