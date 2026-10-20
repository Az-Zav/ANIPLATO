package com.aniplato.controller.admin;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class AccountsController {

    @FXML private Label accountsSummaryLabel;
    @FXML private VBox accountRowsBox;

    // Embedded Dialog Overlay Controller Injection
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    @FXML
    private void initialize() {
        // TODO(service): populate accounts dynamically
    }

    @FXML
    private void handleSuspend(ActionEvent event) {
        Button btn = (Button) event.getSource();
        GridPane row = (GridPane) btn.getParent().getParent();
        VBox nameBox = (VBox) row.getChildren().get(0);
        Label nameLabel = (Label) nameBox.getChildren().get(0);
        String orgName = nameLabel.getText();

        if (dialogController != null) {
            dialogController.showSuspendAccountMode(orgName);
        }
    }

    @FXML
    private void handleReinstate(ActionEvent event) {
        Button btn = (Button) event.getSource();
        GridPane row = (GridPane) btn.getParent().getParent();
        VBox nameBox = (VBox) row.getChildren().get(0);
        Label nameLabel = (Label) nameBox.getChildren().get(0);
        String orgName = nameLabel.getText();

        if (dialogController != null) {
            dialogController.showReinstateAccountMode(orgName, "Renewed certificate was verified.");
        }
    }

    @FXML
    private void handleReview(ActionEvent event) {
        Navigator.show(Screen.REVIEW_REGISTRATION);
    }
}