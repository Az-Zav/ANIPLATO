package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.function.Consumer;

/** Suspend Account: the admin's reason is shown to the organization (6.3). */
public class SuspendAccountDialogController extends DialogCard {

    @FXML private Label suspendDescriptionLabel;
    @FXML private TextArea suspendReasonTextArea;

    private Consumer<String> onSuspend;   // gets the reason typed in the dialog

    public void show(String orgName) {
        onSuspend = null;
        open();
        suspendDescriptionLabel.setText(orgName + " can still log in, upload renewals and finish handoffs it already accepted. It can’t post, re-post, offer or accept, and its listings are hidden from food groups, until you reinstate it.");
        suspendReasonTextArea.clear();
    }

    /** Suspend with a real action: {@code onConfirm} gets the reason. */
    public void show(String orgName, Consumer<String> onConfirm) {
        show(orgName);
        onSuspend = onConfirm;
    }

    // PATTERN:ACTION confirms account suspension
    @FXML
    private void handleConfirmSuspend() {
        if (onSuspend != null) {
            confirm(() -> onSuspend.accept(suspendReasonTextArea.getText()));
            return;
        }
        close();
    }
}
