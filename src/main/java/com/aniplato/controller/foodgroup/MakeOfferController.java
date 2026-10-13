package com.aniplato.controller.foodgroup;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class MakeOfferController {

    @FXML private TextField intendedUseField;
    @FXML private TextField beneficiariesField;
    @FXML private TextField headcountField;
    @FXML private TextField locationField;
    @FXML private TextField pickupTimeField;
    @FXML private TextField messageField;
    @FXML private CheckBox pickupConfirmationCheck;
    @FXML private CheckBox groupAttestationCheck;

    @FXML
    private void initialize() {
        // TODO(service): load the selected listing facts and donor handover details
        // TODO(service): prefill the offer form from the selected listing context
    }

    // PATTERN:ACTION placeholder: navigates back without submitting an offer
    @FXML
    private void handleCancel() {
        Navigator.show(Navigator.Screen.BROWSE_LISTINGS);
    }

    // PATTERN:ACTION placeholder: validates and submits the offer through OfferService
    @FXML
    private void handleSendOffer() {
        // TODO(service): offerService.makeOffer(...) -- R4.1
        Navigator.show(Navigator.Screen.MY_OFFERS);
    }

    // PATTERN:ACTION placeholder: adds an optional offer photo
    @FXML
    private void handleAddPhoto() {
        // TODO(service): validate consent and attach the selected photo to the offer draft
    }
}
