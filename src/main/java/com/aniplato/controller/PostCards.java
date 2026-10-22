package com.aniplato.controller;

import com.aniplato.model.FoodDetail;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.model.UseDetail;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** What a listing, a request and an offer look like as cards. Shared by every list screen so they read the same. */
public final class PostCards {

    private PostCards() { }

    /** The lines that describe food: category, storage, allergens, times, handover text if the viewer may see it. */
    public static List<String> foodLines(FoodDetail f) {
        List<String> lines = new ArrayList<>();
        String allergens = f.allergens.isEmpty() ? "" : "  |  Allergens: "
                + f.allergens.stream().map(FoodDetail.Allergen::label).collect(Collectors.joining(", "));
        lines.add(f.category.label() + "  |  " + f.storage.label() + allergens);
        lines.add("Pickup window: " + FormSupport.when(f.pickupStart) + " to " + FormSupport.when(f.pickupEnd));
        lines.add("Use by: " + FormSupport.when(f.useBy));
        if (f.handoverInstructions != null) lines.add("Handover instructions: " + f.handoverInstructions);
        return lines;
    }

    /** The lines that describe a food group's use of the food. {@code dueLabel} says what due_at means ("Needed by", "Pickup time"). */
    public static List<String> useLines(UseDetail u, String dueLabel) {
        List<String> lines = new ArrayList<>();
        lines.add("For: " + u.useDescription + (u.eventDate == null ? "" : "  |  Event date: " + u.eventDate));
        lines.add("Feeds " + u.headcount + " people: " + u.beneficiaries);
        lines.add("Place: " + u.place);
        lines.add(dueLabel + ": " + FormSupport.when(u.dueAt));
        if (!u.acceptedCategories.isEmpty()) {
            lines.add("Accepts: " + u.acceptedCategories.stream().map(FoodDetail.Category::label).collect(Collectors.joining(", ")));
        }
        if (u.message != null) lines.add("Message: " + u.message);
        return lines;
    }

    /** A listing or request as a card, with the given action row under it. */
    public static VBox postCard(Post post, Node actions) {
        String heading = post.isListing() ? post.food.description : post.use.useDescription;
        VBox card = Cards.card(Cards.title(heading),
                Cards.muted(post.posterName + "  |  " + post.city + "  |  " + FormSupport.kg(post.kg) + "  |  " + post.status.label()));
        Cards.addLines(card, post.isListing() ? foodLines(post.food) : useLines(post.use, "Needed by"));
        if (post.cancelReason != null) card.getChildren().add(Cards.muted("Cancelled: " + post.cancelReason));
        card.getChildren().add(actions);
        return card;
    }

    /** An offer as a card: who made it, how much, what it carries. */
    public static VBox offerCard(Offer offer, Node actions) {
        VBox card = Cards.card(Cards.title(offer.offererName),
                Cards.muted(FormSupport.kg(offer.kg) + "  |  " + offer.status.label() + "  |  sent " + FormSupport.when(offer.createdAt)));
        Cards.addLines(card, offer.food != null ? foodLines(offer.food) : useLines(offer.use, "Pickup time"));
        card.getChildren().add(actions);
        return card;
    }
}
