package com.aniplato.controller.shared.dialog;

import com.aniplato.service.ProfileService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/** Edit contact details: the organization's contact person and number (6.3). */
public class EditContactDialogController extends DialogCard {

    private final ProfileService profileService = new ProfileService();

    @FXML private TextField contactPersonField;
    @FXML private TextField contactPhoneField;

    private Runnable onSaved;

    /** Opens the dialog with the current details; {@code onSaved} runs after a successful save. */
    public void show(String contactPerson, String contactPhone, Runnable onSaved) {
        this.onSaved = onSaved;
        open();
        contactPersonField.setText(contactPerson);
        contactPhoneField.setText(contactPhone);
    }

    // PATTERN:ACTION saves the contact details
    @FXML
    private void handleConfirmEditContact() {
        confirm(() -> {
            profileService.updateContact(contactPersonField.getText(), contactPhoneField.getText());
            if (onSaved != null) onSaved.run();
        });
    }
}
