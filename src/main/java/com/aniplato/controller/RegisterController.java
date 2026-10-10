package com.aniplato.controller;

import com.aniplato.model.RegistrationData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class RegisterController {

    @FXML private Button donorToggleBtn;
    @FXML private Button foodGroupToggleBtn;

    @FXML private TextField orgNameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> cityCombo;
    @FXML private TextField contactPersonField;
    @FXML private TextField contactNumberField;
    @FXML private TextField emailField;

    // Default role selection
    private String selectedRole = "BUSINESS_DONOR";

    @FXML
    public void initialize() {
        if (typeCombo != null && typeCombo.getValue() == null) {
            typeCombo.setValue("Bakery");
        }
        if (cityCombo != null && cityCombo.getValue() == null) {
            cityCombo.setValue("Manila");
        }
    }

    @FXML
    private void handleSelectBusinessDonor() {
        this.selectedRole = "BUSINESS_DONOR";

        if (donorToggleBtn != null && foodGroupToggleBtn != null) {
            donorToggleBtn.getStyleClass().remove("segment-btn-inactive");
            if (!donorToggleBtn.getStyleClass().contains("segment-btn-active")) {
                donorToggleBtn.getStyleClass().add("segment-btn-active");
            }

            foodGroupToggleBtn.getStyleClass().remove("segment-btn-active");
            if (!foodGroupToggleBtn.getStyleClass().contains("segment-btn-inactive")) {
                foodGroupToggleBtn.getStyleClass().add("segment-btn-inactive");
            }
        }
    }

    @FXML
    private void handleSelectFoodGroup() {
        this.selectedRole = "FOOD_GROUP";

        if (donorToggleBtn != null && foodGroupToggleBtn != null) {
            foodGroupToggleBtn.getStyleClass().remove("segment-btn-inactive");
            if (!foodGroupToggleBtn.getStyleClass().contains("segment-btn-active")) {
                foodGroupToggleBtn.getStyleClass().add("segment-btn-active");
            }

            donorToggleBtn.getStyleClass().remove("segment-btn-active");
            if (!donorToggleBtn.getStyleClass().contains("segment-btn-inactive")) {
                donorToggleBtn.getStyleClass().add("segment-btn-inactive");
            }
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        // Save form selections to RegistrationData singleton
        RegistrationData data = RegistrationData.getInstance();
        data.setRole(selectedRole);
        data.setOrgName(orgNameField != null ? orgNameField.getText() : "");
        data.setOrgType(typeCombo != null && typeCombo.getValue() != null ? typeCombo.getValue() : "Bakery");
        data.setCity(cityCombo != null && cityCombo.getValue() != null ? cityCombo.getValue() : "Manila");
        data.setContactPerson(contactPersonField != null ? contactPersonField.getText() : "");
        data.setContactNumber(contactNumberField != null ? contactNumberField.getText() : "");
        data.setEmail(emailField != null ? emailField.getText() : "");

        try {
            // Updated resource path pointing directly to /view/register_step2.fxml
            URL fxmlLocation = getClass().getResource("/com/aniplato/view/register_step2.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ ERROR: Cannot locate /com/aniplato/view/register_step2.fxml on classpath.");
                return;
            }

            Parent step2Root = FXMLLoader.load(fxmlLocation);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(step2Root);

        } catch (IOException e) {
            System.err.println("❌ Failed to navigate to Step 2 (register_step2.fxml):");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) {
        try {
            URL loginLocation = getClass().getResource("/com/aniplato/view/shared/login.fxml");
            if (loginLocation != null) {
                Parent loginRoot = FXMLLoader.load(loginLocation);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(loginRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}