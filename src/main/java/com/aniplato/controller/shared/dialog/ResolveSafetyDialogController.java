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
    // (the message is shown and the dialog stays open, A8).
    private Consumer<String> onDismissNotice;
    private Consumer<String> onSuspendFromNotice;

    /** Resolve: each button's callback gets the admin note. */
    public void show(String raisedBy, String date, String concern, String flaggedOrg,
                     Consumer<String> onDismiss, Consumer<String> onSuspendAccount) {
        onDismissNotice = onDismiss;
        onSuspendFromNotice = onSuspendAccount;
        open();
        safetyNoticeDescriptionLabel.setText("Flagged by " + raisedBy + " on " + date + ": \"" + concern + "\" Close it as dismissed, or suspend " + flaggedOrg + ".");
        safetyAdminNoteTextArea.clear();
    }

    // PATTERN:ACTION dismisses safety notice
    @FXML
    private void handleDismissSafetyNotice() {
        confirm(() -> onDismissNotice.accept(safetyAdminNoteTextArea.getText()));
    }

    // PATTERN:ACTION suspends account from safety notice
    @FXML
    private void handleSuspendFromSafetyNotice() {
        confirm(() -> onSuspendFromNotice.accept(safetyAdminNoteTextArea.getText()));
    }
}
