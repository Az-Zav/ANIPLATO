package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.AcceptanceService;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The rows of Incoming Offers: the offers on one account's posts. The poster is the donor on a listing and the food
 * group on a request; the rows read the same either way (A5).
 */
public final class IncomingOfferRows {

    private final AcceptanceService acceptanceService = new AcceptanceService();
    private final DialogController dialog;
    private final Runnable refresh;

    public IncomingOfferRows(DialogController dialog, Runnable refresh) {
        this.dialog = dialog;
        this.refresh = refresh;
    }

    public List<Node> build(List<Offer> offers) {
        List<Node> rows = new ArrayList<>();
        for (Offer offer : offers) {
            Post post = offer.post;
            String on = post.isListing() ? post.food.description : post.use.useDescription;
            VBox card = Cards.card(Cards.title(offer.offererName),
                    Cards.muted("On " + on + "  |  " + FormSupport.kg(offer.kg) + "  |  " + offer.status.label()
                            + "  |  sent " + FormSupport.when(offer.createdAt)));
            Cards.addLines(card, offer.food != null ? PostCards.foodLines(offer.food) : PostCards.useLines(offer.use, "Pickup time"));
            card.getChildren().add(PostCards.thumbnails(offer.photos));
            if (offer.status == Offer.Status.ACCEPTED) {
                String phone = FormSupport.load(() -> acceptanceService.getContactPhone(offer.offererId, offer.id), "unavailable");
                card.getChildren().add(Cards.line("Contact " + offer.offererName + ": " + phone));
            }
            Button[] buttons = offer.status == Offer.Status.OFFERED
                    ? new Button[]{Cards.primary("Accept", () -> accept(offer, offers)), Cards.secondary("Decline", () -> decline(offer))}
                    : new Button[0];
            card.getChildren().add(Cards.actions(buttons));
            rows.add(card);
        }
        return rows;
    }

    private void accept(Offer offer, List<Offer> all) {
        int others = (int) all.stream().filter(o -> o.postId == offer.postId && o.id != offer.id
                && o.status == Offer.Status.OFFERED).count();
        dialog.showAcceptOfferMode(offer.offererName, FormSupport.kg(offer.kg), FormSupport.when(offer.pickupDeadline()), others, () -> {
            acceptanceService.accept(offer.id);
            refresh.run();
        });
    }

    private void decline(Offer offer) {
        if (FormSupport.attempt(() -> acceptanceService.decline(offer.id))) refresh.run();
    }
}
