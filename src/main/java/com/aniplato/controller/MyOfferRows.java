package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.OfferService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The rows of My Offers: the offers one account sent. The offerer is the food group on a listing and the donor on a
 * request, so the buttons follow what the offer carries, not the user's role (A5).
 */
public final class MyOfferRows {

    private final OfferService offerService = new OfferService();
    private final DialogController dialog;
    private final Runnable refresh;

    public MyOfferRows(DialogController dialog, Runnable refresh) {
        this.dialog = dialog;
        this.refresh = refresh;
    }

    public List<Node> build(List<Offer> offers) {
        List<Node> rows = new ArrayList<>();
        for (Offer offer : offers) {
            Post post = offer.post;
            String heading = post.isListing() ? post.food.description : post.use.useDescription;
            VBox card = Cards.card(Cards.title(heading),
                    Cards.muted("To " + post.posterName + "  |  " + FormSupport.kg(offer.kg) + "  |  " + offer.status.label()
                            + "  |  sent " + FormSupport.when(offer.createdAt)));
            Cards.addLines(card, offer.food != null ? PostCards.foodLines(offer.food) : PostCards.useLines(offer.use, "Pickup time"));
            card.getChildren().add(PostCards.thumbnails(offer.photos));
            if (offer.status == Offer.Status.ACCEPTED || offer.status == Offer.Status.COMPLETED) {
                card.getChildren().add(Cards.line(contact(offer)));
                if (post.isListing() && post.food.handoverInstructions != null) {
                    card.getChildren().add(Cards.line("Handover instructions: " + post.food.handoverInstructions));
                }
            }
            if (post.cancelReason != null) card.getChildren().add(Cards.muted("The post was cancelled: " + post.cancelReason));
            card.getChildren().add(Cards.actions(buttons(offer).toArray(Button[]::new)));
            rows.add(card);
        }
        return rows;
    }

    private String contact(Offer offer) {
        if (offer.status == Offer.Status.COMPLETED) return "Completed with " + offer.post.posterName;
        String phone = FormSupport.load(() -> offerService.getContactPhone(offer.post.posterId, offer.id), "unavailable");
        return "Accepted by " + offer.post.posterName + "  |  contact " + phone;
    }

    /** The donor confirms the food was released to the group that will collect it. */
    private Button confirmRelease(Offer offer) {
        return Cards.primary("Confirm release", () -> dialog.showConfirmReleaseMode(offer.food.description,
                FormSupport.kg(offer.kg), offer.post.posterName, FormSupport.when(offer.food.pickupEnd),
                FormSupport.load(() -> offerService.getContactPhone(offer.post.posterId, offer.id), ""), offer.id, () -> {
                    offerService.confirmRelease(offer.id);
                    refresh.run();
                }));
    }

    /** The donor has released, so the food group confirms it received the food. */
    private Button confirmReceipt(Offer offer) {
        return Cards.primary("Confirm receipt", () -> dialog.showConfirmReceiptMode(offer.post.food.description,
                FormSupport.kg(offer.kg), offer.post.posterName, FormSupport.when(offer.use.dueAt),
                FormSupport.load(() -> offerService.getContactPhone(offer.post.posterId, offer.id), ""),
                FormSupport.when(offer.releasedAt), offer.id, () -> {
                    offerService.confirmReceipt(offer.id);
                    refresh.run();
                }));
    }

    private List<Button> buttons(Offer offer) {
        List<Button> buttons = new ArrayList<>();
        boolean supplier = offer.food != null;   // the donor's offer carries the food; the food group's carries its use
        if (offer.status == Offer.Status.OFFERED) {
            buttons.add(Cards.secondary("Withdraw", () -> dialog.showWithdrawOfferMode(offer.post.posterName, () -> {
                offerService.withdraw(offer.id);
                refresh.run();
            })));
        }
        if (offer.status == Offer.Status.WITHDRAWN && offer.post.status == Post.Status.OPEN) {
            buttons.add(Cards.primary("Offer again", () -> {
                Selection.selectPost(offer.postId);
                Navigator.show(offer.post.isListing() ? Navigator.Screen.MAKE_OFFER : Navigator.Screen.DONOR_MAKE_OFFER);
            }));
        }
        if (offer.status == Offer.Status.ACCEPTED && supplier && offer.releasedAt == null) buttons.add(confirmRelease(offer));
        if (offer.status == Offer.Status.ACCEPTED && !supplier && offer.releasedAt != null) buttons.add(confirmReceipt(offer));
        if (offer.status == Offer.Status.ACCEPTED || offer.status == Offer.Status.COMPLETED) {
            buttons.add(Cards.secondary("Flag a concern", () -> dialog.showFlagSafetyConcernMode(offer.id)));
        }
        if (offer.status == Offer.Status.COMPLETED) {
            buttons.add(Cards.primary("View record", () -> {
                Selection.selectOffer(offer.id);
                Navigator.show(Navigator.Screen.HANDOFF_RECORD);
            }));
        }
        return buttons;
    }
}
