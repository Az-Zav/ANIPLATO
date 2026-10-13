package com.aniplato.controller.donor;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class NewListingController {

    @FXML private TextField foodDescriptionField;
    @FXML private TextField quantityField;
    @FXML private DatePicker useByDatePicker;
    @FXML private TextField pickupWindowField;
    @FXML private TextArea notesArea;

    @FXML
    private void initialize() {
        // TODO(service): populate categoryComboBox from listingService.getCategories()
        // TODO(service): submit listing details to listingService.createListing(...) -- R1.1
    }

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handlePublish() {
        Navigator.show(Navigator.Screen.MY_LISTINGS);
    }
}
