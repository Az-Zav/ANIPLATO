package com.aniplato.controller.foodgroup;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.board.OffersBoard;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.service.OfferService;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

/** My Offers for the food group (offers on listings). */
public class MyOffersController {

    private final OfferService offerService = new OfferService();
    private OffersBoard board;

    @FXML private VBox boardHost;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        board = new OffersBoard(OffersBoard.Side.FOOD_GROUP, dialogController, this::refresh);
        boardHost.getChildren().setAll(board.view());
        refresh();
    }

    private void refresh() {
        board.show(FormSupport.load(offerService::getMyOffers, List.<Offer>of()));
    }
}
