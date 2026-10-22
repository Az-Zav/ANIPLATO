package com.aniplato.controller.foodgroup;

import com.aniplato.controller.Cards;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.MyOfferRows;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.service.OfferService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/** My Offers for the food group (offers on listings). Same rows as the shared screen. */
public class MyOffersController {

    private final OfferService offerService = new OfferService();
    private MyOfferRows rows;

    @FXML private VBox rowsBox;
    @FXML private Label emptyLabel;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        rows = new MyOfferRows(dialogController, this::refresh);
        refresh();
    }

    private void refresh() {
        List<Offer> offers = FormSupport.load(offerService::getMyOffers, List.of());
        Cards.fill(rowsBox, emptyLabel, rows.build(offers));
    }
}
