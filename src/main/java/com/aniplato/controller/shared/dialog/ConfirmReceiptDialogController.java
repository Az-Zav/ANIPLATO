package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/** Confirm Receipt (Food Group). Does nothing but close until the handoff feature is built (FLOWS.md module 8). */
public class ConfirmReceiptDialogController extends DialogCard {

    @FXML private Label receiptFoodLabel;
    @FXML private Label receiptQuantityLabel;
    @FXML private Label receiptDonorLabel;
    @FXML private Label receiptPickupLabel;
    @FXML private Label receiptContactLabel;
    @FXML private CheckBox receiptQuantityCheck;
    @FXML private CheckBox receiptConditionCheck;
    @FXML private Label receiptNoticeLabel;

    public void show(String food, String qty, String donor, String pickup, String contact, String releaseTime) {
        open();
        receiptFoodLabel.setText(food);
        receiptQuantityLabel.setText(qty);
        receiptDonorLabel.setText(donor);
        receiptPickupLabel.setText(pickup);
        receiptContactLabel.setText(contact);
        receiptQuantityCheck.setSelected(true);
        receiptConditionCheck.setSelected(true);

        if (releaseTime != null && !releaseTime.isEmpty()) {
            receiptNoticeLabel.setText(donor + " confirmed release at " + releaseTime + ". Confirming receipt marks this listing Rescued.");
        } else {
            receiptNoticeLabel.setText("Confirming receipt marks this listing Rescued.");
        }
    }

    @FXML
    private void handleSwitchToFlagSafety() {
        openFlagSafetyConcern();
    }

    // PATTERN:ACTION confirms receiving food
    @FXML
    private void handleConfirmReceipt() {
        close();
    }
}
