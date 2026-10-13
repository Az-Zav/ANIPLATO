package com.aniplato.controller.shared;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HandoffRecordController {

    // Embedded Dialog Controller & Overlay Container
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    // Dynamic Header Labels
    @FXML private Label itemTitleLabel;
    @FXML private Label recordMetaLabel;
    @FXML private Label statusPillLabel;

    // Dynamic Details Labels
    @FXML private Label donorDetailsLabel;
    @FXML private Label foodGroupDetailsLabel;
    @FXML private Label foodTypeQuantityLabel;
    @FXML private Label statedUseLabel;
    @FXML private Label beneficiariesLabel;
    @FXML private Label pickupTimeLabel;
    @FXML private Label releaseTimestampLabel;
    @FXML private Label receiptTimestampLabel;
    @FXML private Label donorAttestationLabel;
    @FXML private Label groupAttestationLabel;

    @FXML
    private void initialize() {
        // TODO(service): Load handoff record details from HandoffService
    }

    // PATTERN:NAV -> routes back to History list
    @FXML
    private void handleBackToHistory() {
        Navigator.show(Screen.HISTORY);
    }
}