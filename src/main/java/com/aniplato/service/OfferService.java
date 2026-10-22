package com.aniplato.service;

import com.aniplato.dao.DetailDao;
import com.aniplato.dao.LogDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.dao.PhotoDao;
import com.aniplato.dao.PostDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.FoodDetail;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.model.UseDetail;
import com.aniplato.util.Database;
import com.aniplato.util.Session;

import java.io.File;
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

/** Offers on both routes (stage 3): send and withdraw, and the offerer's own list. Accept, handoff and contacts come in later steps. */
public class OfferService {

    private static final String OFFER_ENTITY = "OFFER";

    private final OfferDao offerDao = new OfferDao();
    private final PostDao postDao = new PostDao();
    private final DetailDao detailDao = new DetailDao();
    private final PhotoDao photoDao = new PhotoDao();
    private final LogDao logDao = new LogDao();
    private final FileStorageService fileStorageService = new FileStorageService();

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
        if (me.role == Account.Role.CASUAL_DONOR && !f.category().allowedForCasual()) {
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

    //---HELPERS---

    /** Saves the photos, then the offer, its detail and photos in one transaction; removes the files if it fails (M4). */
    private int submit(Account me, Post post, double kg, FoodDetail food, UseDetail use, List<File> photos, LocalDateTime now) {
        List<String> paths = fileStorageService.saveAllPhotos(photos);
        List<String> replaced = new ArrayList<>();
        try {
            int id = Database.inTransaction("Could not send the offer", c -> {
                if (postDao.findOpenForUpdate(c, post.id) == null) {
                    throw new RuleViolationException("This " + noun(post) + " is no longer open.");
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
                            ? "Your offer on this " + noun(post) + " was declined, so you cannot offer again."
                            : "You have already offered on this " + noun(post) + ".");
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
        if (post.status != Post.Status.OPEN) throw new RuleViolationException("This " + noun(post) + " is no longer open.");
        return post;
    }

    private static Account requireOfferer(String wrongRoleMessage, Account.Role... allowed) {
        Account me = Session.require();
        if (!List.of(allowed).contains(me.role)) throw new RuleViolationException(wrongRoleMessage);
        if (!me.canPostOrOffer()) throw new RuleViolationException("A suspended account cannot send offers.");
        return me;
    }

    private static String noun(Post post) {
        return post.isListing() ? "listing" : "request";
    }
}
