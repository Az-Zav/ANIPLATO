package com.aniplato.controller;

import com.aniplato.model.Registration;
import com.aniplato.util.RegistrationDraft;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RegisterSubmittedController {

    // FXML: orgSubheadLabel shows "<name> · <city>"; emailNoticeLabel shows the confirmation sentence
    @FXML private Label orgSubheadLabel;
    @FXML private Label emailNoticeLabel;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        Registration submitted = RegistrationDraft.get();
        orgSubheadLabel.setText(submitted.name + " · " + submitted.city.name);
        emailNoticeLabel.setText("We sent a confirmation to " + submitted.email
                + ". No account exists yet, so there's nothing to log in to.");
        RegistrationDraft.clear();   // a new registration starts from an empty form
    }
}
