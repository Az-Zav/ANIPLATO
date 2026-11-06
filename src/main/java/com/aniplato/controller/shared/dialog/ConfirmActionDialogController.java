package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * A plain "are you sure?" for an action that cannot be taken back (sign out, decline an offer). The caller supplies the
 * words and the action; the card holds no rule.
 */
public class ConfirmActionDialogController extends DialogCard {

    @FXML private Label confirmActionTitleLabel;
    @FXML private Label confirmActionDescriptionLabel;
    @FXML private Button confirmActionButton;

    private Runnable onConfirm;

    /** {@code danger} picks the red button (the action loses something) over the green one. */
    public void show(String title, String description, String confirmLabel, boolean danger, Runnable action) {
        onConfirm = action;
        open();
        confirmActionTitleLabel.setText(title);
        confirmActionDescriptionLabel.setText(description);
        confirmActionButton.setText(confirmLabel);
        confirmActionButton.getStyleClass().removeAll("danger-button", "primary-button");
        confirmActionButton.getStyleClass().add(danger ? "danger-button" : "primary-button");
    }

    @FXML
    private void handleConfirmAction() {
        confirm(onConfirm);
    }
}
