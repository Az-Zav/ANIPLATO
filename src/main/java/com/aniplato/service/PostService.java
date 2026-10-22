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
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static com.aniplato.service.DetailRules.blankToNull;
import static com.aniplato.service.DetailRules.foodDetail;
import static com.aniplato.service.DetailRules.requireAttestation;
import static com.aniplato.service.DetailRules.requireKg;
import static com.aniplato.service.DetailRules.requireText;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Listings and requests: create and validate them (stage 2), and the boards and lists that show them. */
public class PostService {

    private static final String POST_ENTITY = "POST";

    private final PostDao postDao = new PostDao();
    private final DetailDao detailDao = new DetailDao();
    private final PhotoDao photoDao = new PhotoDao();
    private final OfferDao offerDao = new OfferDao();
    private final LogDao logDao = new LogDao();
    private final FileStorageService fileStorageService = new FileStorageService();

    /** What a donor enters for a listing (2A). */
    public record ListingForm(String description, FoodDetail.Category category, FoodDetail.Storage storage,
                              Set<FoodDetail.Allergen> allergens, double kg, LocalDateTime useBy,
                              LocalDateTime pickupStart, LocalDateTime pickupEnd, String handoverInstructions,
                              boolean attested, List<File> photos) { }

    /** What a food group enters for a request (2B). */
    public record RequestForm(String eventName, LocalDate eventDate, String beneficiaries, int headcount, String place,
                              LocalDateTime neededBy, String message, Set<FoodDetail.Category> acceptedCategories,
                              double kg, boolean attested, List<File> photos) { }

    //---CREATE---

    /** A donor posts a listing (R2.1). One transaction: food detail, post, photos, status log. Returns the new post id. */
    public int createListing(ListingForm f) {
        Account me = requirePoster("Food groups post requests, not listings.", Account.Role.BUSINESS_DONOR, Account.Role.CASUAL_DONOR);
        LocalDateTime now = LocalDateTime.now();
        requireText(f.description(), "Describe the food.", 200);
        if (f.category() == null) throw new RuleViolationException("Pick a category.");
        if (!me.mayGive(f.category())) {
            throw new RuleViolationException("Casual donors can give raw or sealed-packaged food only.");
        }
        if (f.storage() == null) throw new RuleViolationException("Pick a storage condition.");
        requireKg(f.kg());
        FoodDetail food = foodDetail(f.description(), f.category(), f.storage(), f.allergens(), f.useBy(),
                f.pickupStart(), f.pickupEnd(), f.handoverInstructions(), f.attested(), now);
        List<String> paths = fileStorageService.saveAllPhotos(f.photos());
        try {
            return Database.inTransaction("Could not save the listing", c -> {
                int foodId = detailDao.insertFood(c, food);
                int postId = postDao.insert(c, Post.Kind.LISTING, me.id, me.registration.city.id, f.kg(), foodId, null, now);
                savePhotosAndLog(c, postId, paths, me.id);
                return postId;
            });
        } catch (RuntimeException e) {
            fileStorageService.deleteAll(paths);   // the database never pointed at these files
            throw e;
        }
    }

    /** A food group posts a request (R2.2). One transaction: use detail, post, photos, status log. Returns the new post id. */
    public int createRequest(RequestForm f) {
        Account me = requirePoster("Only a food group can post a request.", Account.Role.FOOD_GROUP);
        LocalDateTime now = LocalDateTime.now();
        requireText(f.eventName(), "Name the event or program.", 300);
        requireText(f.beneficiaries(), "Say who the food is for.", 200);
        requireText(f.place(), "Say where the food should go.", 200);
        if (f.message() != null && f.message().length() > 500) throw new RuleViolationException("Keep the message to 500 characters or fewer.");
        if (f.headcount() <= 0) throw new RuleViolationException("How many people it feeds must be more than 0.");
        requireKg(f.kg());
        if (f.acceptedCategories() == null || f.acceptedCategories().isEmpty()) {
            throw new RuleViolationException("Pick at least one category you will accept.");
        }
        if (f.neededBy() == null || !f.neededBy().isAfter(now)) throw new RuleViolationException("Needed-by must be in the future.");
        if (f.eventDate() == null) throw new RuleViolationException("Enter the event date.");
        if (f.eventDate().isBefore(f.neededBy().toLocalDate())) {
            throw new RuleViolationException("The event date must be on or after the needed-by date.");
        }
        requireAttestation(f.attested());
        UseDetail use = new UseDetail();
        use.useDescription = f.eventName().trim();
        use.eventDate = f.eventDate();
        use.beneficiaries = f.beneficiaries().trim();
        use.headcount = f.headcount();
        use.place = f.place().trim();
        use.dueAt = f.neededBy();
        use.message = blankToNull(f.message());
        use.acceptedCategories = EnumSet.copyOf(f.acceptedCategories());
        use.attestedAt = now;
        List<String> paths = fileStorageService.saveAllPhotos(f.photos());
        try {
            return Database.inTransaction("Could not save the request", c -> {
                int useId = detailDao.insertUse(c, use);
                int postId = postDao.insert(c, Post.Kind.REQUEST, me.id, me.registration.city.id, f.kg(), null, useId, now);
                savePhotosAndLog(c, postId, paths, me.id);
                return postId;
            });
        } catch (RuntimeException e) {
            fileStorageService.deleteAll(paths);
            throw e;
        }
    }

    //---BOARDS AND LISTS---

    /** The food group's browse board: Open listings, its own city first, Suspended donors hidden (R2.3). Handover instructions are never included. */
    public List<Post> getOpenListings() {
        Account me = Session.require();
        if (me.role != Account.Role.FOOD_GROUP) throw new RuleViolationException("Only food groups browse listings.");
        List<Post> board = readBoard(Post.Kind.LISTING, me);
        for (Post post : board) post.food.handoverInstructions = null;
        return board;
    }

    /**
     * The donor's browse board: Open requests, its own city first, Suspended groups hidden (R2.3). A Casual donor
     * sees only requests that accept raw or sealed-packaged food, and only those categories (6.5).
     */
    public List<Post> getOpenRequests() {
        Account me = Session.require();
        if (me.role != Account.Role.BUSINESS_DONOR && me.role != Account.Role.CASUAL_DONOR) {
            throw new RuleViolationException("Only donors browse requests.");
        }
        List<Post> board = readBoard(Post.Kind.REQUEST, me);
        if (me.role != Account.Role.CASUAL_DONOR) return board;
        List<Post> visible = new ArrayList<>();
        for (Post post : board) {
            Set<FoodDetail.Category> allowed = EnumSet.noneOf(FoodDetail.Category.class);
            for (FoodDetail.Category category : post.use.acceptedCategories) {
                if (category.allowedForCasual()) allowed.add(category);
            }
            if (allowed.isEmpty()) continue;
            post.use.acceptedCategories = allowed;
            visible.add(post);
        }
        return visible;
    }

    /** The logged-in account's own posts, newest first, each with its offer count (My Listings, My Requests). */
    public List<Post> getMyPosts() {
        Account me = Session.require();
        return Database.inTransaction("Could not load your posts", c -> {
            List<Post> posts = postDao.findByPoster(c, me.id);
            for (Post post : posts) {
                post.photos = photoDao.findPaths(c, PhotoDao.Owner.POST, post.id);
                List<Integer> settled = offerDao.findIdsByStatus(c, post.id, Offer.Status.ACCEPTED);
                if (settled.isEmpty()) settled = offerDao.findIdsByStatus(c, post.id, Offer.Status.COMPLETED);
                if (!settled.isEmpty()) post.acceptedOffer = offerDao.findById(c, settled.get(0));
            }
            return posts;
        });
    }

    //---STATUS---

    /**
     * Moves a post from one status to the next, only if it is still in the first, and writes the log (A3). This is the
     * one door for post status changes; OfferService calls it inside its own transaction. Returns false if the post had moved on.
     */
    public boolean changeStatus(Connection c, int postId, Post.Status from, Post.Status to, String reason, Integer changedBy) throws SQLException {
        if (postDao.updateStatusIf(c, postId, from, to, to == Post.Status.CANCELLED ? reason : null) == 0) return false;
        logDao.logStatusChange(c, POST_ENTITY, postId, from.name(), to.name(), reason, changedBy);
        return true;
    }

    //---CANCEL---

    /**
     * The poster cancels an Open or Accepted post with a reason (R6.1). One transaction: waiting offers are Declined,
     * an Accepted offer is Cancelled, the post is Cancelled, all logged. The reason stays on the post for the offerers
     * to read. Not allowed once the donor has confirmed release (M3), and never after the handoff completes.
     */
    public void cancelPost(int postId, String reason) {
        Account me = Session.require();
        requireText(reason, "Write a reason for cancelling.", 500);
        String why = reason.trim();
        Database.inTransaction("Could not cancel the post", c -> {
            Post post = postDao.findForUpdate(c, postId);
            if (post == null) throw new RuleViolationException("That post no longer exists.");
            if (post.posterId != me.id) throw new RuleViolationException("Only the poster can cancel this post.");
            if (post.status != Post.Status.OPEN && post.status != Post.Status.ACCEPTED) {
                throw new RuleViolationException("Only an Open or Accepted post can be cancelled.");
            }
            for (int offerId : offerDao.findIdsByStatus(c, postId, Offer.Status.ACCEPTED)) {
                if (offerDao.findById(c, offerId).releasedAt != null) {
                    throw new RuleViolationException("The food has already been released, so this post can no longer be cancelled.");
                }
                settleOffer(c, offerId, Offer.Status.ACCEPTED, Offer.Status.CANCELLED, why, me.id);
            }
            for (int offerId : offerDao.findIdsByStatus(c, postId, Offer.Status.OFFERED)) {
                settleOffer(c, offerId, Offer.Status.OFFERED, Offer.Status.DECLINED, why, me.id);
            }
            changeStatus(c, postId, post.status, Post.Status.CANCELLED, why, me.id);
            return null;
        });
    }

    /**
     * Expiry (6.6, steps 2 and 3): an Open listing past its use-by, or an Open request past its needed-by, becomes
     * Expired and its Offered offers expire with it. Logged by the system (A3).
     */
    public void expireOpenPosts(Post.Kind kind, LocalDateTime now) {
        Database.inTransaction("Could not expire old posts", c -> {
            for (int postId : postDao.findOpenPastDeadline(c, kind, now)) {
                if (!changeStatus(c, postId, Post.Status.OPEN, Post.Status.EXPIRED, "Deadline passed", null)) continue;
                for (int offerId : offerDao.findIdsByStatus(c, postId, Offer.Status.OFFERED)) {
                    settleOffer(c, offerId, Offer.Status.OFFERED, Offer.Status.EXPIRED, "Deadline passed", null);
                }
            }
            return null;
        });
    }

    private void settleOffer(Connection c, int offerId, Offer.Status from, Offer.Status to, String reason, Integer changedBy) throws SQLException {
        if (offerDao.updateStatusIf(c, offerId, from, to) > 0) {
            logDao.logStatusChange(c, "OFFER", offerId, from.name(), to.name(), reason, changedBy);   // A3
        }
    }

    //---HELPERS---

    private List<Post> readBoard(Post.Kind kind, Account me) {
        return Database.inTransaction("Could not load the board", c -> {
            List<Post> board = postDao.findOpenBoard(c, kind, me.registration.city.id);
            for (Post post : board) post.photos = photoDao.findPaths(c, PhotoDao.Owner.POST, post.id);
            return board;
        });
    }

    private void savePhotosAndLog(Connection c, int postId, List<String> paths, int accountId) throws SQLException {
        for (String path : paths) photoDao.insert(c, PhotoDao.Owner.POST, postId, path);
        logDao.logStatusChange(c, POST_ENTITY, postId, null, Post.Status.OPEN.name(), null, accountId);   // A3
    }

    /** The logged-in account, if it has one of the roles and may post (6.3). */
    private static Account requirePoster(String wrongRoleMessage, Account.Role... allowed) {
        Account me = Session.require();
        if (!List.of(allowed).contains(me.role)) throw new RuleViolationException(wrongRoleMessage);
        if (!me.canPostOrOffer()) throw new RuleViolationException("A suspended account cannot post.");
        return me;
    }
}
