package com.aniplato.controller.board;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PostCards;
import com.aniplato.controller.TableRows;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.FoodDetail;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.AcceptanceService;
import com.aniplato.service.AcceptanceService.PostOffers;
import com.aniplato.util.Navigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The offers on one listing or one request (Figma "Offers for this listing / request"): a back link, the post in a
 * strip of facts, then one card per offer with Decline and Accept offer. The poster is the donor on a listing and the
 * food group on a request; the page reads the same either way (A5). Only the first two offers show until the user asks for more.
 */
public final class OffersPage {

    /** What the offers are on; it fixes the wording. */
    public enum Kind { LISTING, REQUEST }

    private static final int SHOWN_FIRST = 2;

    private final Kind kind;
    private final DialogController dialog;
    private final Navigator.Screen back;
    private final Runnable refresh;
    private final AcceptanceService acceptanceService = new AcceptanceService();
    private final VBox root = new VBox(16);
    private PostOffers data;
    private boolean showAll;

    /** @param back the list this page came from; @param refresh reloads the page after a decline */
    public OffersPage(Kind kind, DialogController dialog, Navigator.Screen back, Runnable refresh) {
        this.kind = kind;
        this.dialog = dialog;
        this.back = back;
        this.refresh = refresh;
    }

    public Node view() {
        return root;
    }

    /** The offer cards now on the page. Tests read this. */
    public int shownOffers() {
        return (int) root.lookupAll(".offer-card").size();
    }

    /** Draws the page; {@code null} (no post chosen, or not yours) shows only the way back. */
    public void show(PostOffers loaded) {
        data = loaded;
        showAll = false;
        render();
    }

    private void render() {
        root.getChildren().clear();
        Hyperlink link = new Hyperlink("← " + (kind == Kind.LISTING ? "My Listings" : "My Requests"));
        link.getStyleClass().add("back-link");
        link.setOnAction(e -> Navigator.show(back));
        Label title = new Label("Offers for this " + (kind == Kind.LISTING ? "listing" : "request"));
        title.getStyleClass().add("page-title");
        root.getChildren().addAll(link, title);
        if (data == null) {
            root.getChildren().add(muted("Open " + (kind == Kind.LISTING ? "a listing from My Listings" : "a request from My Requests") + " to see its offers."));
            return;
        }
        root.getChildren().add(summary(data.post()));
        List<Offer> offers = data.offers();
        Label count = new Label(offers.size() + (offers.size() == 1 ? " offer" : " offers"));
        count.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox countRow = new HBox(8, count, spacer, muted("Accepting one offer declines the others. Contact numbers are shared once you accept."));
        countRow.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(countRow);
        if (offers.isEmpty()) {
            root.getChildren().add(muted("No offers are waiting for your decision."));
            return;
        }
        int limit = showAll ? offers.size() : Math.min(SHOWN_FIRST, offers.size());
        for (Offer offer : offers.subList(0, limit)) root.getChildren().add(card(offer, offers.size()));
        if (offers.size() > limit) {
            int more = offers.size() - limit;
            Hyperlink moreLink = new Hyperlink("+ " + more + " more " + (more == 1 ? "offer" : "offers"));
            moreLink.getStyleClass().add("back-link");
            moreLink.setOnAction(e -> {
                showAll = true;
                render();
            });
            root.getChildren().add(moreLink);
        }
    }

    // ---- the post ----

    private Node summary(Post post) {
        LocalDate today = LocalDate.now();
        List<Node> facts = post.isListing()
                ? List.of(fact("FOOD", post.food.description), fact("CATEGORY", post.food.category.label()),
                        fact("QUANTITY", FormSupport.kg(post.kg) + ", whole listing"),
                        fact("USE-BY", FormSupport.soon(post.food.useBy, today)),
                        fact("PICKUP WINDOW", FormSupport.window(post.food.pickupStart, post.food.pickupEnd, today)))
                : List.of(fact("REQUEST", post.use.useDescription),
                        fact("ACCEPTS", post.use.acceptedCategories.stream().map(FoodDetail.Category::label).sorted().collect(Collectors.joining(", "))),
                        fact("KG NEEDED", FormSupport.kg(post.kg)),
                        fact("NEEDED-BY", FormSupport.soon(post.use.dueAt, today)),
                        fact("EVENT DATE", post.use.eventDate == null ? "—" : post.use.eventDate.toString()));
        HBox strip = row(facts);
        VBox card = new VBox(strip);
        card.getStyleClass().add("table-card");
        card.setPadding(new Insets(20));
        return card;
    }

    // ---- one offer ----

    private Node card(Offer offer, int total) {
        LocalDate today = LocalDate.now();
        Label name = new Label(offer.offererName);
        name.getStyleClass().add("offer-title");
        VBox who = new VBox(2, name, muted("offered " + FormSupport.soon(offer.createdAt, today)));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button decline = TableRows.rowOutline("Decline", () -> decline(offer));
        Button accept = TableRows.rowButton("Accept offer", () -> accept(offer, total - 1));
        HBox header = new HBox(8, who, spacer, decline, accept);
        header.setAlignment(Pos.TOP_LEFT);

        List<Node> facts = offer.use != null
                ? List.of(fact("WHAT IT'S FOR", offer.use.useDescription),
                        fact("WHO IT FEEDS", offer.use.headcount + " people, " + offer.use.beneficiaries),
                        fact("WHERE", offer.use.place),
                        fact("PICKUP", FormSupport.soon(offer.use.dueAt, today)))
                : List.of(fact("FOOD", offer.food.description),
                        fact("CATEGORY AND STORAGE", offer.food.category.label() + " · " + offer.food.storage.label()),
                        fact("QUANTITY", FormSupport.kg(offer.kg)),
                        fact("PICKUP WINDOW", FormSupport.window(offer.food.pickupStart, offer.food.pickupEnd, today)));

        VBox words = new VBox(6);
        String message = offer.use != null ? offer.use.message : null;
        if (message != null && !message.isBlank()) words.getChildren().add(new Label("“" + message + "”"));
        boolean attested = offer.use != null ? offer.use.attestedAt != null : offer.food.attestedAt != null;
        if (attested) {
            Label attestation = new Label("✓ " + (offer.use != null
                    ? "Group attestation signed: free distribution, no resale, safe handling, photo consent"
                    : "Donor attestation signed: wholesome, labeled, donated in good faith"));
            attestation.getStyleClass().add("board-confirmed");
            attestation.setWrapText(true);
            words.getChildren().add(attestation);
        }
        HBox extras = new HBox(16, PostCards.thumbnails(offer.photos), words);
        extras.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, header, row(facts), extras);
        card.getStyleClass().addAll("table-card", "offer-card");
        card.setPadding(new Insets(20));
        return card;
    }

    private void accept(Offer offer, int others) {
        dialog.showAcceptOfferMode(offer.offererName, FormSupport.kg(offer.kg), FormSupport.when(offer.pickupDeadline()), others, () -> {
            acceptanceService.accept(offer.id);
            Navigator.show(back);   // the post is Accepted now, so it has no offers left to decide
        });
    }

    private void decline(Offer offer) {
        if (FormSupport.attempt(() -> acceptanceService.decline(offer.id))) refresh.run();
    }

    // ---- small pieces ----

    private static HBox row(List<Node> facts) {
        HBox row = new HBox(24);
        for (Node fact : facts) {
            HBox.setHgrow(fact, Priority.ALWAYS);
            ((VBox) fact).setPrefWidth(0);   // equal columns
        }
        row.getChildren().addAll(facts);
        return row;
    }

    private static VBox fact(String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("fact-label");
        Label text = new Label(value == null || value.isBlank() ? "—" : value);
        text.getStyleClass().add("fact-value");
        text.setWrapText(true);
        VBox box = new VBox(4, name, text);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-muted");
        label.setWrapText(true);
        return label;
    }
}
