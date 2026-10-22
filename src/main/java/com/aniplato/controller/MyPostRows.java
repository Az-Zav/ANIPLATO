package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.AcceptanceService;
import com.aniplato.service.HandoffService;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The rows of My Listings (a donor's posts) and My Requests (a food group's posts). The poster is the donor on a
 * listing and the food group on a request, so the buttons follow the post's kind, not the user's role (A5).
 */
public final class MyPostRows {

    private final PostService postService = new PostService();
    private final HandoffService handoffService = new HandoffService();
    private final AcceptanceService acceptanceService = new AcceptanceService();
    private final DialogController dialog;
    private final Navigator.Screen offersScreen;
    private final Runnable refresh;

    /** @param offersScreen where "View offers" goes; @param refresh reloads the list after an action */
    public MyPostRows(DialogController dialog, Navigator.Screen offersScreen, Runnable refresh) {
        this.dialog = dialog;
        this.offersScreen = offersScreen;
        this.refresh = refresh;
    }

    public List<Node> build(List<Post> posts) {
        List<Node> rows = new ArrayList<>();
        for (Post post : posts) {
            VBox card = PostCards.postCard(post, Cards.actions(buttons(post).toArray(Button[]::new)));
            if (post.acceptedOffer != null) card.getChildren().add(card.getChildren().size() - 1, contactLine(post));
            rows.add(card);
        }
        return rows;
    }

    private List<Button> buttons(Post post) {
        List<Button> buttons = new ArrayList<>();
        Offer accepted = post.acceptedOffer;
        boolean released = accepted != null && accepted.releasedAt != null;
        if (post.status == Post.Status.OPEN && post.offerCount > 0) {
            buttons.add(Cards.primary("View offers (" + post.offerCount + ")", () -> Navigator.show(offersScreen)));
        }
        if (post.status == Post.Status.ACCEPTED && post.isListing() && !released) {
            buttons.add(Cards.primary("Confirm release", () -> confirmRelease(post, accepted)));
        }
        if (post.status == Post.Status.ACCEPTED && !post.isListing() && released) {
            buttons.add(Cards.primary("Confirm receipt", () -> confirmReceipt(post, accepted)));
        }
        if ((post.status == Post.Status.OPEN || post.status == Post.Status.ACCEPTED) && !released) {
            buttons.add(Cards.secondary("Cancel", () -> cancel(post)));
        }
        if (accepted != null) {
            buttons.add(Cards.secondary("Flag a concern", () -> dialog.showFlagSafetyConcernMode(accepted.id)));
        }
        if (post.status == Post.Status.RESCUED && accepted != null) {
            buttons.add(Cards.primary("View record", () -> openRecord(accepted.id)));
        }
        return buttons;
    }

    /** The other side's contact number, shown while the offer is Accepted (R4.3). */
    private Node contactLine(Post post) {
        Offer offer = post.acceptedOffer;
        if (offer.status != Offer.Status.ACCEPTED) return Cards.muted("Completed with " + offer.offererName);
        String phone = FormSupport.load(() -> acceptanceService.getContactPhone(offer.offererId, offer.id), "unavailable");
        return Cards.line("Accepted: " + offer.offererName + "  |  " + FormSupport.kg(offer.kg) + "  |  contact " + phone
                + (released(offer) ? "  |  food released " + FormSupport.when(offer.releasedAt) : ""));
    }

    private static boolean released(Offer offer) {
        return offer.releasedAt != null;
    }

    private void confirmRelease(Post post, Offer offer) {
        String phone = FormSupport.load(() -> acceptanceService.getContactPhone(offer.offererId, offer.id), "");
        dialog.showConfirmReleaseMode(post.food.description, FormSupport.kg(offer.kg), offer.offererName,
                FormSupport.when(offer.pickupDeadline()), phone, offer.id, () -> {
                    handoffService.confirmRelease(offer.id);
                    refresh.run();
                });
    }

    private void confirmReceipt(Post post, Offer offer) {
        String phone = FormSupport.load(() -> acceptanceService.getContactPhone(offer.offererId, offer.id), "");
        dialog.showConfirmReceiptMode(offer.food.description, FormSupport.kg(offer.kg), offer.offererName,
                FormSupport.when(offer.food.pickupEnd), phone, FormSupport.when(offer.releasedAt), offer.id, () -> {
                    handoffService.confirmReceipt(offer.id);
                    refresh.run();
                });
    }

    private void cancel(Post post) {
        String title = post.isListing() ? post.food.description : post.use.useDescription;
        String accepted = post.acceptedOffer == null ? null : post.acceptedOffer.offererName;
        dialog.showCancelListingMode(title, accepted, reason -> {
            postService.cancelPost(post.id, reason);
            refresh.run();
        });
    }

    private static void openRecord(int offerId) {
        Selection.selectOffer(offerId);
        Navigator.show(Navigator.Screen.HANDOFF_RECORD);
    }
}
