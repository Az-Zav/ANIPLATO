package com.aniplato.controller.foodgroup;

import com.aniplato.controller.DateTimeField;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PhotoPicker;
import com.aniplato.controller.PostCards;
import com.aniplato.model.Post;
import com.aniplato.service.OfferService;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/** The food group's Make Offer form for a listing (R3.1). Reads the fields and hands them to OfferService. */
public class MakeOfferController {

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
                .ifPresent(post -> summaryBox.getChildren().add(PostCards.postCard(post, new VBox())));
    }

    // PATTERN:ACTION sends the offer, then opens My Offers
    @FXML
    private void handleSend() {
        boolean sent = FormSupport.attempt(() -> offerService.sendListingOffer(new OfferService.ListingOfferForm(postId,
                useDescriptionField.getText(), eventDatePicker.getValue(), beneficiariesField.getText(),
                FormSupport.wholeNumber(headcountField.getText(), "how many people"), placeField.getText(),
                pickupField.value("your pickup time"), messageArea.getText(), attestationCheck.isSelected(),
                photoPicker.files())));
        if (sent) Navigator.show(Navigator.Screen.MY_OFFERS);
    }
}
