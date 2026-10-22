package com.aniplato.controller.donor;

import com.aniplato.controller.DateTimeField;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.LabelConverter;
import com.aniplato.controller.PhotoPicker;
import com.aniplato.model.Account;
import com.aniplato.model.FoodDetail;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** The donor's New Listing form (R2.1). Reads the fields and hands them to PostService, which owns every rule. */
public class NewListingController {

    private final PostService postService = new PostService();
    private final Map<FoodDetail.Allergen, CheckBox> allergenChecks = new EnumMap<>(FoodDetail.Allergen.class);

    @FXML private TextField descriptionField;
    @FXML private ComboBox<FoodDetail.Category> categoryCombo;
    @FXML private FlowPane allergenBox;
    @FXML private TextField kgField;
    @FXML private DateTimeField useByField;
    @FXML private DateTimeField pickupStartField;
    @FXML private DateTimeField pickupEndField;
    @FXML private PhotoPicker photoPicker;
    @FXML private ComboBox<FoodDetail.Storage> storageCombo;
    @FXML private CheckBox attestationCheck;
    @FXML private TextArea handoverArea;

    @FXML
    private void initialize() {
        Account me = Session.require();
        for (FoodDetail.Category category : FoodDetail.Category.values()) {
            if (me.mayGive(category)) categoryCombo.getItems().add(category);   // a Casual donor sees raw and packaged only
        }
        storageCombo.getItems().addAll(FoodDetail.Storage.values());
        for (FoodDetail.Allergen allergen : FoodDetail.Allergen.values()) {
            CheckBox box = new CheckBox(allergen.label());
            allergenChecks.put(allergen, box);
            allergenBox.getChildren().add(box);
        }
        // Combo boxes show the enum's label, not its constant name.
        categoryCombo.setConverter(new LabelConverter<>(FoodDetail.Category::label));
        storageCombo.setConverter(new LabelConverter<>(FoodDetail.Storage::label));
    }

    // PATTERN:ACTION posts the listing, then opens My Listings
    @FXML
    private void handlePublish() {
        boolean saved = FormSupport.attempt(() -> {
            Set<FoodDetail.Allergen> allergens = EnumSet.noneOf(FoodDetail.Allergen.class);
            allergenChecks.forEach((allergen, box) -> {
                if (box.isSelected()) allergens.add(allergen);
            });
            postService.createListing(new PostService.ListingForm(descriptionField.getText(),
                    categoryCombo.getValue(), storageCombo.getValue(), allergens,
                    FormSupport.number(kgField.getText(), "the weight"), useByField.value("the use-by"),
                    pickupStartField.value("the pickup window start"), pickupEndField.value("the pickup window end"),
                    handoverArea.getText(), attestationCheck.isSelected(), photoPicker.files()));
        });
        if (saved) Navigator.show(Navigator.Screen.MY_LISTINGS);
    }
}
