package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/** Flag Safety Concern. Does nothing but close until the safety feature is wired (FLOWS.md module 10). */
public class FlagSafetyDialogController extends DialogCard {

    @FXML private TextArea flagSafetyConcernTextArea;

    public void show() {
        open();
        flagSafetyConcernTextArea.clear();
    }

    // PATTERN:ACTION confirms flagging safety concern
    @FXML
    private void handleConfirmFlagSafety() {
        close();
    }
}
