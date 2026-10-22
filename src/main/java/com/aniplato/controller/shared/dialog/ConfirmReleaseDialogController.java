package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/** Confirm Release (Donor): the donor says the food was handed over. */
public class ConfirmReleaseDialogController extends DialogCard {

    @FXML private Label releaseFoodLabel;
    @FXML private Label releaseQuantityLabel;
    @FXML private Label releaseFoodGroupLabel;
    @FXML private Label releasePickupLabel;
    @FXML private Label releaseContactLabel;
    @FXML private CheckBox releaseSealedCheck;
    @FXML private CheckBox releaseStorageCheck;
    @FXML private Label releaseNoticeLabel;

    private Runnable onRelease;

    /** Release: {@code onConfirm} runs the service call. */
    public void show(String food, String qty, String group, String pickup, String contact, Runnable onConfirm) {
        onRelease = onConfirm;
        open();
        releaseFoodLabel.setText(food);
        releaseQuantityLabel.setText(qty);
        releaseFoodGroupLabel.setText(group);
        releasePickupLabel.setText(pickup);
        releaseContactLabel.setText(contact);
        releaseSealedCheck.setSelected(true);
        releaseStorageCheck.setSelected(true);
        releaseNoticeLabel.setText(group + " hasn’t confirmed receipt yet. The listing becomes Rescued once they do.");
    }

    @FXML
    private void handleSwitchToFlagSafety() {
        openFlagSafetyConcern();
    }

    // PATTERN:ACTION confirms food release
    @FXML
    private void handleConfirmRelease() {
        confirm(onRelease);
    }
}
