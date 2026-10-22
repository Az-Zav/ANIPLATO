package com.aniplato.controller.shared.dialog;

import com.aniplato.service.SafetyService;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/** Flag Safety Concern: either side of an Accepted or Completed offer tells the admin about the other (R7.1). */
public class FlagSafetyDialogController extends DialogCard {

    private final SafetyService safetyService = new SafetyService();

    @FXML private TextArea flagSafetyConcernTextArea;

    private int offerId;

    /** Opens the dialog for one offer; Send raises the concern about its other side. */
    public void show(int offerId) {
        this.offerId = offerId;
        open();
        flagSafetyConcernTextArea.clear();
    }

    // PATTERN:ACTION confirms flagging safety concern
    @FXML
    private void handleConfirmFlagSafety() {
        confirm(() -> safetyService.raise(offerId, flagSafetyConcernTextArea.getText()));
    }
}
