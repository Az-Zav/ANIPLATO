package com.aniplato.controller.foodgroup;

import com.aniplato.controller.Cards;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.IncomingOfferRows;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Offer;
import com.aniplato.service.AcceptanceService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/** Offers Received: the offers on the food group's requests, accept or decline. */
public class FoodGroupIncomingOffersController {

    private final AcceptanceService acceptanceService = new AcceptanceService();
    private IncomingOfferRows rows;

    @FXML private VBox rowsBox;
    @FXML private Label emptyLabel;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        rows = new IncomingOfferRows(dialogController, this::refresh);
        refresh();
    }

    private void refresh() {
        List<Offer> offers = FormSupport.load(acceptanceService::getIncomingOffers, List.of());
        Cards.fill(rowsBox, emptyLabel, rows.build(offers));
    }
}
