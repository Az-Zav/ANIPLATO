package com.aniplato.controller.shared;

import com.aniplato.controller.Cards;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PostCards;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.OfferService;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Browse Listings (food group) and Browse Requests (donor): the Open posts of the other side, its city first. Shared screen:
 * it asks for "the board" and never checks the role (A5); the Make Offer form follows the post's kind.
 */
public class BrowseController {

    private final PostService postService = new PostService();
    private final OfferService offerService = new OfferService();

    @FXML private VBox rowsBox;
    @FXML private Label emptyLabel;

    @FXML
    private void initialize() {
        List<Post> board = FormSupport.load(postService::getOpenBoard, List.of());
        Set<Integer> offered = new HashSet<>();   // posts already offered on; a Withdrawn offer may be sent again
        for (Offer offer : FormSupport.load(offerService::getMyOffers, List.<Offer>of())) {
            if (offer.status != Offer.Status.WITHDRAWN) offered.add(offer.postId);
        }
        List<Node> rows = new ArrayList<>();
        for (Post post : board) {
            Button action = offered.contains(post.id)
                    ? Cards.secondary("Offered", () -> { })
                    : Cards.primary("Make offer", () -> {
                        Selection.selectPost(post.id);
                        Navigator.show(post.kind == Post.Kind.LISTING ? Navigator.Screen.MAKE_OFFER : Navigator.Screen.DONOR_MAKE_OFFER);
                    });
            action.setDisable(offered.contains(post.id));
            rows.add(PostCards.postCard(post, Cards.actions(action)));
        }
        Cards.fill(rowsBox, emptyLabel, rows);
    }
}
