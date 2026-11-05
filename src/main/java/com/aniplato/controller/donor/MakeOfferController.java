package com.aniplato.controller.donor;

import com.aniplato.controller.DateTimeField;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PhotoPicker;
import com.aniplato.model.Account;
import com.aniplato.model.FoodDetail;
import com.aniplato.model.Post;
import com.aniplato.service.OfferService;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import com.aniplato.util.Session;
import com.aniplato.controller.ChipChoice;
import com.aniplato.controller.CardChoice;
import com.aniplato.controller.board.PostSummary;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The donor's Make Offer form for a request (R3.2). Reads the fields and hands them to OfferService. */
public class MakeOfferController {

    private final OfferService offerService = new OfferService();
    private final PostService postService = new PostService();
    private final Map<FoodDetail.Allergen, CheckBox> allergenChecks = new EnumMap<>(FoodDetail.Allergen.class);
    private int postId;

    @FXML private VBox summaryBox;
    @FXML private TextField descriptionField;
    @FXML private ChipChoice<FoodDetail.Category> categoryChoice;
    @FXML private CardChoice<FoodDetail.Storage> storageChoice;
    @FXML private FlowPane allergenBox;
    @FXML private TextField kgField;
    @FXML private DateTimeField useByField;
    @FXML private DateTimeField pickupStartField;
    @FXML private DateTimeField pickupEndField;
    @FXML private PhotoPicker photoPicker;
    @FXML private TextArea handoverArea;
    @FXML private CheckBox attestationCheck;

    @FXML
    private void initialize() {
        postId = Selection.postId();
        Account me = Session.require();
        List<Post> board = FormSupport.load(postService::getOpenRequests, List.of());
        Post request = board.stream().filter(p -> p.id == postId).findFirst().orElse(null);
        List<FoodDetail.Category> allowed = List.of();
        if (request != null) {
            summaryBox.getChildren().add(PostSummary.of(request));
            // only what the request accepts and the donor may give
            allowed = request.use.acceptedCategories.stream().filter(me::mayGive).sorted().toList();
        }
        categoryChoice.setItems(allowed, FoodDetail.Category::label);
        storageChoice.setItems(List.of(FoodDetail.Storage.values()), FoodDetail.Storage::label, CardChoice::storageHint);
        for (FoodDetail.Allergen allergen : FoodDetail.Allergen.values()) {
            CheckBox box = new CheckBox(allergen.label());
            box.getStyleClass().add("chip-check");
            allergenChecks.put(allergen, box);
            allergenBox.getChildren().add(box);
        }
    }

    // PATTERN:ACTION sends the offer, then opens My Offers
    @FXML
    private void handleSend() {
        boolean sent = FormSupport.attempt(() -> {
            Set<FoodDetail.Allergen> allergens = EnumSet.noneOf(FoodDetail.Allergen.class);
            allergenChecks.forEach((allergen, box) -> {
                if (box.isSelected()) allergens.add(allergen);
            });
            offerService.sendRequestOffer(new OfferService.RequestOfferForm(postId, descriptionField.getText(),
                    categoryChoice.getValue(), storageChoice.getValue(), allergens,
                    FormSupport.number(kgField.getText(), "the weight"), useByField.value("the use-by"),
                    pickupStartField.value("the pickup window start"), pickupEndField.value("the pickup window end"),
                    handoverArea.getText(), attestationCheck.isSelected(), photoPicker.files()));
        });
        if (sent) Navigator.show(Navigator.Screen.DONOR_MY_OFFERS);
    }
}
