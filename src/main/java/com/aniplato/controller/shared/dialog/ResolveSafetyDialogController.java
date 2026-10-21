package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.function.Consumer;

/** Resolve Safety Notice: the admin dismisses it or suspends the flagged account, with a note. */
public class ResolveSafetyDialogController extends DialogCard {

    @FXML private Label safetyNoticeDescriptionLabel;
    @FXML private TextArea safetyAdminNoteTextArea;

    // What each button does on confirm; a callback gets the admin note and may throw RuleViolationException
    // (the message is shown and the dialog stays open, A8). A dialog opened without callbacks just closes.
    private Consumer<String> onDismissNotice;
    private Consumer<String> onSuspendFromNotice;

    public void show(String raisedBy, String date, String concern, String flaggedOrg) {
        onDismissNotice = null;
        onSuspendFromNotice = null;
        open();
        safetyNoticeDescriptionLabel.setText("Flagged by " + raisedBy + " on " + date + ": \"" + concern + "\" Close it as dismissed, or suspend " + flaggedOrg + ".");
        safetyAdminNoteTextArea.clear();
    }

    /** Resolve with real actions: each button's callback gets the admin note. */
    public void show(String raisedBy, String date, String concern, String flaggedOrg,
                     Consumer<String> onDismiss, Consumer<String> onSuspendAccount) {
        show(raisedBy, date, concern, flaggedOrg);
        onDismissNotice = onDismiss;
        onSuspendFromNotice = onSuspendAccount;
    }

    // PATTERN:ACTION dismisses safety notice
    @FXML
    private void handleDismissSafetyNotice() {
        if (onDismissNotice != null) {
            confirm(() -> onDismissNotice.accept(safetyAdminNoteTextArea.getText()));
            return;
        }
        close();
    }

    // PATTERN:ACTION suspends account from safety notice
    @FXML
    private void handleSuspendFromSafetyNotice() {
        if (onSuspendFromNotice != null) {
            confirm(() -> onSuspendFromNotice.accept(safetyAdminNoteTextArea.getText()));
            return;
        }
        close();
    }
}
