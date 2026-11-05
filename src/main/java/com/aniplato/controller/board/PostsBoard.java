package com.aniplato.controller.board;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.TableRows;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.AcceptanceService;
import com.aniplato.service.BoardService;
import com.aniplato.service.BoardService.PostFigures;
import com.aniplato.service.HandoffService;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * My Listings (a donor's posts) and My Requests (a food group's posts) as a board. The poster is the donor on a
 * listing and the food group on a request, so the buttons follow the post's kind, not the user's role (A5). Only
 * Open and Accepted posts are listed; the rest are in History.
 */
public final class PostsBoard {

    /** Which posts the screen lists; it fixes the wording. */
    public enum Kind { LISTINGS, REQUESTS }

    private static final double[] WIDTHS = {18, 6, 12, 10, 17, 10, 7, 20};

    private final Kind kind;
    private final Board<Post> board;
    private final BoardService figures = new BoardService();
    private final PostService postService = new PostService();
    private final HandoffService handoffService = new HandoffService();
    private final AcceptanceService acceptanceService = new AcceptanceService();
    private final DialogController dialog;
    private final Navigator.Screen offersScreen;
    private final Runnable refresh;
    private final Map<Integer, String> phones = new HashMap<>();   // the other side's number, only while the offer is Accepted (R4.3)

    /** @param offersScreen where "Review offers" goes; @param refresh reloads the list after an action */
    public PostsBoard(Kind kind, DialogController dialog, Navigator.Screen offersScreen, Runnable refresh) {
        this.kind = kind;
        this.dialog = dialog;
        this.offersScreen = offersScreen;
        this.refresh = refresh;
        boolean listings = kind == Kind.LISTINGS;
        Button create = new Button(listings ? "New Listing" : "New Request");
        create.getStyleClass().add("primary-button");
        create.setOnAction(e -> Navigator.show(listings ? Navigator.Screen.LISTING_FORM : Navigator.Screen.NEW_REQUEST));
        board = new Board<Post>(listings ? "My Listings" : "My Requests")
                .action(create)
                .tabs(List.of(new Board.Tab<>("All", p -> true),
                        new Board.Tab<>("Accepted", p -> p.status == Post.Status.ACCEPTED),
                        new Board.Tab<>("Open", p -> p.status == Post.Status.OPEN)))
                .search(listings ? "Search food or food group…" : "Search request or donor…",
                        p -> heading(p) + " " + (p.acceptedOffer == null ? "" : p.acceptedOffer.offererName))
                .columns(WIDTHS, listings ? "FOOD" : "REQUEST", "KG", listings ? "USE-BY" : "NEEDED-BY", "STATUS",
                        "ACCEPTED OFFER", "PICKUP", "RECEIPT", "")
                .row(this::row)
                .footnote("Rescued, expired and cancelled " + (listings ? "listings" : "requests") + " move to History.")
                .empty(listings ? "You have not posted a listing yet." : "You have not posted a request yet.");
    }

    public Node view() {
        return board.view();
    }

    /** The rows now shown after the tab and search. Tests read this. */
    public List<Post> visible() {
        return board.visible();
    }

    public void show(List<Post> mine) {
        List<Post> active = mine.stream().filter(p -> p.status == Post.Status.OPEN || p.status == Post.Status.ACCEPTED).toList();
        phones.clear();
        for (Post post : active) {
            Offer accepted = post.acceptedOffer;
            if (accepted != null && accepted.status == Offer.Status.ACCEPTED) {
                phones.put(post.id, FormSupport.load(() -> acceptanceService.getContactPhone(accepted.offererId, accepted.id), "unavailable"));
            }
        }
        PostFigures f = FormSupport.load(() -> figures.posts(mine), null);
        if (f != null) board.tiles(tiles(f));
        board.show(active);
    }

    private List<Tile> tiles(PostFigures f) {
        String awaiting = FormSupport.kg(f.awaitingKg()) + " · confirm at pickup";
        if (kind == Kind.LISTINGS) {
            return List.of(
                    Tile.of(Tile.Icon.BLUE, "OFFERS TO REVIEW", String.valueOf(f.offersToReview()),
                            "On " + f.postsWithOffers() + " of your " + f.open() + " open listing" + (f.open() == 1 ? "" : "s")),
                    Tile.of(Tile.Icon.ORANGE, "ACCEPTED, AWAITING PICKUP", String.valueOf(f.awaitingPickup()), awaiting),
                    Tile.of(Tile.Icon.GREEN, "OPEN ON BOARD", String.valueOf(f.open()), FormSupport.kg(f.openKg()) + " on the board"));
        }
        return List.of(
                Tile.of(Tile.Icon.BLUE, "OPEN REQUESTS", String.valueOf(f.open()), FormSupport.kg(f.openKg()) + " needed"),
                Tile.of(Tile.Icon.ORANGE, "ACCEPTED, AWAITING PICKUP", String.valueOf(f.awaitingPickup()), awaiting),
                Tile.of(Tile.Icon.GREEN, "OFFERS TO REVIEW", String.valueOf(f.offersToReview()),
                        "On " + f.postsWithOffers() + " open request" + (f.postsWithOffers() == 1 ? "" : "s")));
    }

    // ---- one row ----

    private GridPane row(Post post) {
        Offer accepted = post.acceptedOffer;
        boolean isAccepted = post.status == Post.Status.ACCEPTED && accepted != null;
        GridPane row = TableRows.row(WIDTHS);
        TableRows.put(row, 0, TableRows.text(heading(post), "form-label"));
        TableRows.put(row, 1, TableRows.text(FormSupport.kg(post.kg), "table-cell-text"));
        TableRows.put(row, 2, TableRows.text(FormSupport.soon(BoardService.dueAt(post), LocalDate.now()), "table-cell-muted"));
        TableRows.put(row, 3, TableRows.pill(post.status.label(), isAccepted ? "accepted" : "open"));
        TableRows.put(row, 4, isAccepted ? acceptedCell(post, accepted) : offersCell(post));
        TableRows.put(row, 5, TableRows.text(isAccepted ? FormSupport.soon(accepted.pickupDeadline(), LocalDate.now()) : null, "table-cell-muted"));
        TableRows.put(row, 6, TableRows.text(isAccepted ? "Pending" : null, "table-cell-muted"));
        TableRows.put(row, 7, TableRows.stack(actions(post).toArray(Node[]::new)));
        return row;
    }

    private Node acceptedCell(Post post, Offer accepted) {
        String phone = phones.get(post.id);
        Label name = TableRows.text(accepted.offererName, "form-label");
        return phone == null ? name : TableRows.stack(name, TableRows.text("contact " + phone, "table-cell-muted"));
    }

    private static Node offersCell(Post post) {
        return post.offerCount > 0
                ? TableRows.text(post.offerCount + (post.offerCount == 1 ? " offer" : " offers"), "board-offers")
                : TableRows.text("No offers yet", "table-cell-muted");
    }

    private List<Node> actions(Post post) {
        List<Node> actions = new ArrayList<>();
        Offer accepted = post.acceptedOffer;
        boolean listing = post.isListing();
        boolean released = accepted != null && accepted.releasedAt != null;
        if (post.status == Post.Status.OPEN && post.offerCount > 0) {
            actions.add(TableRows.rowButton("Review offers", () -> {
                Selection.selectPost(post.id);
                Navigator.show(offersScreen);
            }));
        }
        if (post.status == Post.Status.ACCEPTED && accepted != null) {
            if (listing && !released) {
                actions.add(TableRows.rowButton("Confirm Release", () -> confirmRelease(post, accepted)));
            } else if (listing) {
                actions.add(TableRows.text("Waiting for food group receipt", "table-cell-muted"));
            } else if (released) {
                actions.add(TableRows.rowButton("Confirm Receipt", () -> confirmReceipt(accepted)));
            } else {
                Button waiting = TableRows.rowButton("Confirm Receipt", () -> { });
                waiting.setDisable(true);
                actions.add(waiting);
                actions.add(TableRows.text("Waiting for donor release", "table-cell-muted"));
            }
            actions.add(TableRows.rowLink("Flag a concern", () -> dialog.showFlagSafetyConcernMode(accepted.id)));
        }
        if ((post.status == Post.Status.OPEN || post.status == Post.Status.ACCEPTED) && !released) {
            actions.add(TableRows.rowLink("Cancel " + post.noun(), () -> cancel(post)));
        }
        return actions;
    }

    // ---- what the buttons do (the rules are in the services) ----

    private void confirmRelease(Post post, Offer offer) {
        String phone = FormSupport.load(() -> acceptanceService.getContactPhone(offer.offererId, offer.id), "");
        dialog.showConfirmReleaseMode(post.food.description, FormSupport.kg(offer.kg), offer.offererName,
                FormSupport.when(offer.pickupDeadline()), phone, offer.id, () -> {
                    handoffService.confirmRelease(offer.id);
                    refresh.run();
                });
    }

    private void confirmReceipt(Offer offer) {
        String phone = FormSupport.load(() -> acceptanceService.getContactPhone(offer.offererId, offer.id), "");
        dialog.showConfirmReceiptMode(offer.food.description, FormSupport.kg(offer.kg), offer.offererName,
                FormSupport.when(offer.food.pickupEnd), phone, FormSupport.when(offer.releasedAt), offer.id, () -> {
                    handoffService.confirmReceipt(offer.id);
                    refresh.run();
                });
    }

    private void cancel(Post post) {
        String accepted = post.acceptedOffer == null ? null : post.acceptedOffer.offererName;
        dialog.showCancelListingMode(heading(post), accepted, reason -> {
            postService.cancelPost(post.id, reason);
            refresh.run();
        });
    }

    private static String heading(Post post) {
        return post.isListing() ? post.food.description : post.use.useDescription;
    }
}
