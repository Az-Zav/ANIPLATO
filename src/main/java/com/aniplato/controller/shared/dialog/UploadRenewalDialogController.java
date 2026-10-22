package com.aniplato.controller.shared.dialog;

import com.aniplato.model.Document;
import com.aniplato.service.DocumentReviewService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;

/** Upload Renewal: the renewed file and its new expiry date go to the admin for review (6.3). */
public class UploadRenewalDialogController extends DialogCard {

    private final DocumentReviewService documentReviewService = new DocumentReviewService();

    @FXML private Label uploadRenewalTitleLabel;
    @FXML private Label selectedRenewalFileLabel;
    @FXML private DatePicker newExpiryDatePicker;
    @FXML private CheckBox noExpiryCheck;

    private Document.Type type;
    private File file;
    private Runnable onDone;

    public void show(String documentName) {
        type = null;
        show(documentName, null);
    }

    /** Opens the dialog for one document type; Submit saves the renewal and then runs {@code onDone}. */
    public void show(Document.Type type, Runnable onDone) {
        this.type = type;
        show(type.label(), onDone);
        boolean optional = !type.requiresExpiry();   // only some documents may have no expiry date
        noExpiryCheck.setVisible(optional);
        noExpiryCheck.setManaged(optional);
    }

    private void show(String documentName, Runnable onDone) {
        this.onDone = onDone;
        file = null;
        open();
        uploadRenewalTitleLabel.setText("Upload renewal: " + documentName);
        selectedRenewalFileLabel.setText("No file chosen");
        newExpiryDatePicker.setValue(null);
        noExpiryCheck.setSelected(false);
        noExpiryCheck.setVisible(false);
        noExpiryCheck.setManaged(false);
    }

    @FXML
    private void handleChooseRenewalFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the renewed document");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF, JPG or PNG", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        File chosen = chooser.showOpenDialog(card.getScene() == null ? null : card.getScene().getWindow());
        if (chosen == null) return;
        file = chosen;
        selectedRenewalFileLabel.setText(chosen.getName());
    }

    // PATTERN:ACTION confirms uploading renewal document
    @FXML
    private void handleConfirmUploadRenewal() {
        if (type == null) {
            close();
            return;
        }
        confirm(() -> {
            documentReviewService.uploadRenewal(type, file, newExpiryDatePicker.getValue(), noExpiryCheck.isSelected());
            if (onDone != null) onDone.run();
        });
    }
}
