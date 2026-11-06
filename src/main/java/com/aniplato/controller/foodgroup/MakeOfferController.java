package com.aniplato.controller.foodgroup;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.controller.DateTimeField;
import com.aniplato.controller.FormInput;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PhotoPicker;
import com.aniplato.model.Post;
import com.aniplato.service.OfferService;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import com.aniplato.controller.board.PostSummary;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/** The food group's Make Offer form for a listing (R3.1). Reads the fields and hands them to OfferService. */
public class MakeOfferController {
    @FXML private DialogController dialogController;


    private final OfferService offerService = new OfferService();
    private final PostService postService = new PostService();
    private int postId;

    @FXML private VBox summaryBox;
    @FXML private TextField useDescriptionField;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField beneficiariesField;
    @FXML private TextField headcountField;
    @FXML private TextField placeField;
    @FXML private DateTimeField pickupField;
    @FXML private PhotoPicker photoPicker;
    @FXML private TextArea messageArea;
    @FXML private CheckBox attestationCheck;

    @FXML
    private void initialize() {
        postId = Selection.postId();
        List<Post> board = FormSupport.load(postService::getOpenListings, List.of());
        board.stream().filter(p -> p.id == postId).findFirst()
                .ifPresent(post -> summaryBox.getChildren().add(PostSummary.of(post)));
    }

    // PATTERN:ACTION sends the offer, then opens My Offers
    @FXML
    private void handleSend() {
        FormInput input = new FormInput();
        boolean sent = FormSupport.attempt(() -> offerService.sendListingOffer(new OfferService.ListingOfferForm(postId,
                useDescriptionField.getText(), eventDatePicker.getValue(), beneficiariesField.getText(),
                input.whole(headcountField.getText(), "how many people", FormInput.HEADCOUNT, 0), placeField.getText(),
                input.when(pickupField, "your pickup time", FormInput.PICKUP_TIME), messageArea.getText(), attestationCheck.isSelected(),
                photoPicker.files())), input);
        if (sent) dialogController.showPostedMode("Offer sent", "The donor will decide on it. You can follow it under My Offers.",
                "View my offers", () -> Navigator.show(Navigator.Screen.MY_OFFERS), null, null);
    }
}
