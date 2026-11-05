package com.aniplato.controller.board;

import com.aniplato.controller.CardChoice;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PostCards;
import com.aniplato.controller.TableRows;
import com.aniplato.model.FoodDetail;
import com.aniplato.model.Post;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The card at the top of a Make Offer form: the listing or request being offered on, with its status, three small
 * facts, the photos, and (for a request) where the food should go and the group's message. Layout only.
 */
public final class PostSummary {

    private PostSummary() { }

    public static Node of(Post post) {
        LocalDate today = LocalDate.now();
        boolean listing = post.isListing();
        Label title = new Label(listing ? post.food.description : post.use.useDescription);
        title.getStyleClass().add("offer-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, title, spacer, TableRows.pill(post.status.label(), "open"));
        head.setAlignment(Pos.CENTER_LEFT);

        String meta = post.posterName + " · " + post.city + " · "
                + (listing ? post.food.category.label() + " · Use by " + FormSupport.soon(post.food.useBy, today)
                        : "Accepts " + post.use.acceptedCategories.stream().map(FoodDetail.Category::label).sorted().collect(Collectors.joining(", "))
                                + " · Needed by " + FormSupport.soon(post.use.dueAt, today));
        Label line = new Label(meta);
        line.getStyleClass().add("text-muted");
        line.setWrapText(true);

        List<Node> facts = listing
                ? List.of(fact("QUANTITY", FormSupport.kg(post.kg), "Whole listing only", false),
                        fact("USE-BY", FormSupport.soon(post.food.useBy, today), left(post.food.useBy), true),
                        fact("STORAGE", post.food.storage.label(), CardChoice.storageHint(post.food.storage), false))
                : List.of(fact("QUANTITY", FormSupport.kg(post.kg), "Up to " + FormSupport.kg(post.kg), false),
                        fact("NEEDED-BY", FormSupport.soon(post.use.dueAt, today), left(post.use.dueAt), true),
                        fact("EVENT", post.use.beneficiaries, post.use.headcount + " people · " + post.use.eventDate, false));
        HBox row = new HBox(12);
        for (Node fact : facts) {
            HBox.setHgrow(fact, Priority.ALWAYS);
            ((VBox) fact).setPrefWidth(0);
        }
        row.getChildren().addAll(facts);

        VBox card = new VBox(12, head, line, row);
        if (!listing) {
            card.getChildren().add(extra("Where the food should go: " + post.use.place));
            if (post.use.message != null) card.getChildren().add(extra("Message: " + post.use.message));
        }
        if (!post.photos.isEmpty()) card.getChildren().add(PostCards.thumbnails(post.photos));
        card.getStyleClass().add("table-card");
        card.setPadding(new Insets(20));
        return card;
    }

    /** "1h 40m left" or "2d 3h left"; empty once the time has passed. */
    static String left(LocalDateTime due) {
        return FormSupport.timeLeft(due, LocalDateTime.now());
    }

    private static VBox fact(String label, String value, String note, boolean urgent) {
        Label name = new Label(label);
        name.getStyleClass().add("fact-label");
        Label text = new Label(value);
        text.getStyleClass().add("fact-value-big");
        text.setWrapText(true);
        Label hint = new Label(note);
        hint.getStyleClass().add(urgent ? "fact-urgent" : "text-muted");
        hint.setWrapText(true);
        VBox box = new VBox(4, name, text, hint);
        box.getStyleClass().add("fact-card");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Label extra(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-muted");
        label.setWrapText(true);
        return label;
    }
}
