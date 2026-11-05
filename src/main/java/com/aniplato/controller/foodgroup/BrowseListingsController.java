package com.aniplato.controller.foodgroup;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.board.BrowseBoard;
import com.aniplato.model.Offer;
import com.aniplato.service.OfferService;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

/** Browse Listings: Open listings for the food group, its city first, as a board. Offering opens the Make Offer form. */
public class BrowseListingsController {

    private final PostService postService = new PostService();
    private final OfferService offerService = new OfferService();
    private BrowseBoard board;

    @FXML private VBox boardHost;

    @FXML
    private void initialize() {
        board = new BrowseBoard(BrowseBoard.Kind.LISTINGS, this::refresh, post -> {
            Selection.selectPost(post.id);
            Navigator.show(Navigator.Screen.MAKE_OFFER);
        });
        boardHost.getChildren().setAll(board.view());
        refresh();
    }

    private void refresh() {
        board.show(FormSupport.load(postService::getOpenListings, List.of()),
                BrowseBoard.offeredIds(FormSupport.load(offerService::getMyOffers, List.<Offer>of())));
    }
}
