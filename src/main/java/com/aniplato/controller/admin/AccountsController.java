package com.aniplato.controller.admin;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AccountsController {

    @FXML private Label accountSummaryLabel;
    @FXML private VBox accountRowsBox;
    @FXML private Label emptyLabel;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): accountService.getAllAccounts() -> update summary text and dynamic rows
    }

    // PATTERN:ACTION placeholder: suspends an active account
    @FXML
    private void handleSuspend() {
        // TODO(service): accountService.suspendAccount(...)
    }

    // PATTERN:ACTION placeholder: reinstates a suspended account
    @FXML
    private void handleReinstate() {
        // TODO(service): accountService.reinstateAccount(...)
    }

    // PATTERN:ACTION placeholder: opens detailed document review for account
    @FXML
    private void handleReview() {
        Navigator.show(Screen.REVIEW_REGISTRATION);
    }
}