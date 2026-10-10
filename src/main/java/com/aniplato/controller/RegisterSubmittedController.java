package com.aniplato.controller;

import com.aniplato.model.RegistrationData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class RegisterSubmittedController {

    @FXML private Label orgSubheadLabel;
    @FXML private Label emailNoticeLabel;

    @FXML
    public void initialize() {
        RegistrationData data = RegistrationData.getInstance();

        String orgName = data.getOrgName() != null && !data.getOrgName().isEmpty() ? data.getOrgName() : "Manila Artisanal Bakeshop";
        String city = data.getCity() != null && !data.getCity().isEmpty() ? data.getCity() : "Mandaluyong City";
        String email = data.getEmail() != null && !data.getEmail().isEmpty() ? data.getEmail() : "dispatch@manilaartisanal.ph";

        orgSubheadLabel.setText(orgName + " · " + city);
        emailNoticeLabel.setText("We sent a confirmation to " + email + ". No account exists yet, so there's nothing to log in to.");
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            // Clears shared registration data for future sessions
            RegistrationData.getInstance().clear();

            URL loginLocation = getClass().getResource("/com/aniplato/view/login.fxml");
            if (loginLocation != null) {
                Parent loginRoot = FXMLLoader.load(loginLocation);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(loginRoot);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to return to Login view:");
            e.printStackTrace();
        }
    }
}