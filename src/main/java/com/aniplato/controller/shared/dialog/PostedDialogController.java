package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * The "it is posted" moment after a listing, request, offer or report was saved. There is nothing to cancel, so both
 * buttons close the card and then do what they say (go to the list, or start another).
 */
public class PostedDialogController extends DialogCard {

    @FXML private Label postedTitleLabel;
    @FXML private Label postedDescriptionLabel;
    @FXML private Button postedPrimaryButton;
    @FXML private Button postedSecondaryButton;

    private Runnable onPrimary;
    private Runnable onSecondary;

    /** {@code secondaryLabel} may be null when there is only one place to go next. */
    public void show(String title, String description, String primaryLabel, Runnable primary, String secondaryLabel, Runnable secondary) {
        onPrimary = primary;
        onSecondary = secondary;
        open();
        postedTitleLabel.setText(title);
        postedDescriptionLabel.setText(description);
        postedPrimaryButton.setText(primaryLabel);
        boolean two = secondaryLabel != null;
        postedSecondaryButton.setVisible(two);
        postedSecondaryButton.setManaged(two);
        if (two) postedSecondaryButton.setText(secondaryLabel);
    }

    @FXML
    private void handlePrimary() {
        close();
        onPrimary.run();
    }

    @FXML
    private void handleSecondary() {
        close();
        onSecondary.run();
    }
}
