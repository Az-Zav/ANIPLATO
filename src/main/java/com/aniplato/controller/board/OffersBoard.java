package com.aniplato.controller.board;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.TableRows;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.AcceptanceService;
import com.aniplato.service.BoardService;
import com.aniplato.service.BoardService.OfferFigures;
import com.aniplato.service.HandoffService;
import com.aniplato.service.OfferService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * My Offers as a board: the offers one account sent. A food group offers on listings and a donor offers on requests;
 * the screen says which side it is for, and the buttons follow what the offer carries, not the user's role (A5).
 * Completed offers are in History.
 */
public final class OffersBoard {

    /** The side of the handoff this screen is for; it fixes the wording. */
    public enum Side { FOOD_GROUP, DONOR }

    private static final double[] WIDTHS = {17, 21, 8, 14, 13, 10, 17};

    private final Side side;
    private final Board<Offer> board;
    private final BoardService figures = new BoardService();
    private final OfferService offerService = new OfferService();
    private final HandoffService handoffService = new HandoffService();
    private final AcceptanceService acceptanceService = new AcceptanceService();
    private final DialogController dialog;
    private final Runnable refresh;
    private final Map<Integer, String> phones = new HashMap<>();   // the poster's number, only while the offer is Accepted (R4.3)

    /** @param refresh reloads the list after an action */
    public OffersBoard(Side side, DialogController dialog, Runnable refresh) {
        this.side = side;
        this.dialog = dialog;
        this.refresh = refresh;
        boolean group = side == Side.FOOD_GROUP;
        Button browse = new Button(group ? "Browse Listings" : "Browse Requests");
        browse.getStyleClass().add("primary-button");
        browse.setOnAction(e -> Navigator.show(group ? Navigator.Screen.BROWSE_LISTINGS : Navigator.Screen.DONOR_BROWSE_REQUESTS));
        board = new Board<Offer>("My Offers")
                .action(browse)
                .tabs(List.of(new Board.Tab<>("All", o -> true),
                        new Board.Tab<>(group ? "Waiting for donor" : "Waiting for food group", o -> o.status == Offer.Status.OFFERED),
                        new Board.Tab<>("Accepted", o -> o.status == Offer.Status.ACCEPTED),
                        new Board.Tab<>("Closed", o -> o.status != Offer.Status.OFFERED && o.status != Offer.Status.ACCEPTED)))
                .search(group ? "Search food or donor…" : "Search request or food group…",
                        o -> o.post.title() + " " + o.post.posterName)
                .columns(WIDTHS, group ? "FOOD" : "REQUEST", group ? "DONOR" : "FOOD GROUP", "KG", "PICKUP",
                        group ? "DONOR RELEASE" : "YOUR RELEASE", "STATUS", "")
                .row(this::row)
                .footnote("Accepted offers past their pickup time plus a 2-hour grace period lapse, and the post expires.")
                .empty("You have not sent an offer yet.");
    }

    public Node view() {
        return board.view();
    }

    /** The rows now shown after the tab and search. Tests read this. */
    public List<Offer> visible() {
        return board.visible();
    }

    public void show(List<Offer> mine) {
        List<Offer> open = mine.stream().filter(o -> o.status != Offer.Status.COMPLETED).toList();
        phones.clear();
        for (Offer offer : open) {
            if (offer.status == Offer.Status.ACCEPTED) {
                phones.put(offer.id, FormSupport.load(() -> acceptanceService.getContactPhone(offer.post.posterId, offer.id), "unavailable"));
            }
        }
        OfferFigures f = FormSupport.load(() -> figures.offers(mine), null);
        if (f != null) board.tiles(tiles(f));
        board.show(open);
    }

    private List<Tile> tiles(OfferFigures f) {
        boolean group = side == Side.FOOD_GROUP;
        return List.of(
                Tile.of(Tile.Icon.BLUE, "ACCEPTED OFFERS", String.valueOf(f.accepted()),
                        FormSupport.kg(f.acceptedKg()) + (group ? " · confirm at pickup" : " · confirm at release")),
                Tile.of(Tile.Icon.ORANGE, group ? "DONOR RELEASED" : "YOU RELEASED", String.valueOf(f.released()),
                        FormSupport.kg(f.releasedKg()) + (group ? " · ready for your receipt" : " · waiting for receipt")),
                Tile.kg(Tile.Icon.GREEN, "KG RESCUED", f.kgRescued(), "Total rescued to date"));
    }

    // ---- one row ----

    private GridPane row(Offer offer) {
        GridPane row = TableRows.row(WIDTHS);
        TableRows.put(row, 0, TableRows.text(offer.post.title(), "form-label"));
        TableRows.put(row, 1, partyCell(offer));
        TableRows.put(row, 2, TableRows.text(FormSupport.kg(offer.kg), "table-cell-text"));
        TableRows.put(row, 3, TableRows.text(FormSupport.soon(offer.pickupDeadline(), LocalDate.now()), "table-cell-muted"));
        TableRows.put(row, 4, releaseCell(offer));
        TableRows.put(row, 5, TableRows.pill(offer.status.label(), tone(offer.status)));
        TableRows.put(row, 6, TableRows.stack(actions(offer).toArray(Node[]::new)));
        return row;
    }

    private Node partyCell(Offer offer) {
        List<Node> lines = new ArrayList<>();
        lines.add(TableRows.text(offer.post.posterName, "table-cell-text"));
        String phone = phones.get(offer.id);
        if (phone != null) lines.add(TableRows.text("contact " + phone, "table-cell-muted"));
        Post post = offer.post;
        if (phone != null && post.isListing() && post.food.handoverInstructions != null) {
            lines.add(TableRows.text("Handover: " + post.food.handoverInstructions, "table-cell-muted"));
        }
        if (post.cancelReason != null) lines.add(TableRows.text("Cancelled: " + post.cancelReason, "table-cell-muted"));
        return TableRows.stack(lines.toArray(Node[]::new));
    }

    private static Node releaseCell(Offer offer) {
        if (offer.releasedAt != null) return TableRows.text("Confirmed", "board-confirmed");
        return TableRows.text(offer.status == Offer.Status.ACCEPTED ? "Pending" : null, offer.status == Offer.Status.ACCEPTED ? "board-offers" : "table-cell-muted");
    }

    private static String tone(Offer.Status status) {
        return switch (status) {
            case OFFERED -> "open";
            case ACCEPTED -> "accepted";
            case COMPLETED -> "done";
            default -> "neutral";
        };
    }

    private List<Node> actions(Offer offer) {
        List<Node> actions = new ArrayList<>();
        boolean supplier = offer.food != null;   // the donor's offer carries the food; the food group's carries its use
        boolean released = offer.releasedAt != null;
        if (offer.status == Offer.Status.OFFERED) {
            actions.add(TableRows.rowOutline("Withdraw", () -> dialog.showWithdrawOfferMode(offer.post.posterName, () -> {
                offerService.withdraw(offer.id);
                refresh.run();
            })));
        }
        if (offer.status == Offer.Status.WITHDRAWN && offer.post.status == Post.Status.OPEN) {
            actions.add(TableRows.rowButton("Offer again", () -> {
                Selection.selectPost(offer.postId);
                Navigator.show(offer.post.isListing() ? Navigator.Screen.MAKE_OFFER : Navigator.Screen.DONOR_MAKE_OFFER);
            }));
        }
        if (offer.status == Offer.Status.ACCEPTED) {
            if (supplier && !released) {
                actions.add(TableRows.rowButton("Confirm Release", () -> confirmRelease(offer)));
            } else if (supplier) {
                actions.add(disabled("Confirm Release", "Waiting for food group receipt"));
            } else if (released) {
                actions.add(TableRows.rowButton("Confirm Receipt", () -> confirmReceipt(offer)));
            } else {
                actions.add(disabled("Confirm Receipt", "Waiting for donor release"));
            }
            actions.add(TableRows.rowLink("Flag a concern", () -> dialog.showFlagSafetyConcernMode(offer.id)));
        }
        return actions;
    }

    /** A greyed button with the reason under it. */
    private static Node disabled(String text, String reason) {
        Button button = TableRows.rowButton(text, () -> { });
        button.setDisable(true);
        return TableRows.stack(button, TableRows.text(reason, "table-cell-muted"));
    }

    // ---- what the buttons do (the rules are in the services) ----

    private void confirmRelease(Offer offer) {
        dialog.showConfirmReleaseMode(offer.food.description, FormSupport.kg(offer.kg), offer.post.posterName,
                FormSupport.when(offer.food.pickupEnd),
                FormSupport.load(() -> acceptanceService.getContactPhone(offer.post.posterId, offer.id), ""), offer.id, () -> {
                    handoffService.confirmRelease(offer.id);
                    refresh.run();
                });
    }

    private void confirmReceipt(Offer offer) {
        dialog.showConfirmReceiptMode(offer.post.food.description, FormSupport.kg(offer.kg), offer.post.posterName,
                FormSupport.when(offer.use.dueAt),
                FormSupport.load(() -> acceptanceService.getContactPhone(offer.post.posterId, offer.id), ""),
                FormSupport.when(offer.releasedAt), offer.id, () -> {
                    handoffService.confirmReceipt(offer.id);
                    refresh.run();
                });
    }

}
