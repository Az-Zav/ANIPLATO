package com.aniplato.controller.shared.dialog;

import com.aniplato.model.Document;
import com.aniplato.model.DocumentUpload;
import com.aniplato.service.ProfileService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Upgrade to Business donor: both permits go to the admin as a new registration for review (6.2, 6.3). */
public class UpgradeBusinessDonorDialogController extends DialogCard {

    private final ProfileService profileService = new ProfileService();

    @FXML private Label selectedMayorPermitFileLabel;
    @FXML private DatePicker mayorPermitExpiryDatePicker;
    @FXML private Label selectedSanitaryPermitFileLabel;
    @FXML private DatePicker sanitaryPermitExpiryDatePicker;
    @FXML private CheckBox consentCheck;

    private File mayorPermit;
    private File sanitaryPermit;
    private Runnable onDone;
    private boolean live;   // opened with a real action

    public void show() {
        live = false;
        onDone = null;
        reset();
    }

    /** Opens the dialog; Submit sends the upgrade and then runs {@code onDone}. */
    public void show(Runnable onDone) {
        live = true;
        this.onDone = onDone;
        reset();
    }

    private void reset() {
        mayorPermit = null;
        sanitaryPermit = null;
        open();
        selectedMayorPermitFileLabel.setText("No file chosen");
        mayorPermitExpiryDatePicker.setValue(null);
        selectedSanitaryPermitFileLabel.setText("No file chosen");
        sanitaryPermitExpiryDatePicker.setValue(null);
        consentCheck.setSelected(false);
    }

    @FXML
    private void handleChooseMayorPermitFile() {
        File chosen = choose("Choose your Mayor's Permit");
        if (chosen == null) return;
        mayorPermit = chosen;
        selectedMayorPermitFileLabel.setText(chosen.getName());
    }

    @FXML
    private void handleChooseSanitaryPermitFile() {
        File chosen = choose("Choose your Sanitary Permit");
        if (chosen == null) return;
        sanitaryPermit = chosen;
        selectedSanitaryPermitFileLabel.setText(chosen.getName());
    }

    private File choose(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF, JPG or PNG", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        return chooser.showOpenDialog(card.getScene() == null ? null : card.getScene().getWindow());
    }

    // PATTERN:ACTION confirms upgrading to business donor
    @FXML
    private void handleConfirmUpgradeBusinessDonor() {
        if (!live) {
            close();
            return;
        }
        confirm(() -> {
            List<DocumentUpload> uploads = new ArrayList<>();
            if (mayorPermit != null) uploads.add(new DocumentUpload(Document.Type.MAYORS_PERMIT, mayorPermit, mayorPermitExpiryDatePicker.getValue(), false));
            if (sanitaryPermit != null) uploads.add(new DocumentUpload(Document.Type.SANITARY_PERMIT, sanitaryPermit, sanitaryPermitExpiryDatePicker.getValue(), false));
            profileService.submitUpgrade(uploads, consentCheck.isSelected());
            if (onDone != null) onDone.run();
        });
    }
}
