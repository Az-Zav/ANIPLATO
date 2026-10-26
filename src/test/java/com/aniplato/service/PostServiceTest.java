package com.aniplato.service;

import com.aniplato.model.FoodDetail;
import com.aniplato.model.Post;
import com.aniplato.service.PostService.ListingForm;
import com.aniplato.service.PostService.RequestForm;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 2: posting a listing or a request (R2.1, R2.2), and the boards and lists that show them (R2.3). */
class PostServiceTest extends DbTest {

    private final PostService posts = new PostService();

    @TempDir
    Path tmp;

    //---FORM BUILDERS: a valid form, then one change per test---

    private ListingForm listing(FoodDetail.Category category) {
        LocalDateTime now = LocalDateTime.now();
        return new ListingForm("Sandwiches", category, FoodDetail.Storage.AMBIENT_DRY, Set.of(FoodDetail.Allergen.GLUTEN),
                8, now.plusDays(2), now.plusHours(2), now.plusHours(6), "  Back door  ", true, Fixtures.photos(tmp, 2));
    }

    private RequestForm request() {
        LocalDateTime neededBy = LocalDateTime.now().plusDays(3);
        return new RequestForm("Sunday feeding", neededBy.toLocalDate().plusDays(1), "Elderly", 40, "Barangay hall",
                neededBy, "  Bring boxes  ", Set.of(FoodDetail.Category.RAW, FoodDetail.Category.PACKAGED), 20, true,
                Fixtures.photos(tmp, 1));
    }

    private ListingForm with(ListingForm f, double kg, LocalDateTime useBy, LocalDateTime start, LocalDateTime end, boolean attested, List<File> photos) {
        return new ListingForm(f.description(), f.category(), f.storage(), f.allergens(), kg, useBy, start, end,
                f.handoverInstructions(), attested, photos);
    }

    //---POSTING A LISTING---

    @Test
    void aDonorPostsAnOpenListingWithItsDetailPhotosAndLog() {
        int donorId = loginAs(BUSINESS).id;

        int id = posts.createListing(listing(FoodDetail.Category.COOKED));

        assertEquals("OPEN", TestDb.scalar("SELECT status FROM post WHERE post_id = ?", id));
        assertEquals("LISTING", TestDb.scalar("SELECT kind FROM post WHERE post_id = ?", id));
        assertEquals((long) donorId, TestDb.count("SELECT posted_by FROM post WHERE post_id = ?", id));
        assertEquals((Object) TestDb.scalar("SELECT city_id FROM registration WHERE email = ?", BUSINESS),
                (Object) TestDb.scalar("SELECT city_id FROM post WHERE post_id = ?", id));
        assertEquals("Back door", TestDb.scalar("SELECT handover_instructions FROM food_detail fd JOIN post p "
                + "ON p.food_detail_id = fd.food_detail_id WHERE p.post_id = ?", id));
        assertEquals("gluten", TestDb.scalar("SELECT allergens FROM food_detail fd JOIN post p "
                + "ON p.food_detail_id = fd.food_detail_id WHERE p.post_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM food_detail fd JOIN post p ON p.food_detail_id = fd.food_detail_id "
                + "WHERE p.post_id = ? AND fd.attested_at IS NOT NULL", id));
        assertEquals(2L, TestDb.count("SELECT COUNT(*) FROM photo WHERE post_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'POST' AND entity_id = ? "
                + "AND from_status IS NULL AND to_status = 'OPEN' AND changed_by = ?", id, donorId));
    }

    @Test
    void aListingNeedsPositiveKgAndTheRightDates() {
        loginAs(BUSINESS);
        ListingForm ok = listing(FoodDetail.Category.BAKED);
        LocalDateTime now = LocalDateTime.now();

        assertRule("The weight must be more than 0 kg.", () -> posts.createListing(with(ok, 0, ok.useBy(), ok.pickupStart(), ok.pickupEnd(), true, ok.photos())));
        assertRule("Use-by must be in the future.", () -> posts.createListing(with(ok, 5, now.minusHours(1), ok.pickupStart(), ok.pickupEnd(), true, ok.photos())));
        assertRule("The pickup window must end after it starts.", () -> posts.createListing(with(ok, 5, ok.useBy(), ok.pickupEnd(), ok.pickupStart(), true, ok.photos())));
        assertRule("The pickup window must end before the use-by.", () -> posts.createListing(with(ok, 5, now.plusHours(4), ok.pickupStart(), now.plusHours(5), true, ok.photos())));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM post WHERE posted_by = ?", accountId(BUSINESS)));   // only the seed listing
    }

    @Test
    void aListingNeedsTheAttestationAndOneToFivePhotos() {
        loginAs(BUSINESS);
        ListingForm ok = listing(FoodDetail.Category.BAKED);

        assertRule("Tick the attestation to continue.", () -> posts.createListing(with(ok, 5, ok.useBy(), ok.pickupStart(), ok.pickupEnd(), false, ok.photos())));
        assertRule("Add 1 to 5 photos.", () -> posts.createListing(with(ok, 5, ok.useBy(), ok.pickupStart(), ok.pickupEnd(), true, List.of())));
        assertRule("Add 1 to 5 photos.", () -> posts.createListing(with(ok, 5, ok.useBy(), ok.pickupStart(), ok.pickupEnd(), true, Fixtures.photos(tmp, 6))));
        assertEquals(2L, TestDb.count("SELECT COUNT(*) FROM photo"));   // only the seed photos
    }

    @Test
    void aCasualDonorMayPostRawOrPackagedOnly() {
        loginAs(CASUAL);

        assertRule("Casual donors can give raw or sealed-packaged food only.", () -> posts.createListing(listing(FoodDetail.Category.COOKED)));
        assertRule("Casual donors can give raw or sealed-packaged food only.", () -> posts.createListing(listing(FoodDetail.Category.BAKED)));
        assertTrue(posts.createListing(listing(FoodDetail.Category.RAW)) > 0);
        assertTrue(posts.createListing(listing(FoodDetail.Category.PACKAGED)) > 0);
    }

    @Test
    void onlyAnActiveDonorPostsAListing() {
        loginAs(GROUP);
        assertRule("Food groups post requests, not listings.", () -> posts.createListing(listing(FoodDetail.Category.RAW)));
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", BUSINESS);
        loginAs(BUSINESS);
        assertRule("A suspended account cannot post.", () -> posts.createListing(listing(FoodDetail.Category.RAW)));
    }

    @Test
    void aRefusedListingLeavesNoUploadedFileBehind() {
        loginAs(BUSINESS);
        File uploads = new File(System.getProperty("aniplato.uploads.dir"));
        int before = uploads.list().length;   // other tests in this run leave files here
        File bad = tmp.resolve("notes.txt").toFile();
        List<File> photos = Fixtures.photos(tmp, 1);
        photos = List.of(photos.get(0), bad);   // the second is not an image and does not exist

        ListingForm f = listing(FoodDetail.Category.RAW);
        List<File> mixed = photos;
        assertRefused(() -> posts.createListing(with(f, 5, f.useBy(), f.pickupStart(), f.pickupEnd(), true, mixed)));

        assertEquals(before, uploads.list().length, "no file may be saved when the form is refused");
    }

    //---POSTING A REQUEST---

    @Test
    void aFoodGroupPostsAnOpenRequestWithItsDetailPhotosAndLog() {
        int groupId = loginAs(GROUP).id;

        int id = posts.createRequest(request());

        assertEquals("OPEN", TestDb.scalar("SELECT status FROM post WHERE post_id = ?", id));
        assertEquals("REQUEST", TestDb.scalar("SELECT kind FROM post WHERE post_id = ?", id));
        assertEquals("Bring boxes", TestDb.scalar("SELECT message FROM use_detail ud JOIN post p "
                + "ON p.use_detail_id = ud.use_detail_id WHERE p.post_id = ?", id));
        assertEquals("RAW,PACKAGED", TestDb.scalar("SELECT accepted_categories FROM use_detail ud JOIN post p "
                + "ON p.use_detail_id = ud.use_detail_id WHERE p.post_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM photo WHERE post_id = ?", id));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'POST' AND entity_id = ? "
                + "AND to_status = 'OPEN' AND changed_by = ?", id, groupId));
    }

    @Test
    void aGroupMayHaveSeveralOpenRequests() {
        loginAs(GROUP);
        posts.createRequest(request());
        posts.createRequest(request());
        assertEquals(3L, TestDb.count("SELECT COUNT(*) FROM post WHERE kind = 'REQUEST' AND status = 'OPEN'"));
    }

    @Test
    void aRequestChecksItsDatesCategoriesAndAttestation() {
        loginAs(GROUP);
        RequestForm ok = request();

        assertRule("Needed-by must be in the future.", () -> posts.createRequest(new RequestForm(ok.eventName(), ok.eventDate(),
                ok.beneficiaries(), ok.headcount(), ok.place(), LocalDateTime.now().minusHours(1), null, ok.acceptedCategories(), 5, true, ok.photos())));
        assertRule("The event date must be on or after the needed-by date.", () -> posts.createRequest(new RequestForm(ok.eventName(),
                ok.neededBy().toLocalDate().minusDays(1), ok.beneficiaries(), ok.headcount(), ok.place(), ok.neededBy(), null, ok.acceptedCategories(), 5, true, ok.photos())));
        assertRule("Pick at least one category you will accept.", () -> posts.createRequest(new RequestForm(ok.eventName(), ok.eventDate(),
                ok.beneficiaries(), ok.headcount(), ok.place(), ok.neededBy(), null, Set.of(), 5, true, ok.photos())));
        assertRule("Tick the attestation to continue.", () -> posts.createRequest(new RequestForm(ok.eventName(), ok.eventDate(),
                ok.beneficiaries(), ok.headcount(), ok.place(), ok.neededBy(), null, ok.acceptedCategories(), 5, false, ok.photos())));
        assertRule("The weight must be more than 0 kg.", () -> posts.createRequest(new RequestForm(ok.eventName(), ok.eventDate(),
                ok.beneficiaries(), ok.headcount(), ok.place(), ok.neededBy(), null, ok.acceptedCategories(), -1, true, ok.photos())));
        assertRule("How many people it feeds must be more than 0.", () -> posts.createRequest(new RequestForm(ok.eventName(), ok.eventDate(),
                ok.beneficiaries(), 0, ok.place(), ok.neededBy(), null, ok.acceptedCategories(), 5, true, ok.photos())));
    }

    @Test
    void onlyAnActiveFoodGroupPostsARequest() {
        loginAs(BUSINESS);
        assertRule("Only a food group can post a request.", () -> posts.createRequest(request()));
    }

    //---BOARDS---

    @Test
    void theFoodGroupSeesOpenListingsOwnCityFirstWithoutHandoverInstructions() {
        TestDb.exec("UPDATE registration SET city_id = (SELECT city_id FROM city WHERE name = 'Makati') WHERE email = ?", CASUAL);
        loginAs(CASUAL);
        posts.createListing(listing(FoodDetail.Category.RAW));   // newer, but in Makati
        loginAs(GROUP);

        List<Post> board = posts.getOpenListings();

        assertEquals(2, board.size());
        assertEquals("Quezon City", board.get(0).city);
        assertEquals("Makati", board.get(1).city);
        assertNull(board.get(1).food.handoverInstructions);
        assertEquals(1, board.get(0).photos.size());
    }

    @Test
    void theOneBoardIsListingsForAFoodGroupAndRequestsForADonor() {
        loginAs(GROUP);
        assertEquals(Post.Kind.LISTING, posts.getOpenBoard().get(0).kind);
        loginAs(BUSINESS);
        assertEquals(Post.Kind.REQUEST, posts.getOpenBoard().get(0).kind);
    }

    @Test
    void aSuspendedPostersPostsAreHiddenUntilReinstated() {
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", BUSINESS);
        loginAs(GROUP);
        assertTrue(posts.getOpenListings().isEmpty());
        TestDb.exec("UPDATE account SET status = 'ACTIVE', suspend_reason = NULL WHERE email = ?", BUSINESS);
        assertEquals(1, posts.getOpenListings().size());
    }

    @Test
    void onlyOpenPostsAreOnTheBoard() {
        TestDb.exec("UPDATE post SET status = 'CANCELLED' WHERE kind = 'LISTING'");
        loginAs(GROUP);
        assertTrue(posts.getOpenListings().isEmpty());
    }

    @Test
    void aBusinessDonorSeesEveryOpenRequest() {
        loginAs(BUSINESS);
        List<Post> board = posts.getOpenRequests();
        assertEquals(1, board.size());
        assertEquals(Set.of(FoodDetail.Category.RAW, FoodDetail.Category.PACKAGED, FoodDetail.Category.BAKED),
                board.get(0).use.acceptedCategories);
    }

    @Test
    void aCasualDonorSeesOnlyRequestsAcceptingRawOrPackagedAndOnlyThoseCategories() {
        loginAs(CASUAL);
        List<Post> board = posts.getOpenRequests();
        assertEquals(1, board.size());
        assertEquals(Set.of(FoodDetail.Category.RAW, FoodDetail.Category.PACKAGED), board.get(0).use.acceptedCategories);

        TestDb.exec("UPDATE use_detail SET accepted_categories = 'COOKED,BAKED'");
        assertTrue(posts.getOpenRequests().isEmpty());
    }

    @Test
    void eachSideBrowsesItsOwnBoardOnly() {
        loginAs(BUSINESS);
        assertRule("Only food groups browse listings.", posts::getOpenListings);
        loginAs(GROUP);
        assertRule("Only donors browse requests.", posts::getOpenRequests);
    }

    //---MY POSTS---

    @Test
    void myPostsAreMyOwnNewestFirstWithOfferCounts() {
        loginAs(BUSINESS);
        int second = posts.createListing(listing(FoodDetail.Category.RAW));
        Fixtures.offerOnSeedListing("OFFERED", 5);

        List<Post> mine = posts.getMyPosts();

        assertEquals(2, mine.size());
        assertEquals(second, mine.get(0).id);
        assertEquals(0, mine.get(0).offerCount);
        assertEquals(1, mine.get(1).offerCount);
        assertFalse(mine.stream().anyMatch(p -> p.posterId != accountId(BUSINESS)));
        assertEquals(2, mine.get(0).photos.size());
        assertEquals(LocalDate.now(), mine.get(0).createdAt.toLocalDate());
    }
}
