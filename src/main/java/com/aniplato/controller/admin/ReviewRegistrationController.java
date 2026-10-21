package com.aniplato.controller.admin;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ReviewRegistrationController {

    // Nested Dialog Controller and Overlay Container (Injected from <fx:include fx:id="dialog" ... />)
    @FXML private DialogController dialogController;
    @FXML private VBox dialog;

    // Header & Form Field Labels
    @FXML private Label pageTitleLabel;
    @FXML private Label orgTitleLabel;
    @FXML private Label statusPillLabel;

    @FXML private Label nameHeaderLabel; // "ORGANIZATION NAME", "BUSINESS NAME", or "NAME OR HOUSEHOLD NAME"
    @FXML private Label orgNameLabel;
    @FXML private Label rolePillLabel;
    @FXML private Label contactPersonLabel;
    @FXML private Label emailLabel;

    // Organization Type Container (Hidden for Casual Donors)
    @FXML private VBox orgTypeBox;
    @FXML private Label orgTypeLabel;

    // City Container (Shifted to Left Column when orgTypeBox is hidden)
    @FXML private VBox cityBox;
    @FXML private Label cityLabel;

    // Document Section Labels
    @FXML private Label documentsCountHeaderLabel; // "DOCUMENTS (2)" or "DOCUMENTS (1)"

    // Document 1 Labels
    @FXML private Label doc1TitleLabel;
    @FXML private Label doc1MetaLabel;
    @FXML private Label doc1FileLabel;

    // Document 2 Container & Labels (Hidden for Casual Donors)
    @FXML private VBox doc2Box;
    @FXML private Label doc2TitleLabel;
    @FXML private Label doc2MetaLabel;
    @FXML private Label doc2FileLabel;

    @FXML private Label registeredDateLabel;

    // TODO(service): set from the row picked on Pending Registrations when the screen loads real data
    private int registrationId;

    @FXML
    private void initialize() {
        // You can read the selected account mode/type from your Navigator or session state
        // Defaulting to "CASUAL_DONOR" to match the exact Santos Household view
        loadReviewDetails("CASUAL_DONOR");
    }

    /**
     * Updates all labels, document cards, and grid positions according to the account type being reviewed
     */
    public void loadReviewDetails(String accountType) {
        switch (accountType) {
            case "CASUAL_DONOR": // Santos Household
                if (pageTitleLabel != null) pageTitleLabel.setText("Review Registration");
                if (orgTitleLabel != null) orgTitleLabel.setText("Santos Household");
                if (nameHeaderLabel != null) nameHeaderLabel.setText("NAME OR HOUSEHOLD NAME");
                if (orgNameLabel != null) orgNameLabel.setText("Santos Household");

                if (rolePillLabel != null) {
                    rolePillLabel.setText("Casual donor");
                    rolePillLabel.getStyleClass().setAll("pill-food-donor");
                }

                if (contactPersonLabel != null) contactPersonLabel.setText("Maria Santos · 0917 123 4567");
                if (emailLabel != null) emailLabel.setText("maria.santos@email.com");
                if (cityLabel != null) cityLabel.setText("Pasig City");

                // Casual Donor has NO Organization Type row -> Hide Box
                if (orgTypeBox != null) {
                    orgTypeBox.setVisible(false);
                    orgTypeBox.setManaged(false);
                }

                // Shift CITY to Row 2, Column 0 (Left Side)
                if (cityBox != null) {
                    GridPane.setRowIndex(cityBox, 2);
                    GridPane.setColumnIndex(cityBox, 0);
                }

                // Documents (1) - Barangay Certificate
                if (documentsCountHeaderLabel != null) documentsCountHeaderLabel.setText("DOCUMENTS (1)");
                if (doc1TitleLabel != null) doc1TitleLabel.setText("Barangay Certificate");
                if (doc1MetaLabel != null) doc1MetaLabel.setText("Uploaded Oct 1, 2026 · Expires Apr 19, 2027 (entered by applicant)");
                if (doc1FileLabel != null) doc1FileLabel.setText("barangay-certificate.pdf");

                // Hide second document card
                if (doc2Box != null) {
                    doc2Box.setVisible(false);
                    doc2Box.setManaged(false);
                }
                break;

            case "BUSINESS_DONOR": // Manila Artisanal Bakeshop
                if (pageTitleLabel != null) pageTitleLabel.setText("Review Registration");
                if (orgTitleLabel != null) orgTitleLabel.setText("Manila Artisanal Bakeshop");
                if (nameHeaderLabel != null) nameHeaderLabel.setText("BUSINESS NAME");
                if (orgNameLabel != null) orgNameLabel.setText("Manila Artisanal Bakeshop");

                if (rolePillLabel != null) {
                    rolePillLabel.setText("Business donor");
                    rolePillLabel.getStyleClass().setAll("pill-food-donor");
                }

                if (contactPersonLabel != null) contactPersonLabel.setText("Ramon A. Dizon · 0917 555 0142");
                if (emailLabel != null) emailLabel.setText("hello@manilaartisanal.ph");
                if (cityLabel != null) cityLabel.setText("Mandaluyong City");

                if (orgTypeBox != null) {
                    orgTypeBox.setVisible(true);
                    orgTypeBox.setManaged(true);
                    if (orgTypeLabel != null) orgTypeLabel.setText("Bakery");
                }

                // Restore CITY to Row 2, Column 1 (Right Side)
                if (cityBox != null) {
                    GridPane.setRowIndex(cityBox, 2);
                    GridPane.setColumnIndex(cityBox, 1);
                }

                if (documentsCountHeaderLabel != null) documentsCountHeaderLabel.setText("DOCUMENTS (2)");
                if (doc1TitleLabel != null) doc1TitleLabel.setText("Mayor's / Business Permit");
                if (doc1MetaLabel != null) doc1MetaLabel.setText("Uploaded Oct 1, 2026 · Expires Dec 31, 2026 (entered by applicant)");
                if (doc1FileLabel != null) doc1FileLabel.setText("mayors-permit-2026.pdf");

                if (doc2Box != null) {
                    doc2Box.setVisible(true);
                    doc2Box.setManaged(true);
                }
                if (doc2TitleLabel != null) doc2TitleLabel.setText("Sanitary Permit");
                if (doc2MetaLabel != null) doc2MetaLabel.setText("Uploaded Oct 1, 2026 · Expires Dec 31, 2026 (entered by applicant)");
                if (doc2FileLabel != null) doc2FileLabel.setText("sanitary-permit-2026.pdf");
                break;

            case "FOOD_GROUP": // San Antonio Community Pantry
            default:
                if (pageTitleLabel != null) pageTitleLabel.setText("Review Registration");
                if (orgTitleLabel != null) orgTitleLabel.setText("San Antonio Community Pantry");
                if (nameHeaderLabel != null) nameHeaderLabel.setText("ORGANIZATION NAME");
                if (orgNameLabel != null) orgNameLabel.setText("San Antonio Community Pantry");

                if (rolePillLabel != null) {
                    rolePillLabel.setText("Food group");
                    rolePillLabel.getStyleClass().setAll("pill-food-org");
                }

                if (contactPersonLabel != null) contactPersonLabel.setText("Elena R. Santos · 0918 234 5671");
                if (emailLabel != null) emailLabel.setText("contact@sanantoniopantry.ph");
                if (cityLabel != null) cityLabel.setText("Pasig City");

                if (orgTypeBox != null) {
                    orgTypeBox.setVisible(true);
                    orgTypeBox.setManaged(true);
                    if (orgTypeLabel != null) orgTypeLabel.setText("Charity");
                }

                // Restore CITY to Row 2, Column 1 (Right Side)
                if (cityBox != null) {
                    GridPane.setRowIndex(cityBox, 2);
                    GridPane.setColumnIndex(cityBox, 1);
                }

                if (documentsCountHeaderLabel != null) documentsCountHeaderLabel.setText("DOCUMENTS (2)");
                if (doc1TitleLabel != null) doc1TitleLabel.setText("DSWD Certificate of Registration");
                if (doc1MetaLabel != null) doc1MetaLabel.setText("Uploaded Oct 1, 2026 · Expires Jun 30, 2027 (entered by applicant)");
                if (doc1FileLabel != null) doc1FileLabel.setText("dswd-registration.pdf");

                if (doc2Box != null) {
                    doc2Box.setVisible(true);
                    doc2Box.setManaged(true);
                }
                if (doc2TitleLabel != null) doc2TitleLabel.setText("Authorization letter");
                if (doc2MetaLabel != null) doc2MetaLabel.setText("Uploaded Oct 1, 2026 · No expiry (marked by applicant) · Must name Elena R. Santos");
                if (doc2FileLabel != null) doc2FileLabel.setText("authorization-letter.pdf");
                break;
        }
    }

    @FXML
    private void handleVerifyDoc1() {
        // TODO(service): mark document 1 as verified in service
    }

    @FXML
    private void handleFlagDoc1() {
        if (dialogController != null) {
            dialogController.showFlagSafetyConcernMode();
        }
    }

    @FXML
    private void handleVerifyDoc2() {
        // TODO(service): mark document 2 as verified in service
    }

    @FXML
    private void handleFlagDoc2() {
        if (dialogController != null) {
            dialogController.showFlagSafetyConcernMode();
        }
    }

    @FXML
    private void handleBack() {
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }

    @FXML
    private void handleReject() {
        if (dialogController != null) {
            String name = (orgNameLabel != null && !orgNameLabel.getText().isEmpty())
                    ? orgNameLabel.getText()
                    : "Santos Household";
            dialogController.showRejectMode(registrationId, name);
        }
    }

    @FXML
    private void handleApprove() {
        if (dialogController != null) {
            String name = (orgNameLabel != null && !orgNameLabel.getText().isEmpty())
                    ? orgNameLabel.getText()
                    : "Santos Household";
            String email = (emailLabel != null && !emailLabel.getText().isEmpty())
                    ? emailLabel.getText()
                    : "maria.santos@email.com";

            dialogController.showApproveMode(registrationId, name, email);
        }
    }
}