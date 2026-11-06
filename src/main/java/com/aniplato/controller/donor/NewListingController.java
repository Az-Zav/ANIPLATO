package com.aniplato.controller.donor;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.controller.DateTimeField;
import com.aniplato.controller.FormInput;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PhotoPicker;
import com.aniplato.model.Account;
import com.aniplato.model.FoodDetail;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Session;
import com.aniplato.controller.ChipChoice;
import com.aniplato.controller.CardChoice;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** The donor's New Listing form (R2.1). Reads the fields and hands them to PostService, which owns every rule. */
public class NewListingController {
    @FXML private DialogController dialogController;


    private final PostService postService = new PostService();
    private final Map<FoodDetail.Allergen, CheckBox> allergenChecks = new EnumMap<>(FoodDetail.Allergen.class);

    @FXML private TextField descriptionField;
    @FXML private ChipChoice<FoodDetail.Category> categoryChoice;
    @FXML private FlowPane allergenBox;
    @FXML private TextField kgField;
    @FXML private DateTimeField useByField;
    @FXML private DateTimeField pickupStartField;
    @FXML private DateTimeField pickupEndField;
    @FXML private PhotoPicker photoPicker;
    @FXML private CardChoice<FoodDetail.Storage> storageChoice;
    @FXML private CheckBox attestationCheck;
    @FXML private TextArea handoverArea;

    @FXML
    private void initialize() {
        Account me = Session.require();
        categoryChoice.setItems(java.util.Arrays.stream(FoodDetail.Category.values()).filter(me::mayGive).toList(),
                FoodDetail.Category::label);   // a Casual donor sees raw and packaged only
        storageChoice.setItems(java.util.List.of(FoodDetail.Storage.values()), FoodDetail.Storage::label, CardChoice::storageHint);
        for (FoodDetail.Allergen allergen : FoodDetail.Allergen.values()) {
            CheckBox box = new CheckBox(allergen.label());
            box.getStyleClass().add("chip-check");
            allergenChecks.put(allergen, box);
            allergenBox.getChildren().add(box);
        }
    }

    // PATTERN:ACTION posts the listing, then opens My Listings
    @FXML
    private void handlePublish() {
        FormInput input = new FormInput();
        boolean saved = FormSupport.attempt(() -> {
            Set<FoodDetail.Allergen> allergens = EnumSet.noneOf(FoodDetail.Allergen.class);
            allergenChecks.forEach((allergen, box) -> {
                if (box.isSelected()) allergens.add(allergen);
            });
            postService.createListing(new PostService.ListingForm(descriptionField.getText(),
                    categoryChoice.getValue(), storageChoice.getValue(), allergens,
                    input.number(kgField.getText(), "the weight", FormInput.WEIGHT), input.when(useByField, "the use-by", FormInput.USE_BY),
                    input.when(pickupStartField, "the pickup window start", FormInput.PICKUP_WINDOW),
                    input.when(pickupEndField, "the pickup window end", FormInput.PICKUP_WINDOW),
                    handoverArea.getText(), attestationCheck.isSelected(), photoPicker.files()));
        }, input);
        if (saved) dialogController.showPostedMode("Listing posted", "Food groups can see it on their board now.",
                "View my listings", () -> Navigator.show(Navigator.Screen.MY_LISTINGS),
                "Post another", () -> Navigator.show(Navigator.Screen.LISTING_FORM));
    }
}
