package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/** Confirm Receipt (Food Group): the group says the food arrived. */
public class ConfirmReceiptDialogController extends DialogCard {

    @FXML private Label receiptFoodLabel;
    @FXML private Label receiptQuantityLabel;
    @FXML private Label receiptDonorLabel;
    @FXML private Label receiptPickupLabel;
    @FXML private Label receiptContactLabel;
    @FXML private CheckBox receiptQuantityCheck;
    @FXML private CheckBox receiptConditionCheck;
    @FXML private Label receiptNoticeLabel;

    private Runnable onReceipt;

    /** Receipt: {@code onConfirm} runs the service call. */
    public void show(String food, String qty, String donor, String pickup, String contact, String releaseTime, Runnable onConfirm) {
        onReceipt = onConfirm;
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
        confirm(onReceipt);
    }
}
