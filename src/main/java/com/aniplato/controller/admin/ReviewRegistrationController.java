package com.aniplato.controller.admin;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.Inline;
import com.aniplato.model.Document;
import com.aniplato.model.Registration;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.FileStorageService;
import com.aniplato.service.RegistrationService.ReviewDetail;
import com.aniplato.util.Selection;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.SvgIcon;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;

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

    // Organization Type Container (no organization type is collected, so it stays hidden)
    @FXML private VBox orgTypeBox;
    @FXML private Label orgTypeLabel;

    // City Container (sits in the left column because the organization type is hidden)
    @FXML private VBox cityBox;
    @FXML private Label cityLabel;

    // Document Section Labels
    @FXML private Label documentsCountHeaderLabel;

    // Document 1
    @FXML private Label doc1TitleLabel;
    @FXML private Label doc1MetaLabel;
    @FXML private Label doc1FileLabel;
    @FXML private VBox doc1Checks;
    @FXML private Button doc1VerifyBtn;
    @FXML private Label doc1VerifiedBadge;
    @FXML private Hyperlink doc1UndoLink;

    // Document 2 (only registrations with two documents)
    @FXML private VBox doc2Box;
    @FXML private Label doc2TitleLabel;
    @FXML private Label doc2MetaLabel;
    @FXML private Label doc2FileLabel;
    @FXML private VBox doc2Checks;
    @FXML private Button doc2VerifyBtn;
    @FXML private Label doc2VerifiedBadge;
    @FXML private Hyperlink doc2UndoLink;

    @FXML private Label registeredDateLabel;

    // FXML: errorLabel (style class "error-text") shows why Approve is not possible yet (A8)
    @FXML private Label errorLabel;
    @FXML private Button rejectBtn;
    @FXML private Button approveBtn;

    private final RegistrationService registrationService = new RegistrationService();
    private int registrationId;
    private List<Document> documents = List.of();

    @FXML
    private void initialize() {
        registrationId = Selection.registrationId();
        orgTypeBox.setVisible(false);
        orgTypeBox.setManaged(false);
        GridPane.setRowIndex(cityBox, 2);
        GridPane.setColumnIndex(cityBox, 0);
        approveBtn.setGraphic(SvgIcon.load("/assets/CIrcle_Check_Button.svg", 15));
        rejectBtn.setGraphic(SvgIcon.load("/assets/Circle_Reject_Button.svg", 15));
        load();
    }

    /** Reads the registration and its documents from the service and fills the screen. */
    private void load() {
        Inline.hide(errorLabel);
        ReviewDetail[] read = new ReviewDetail[1];
        String problem = FormSupport.failure(() -> read[0] = registrationService.getForReview(registrationId),
                "Could not load the registration. Check the database connection.");
        if (problem != null) {
            orgTitleLabel.setText(problem);
            return;
        }
        ReviewDetail detail = read[0];
        Registration r = detail.registration();
        documents = r.documents;

        pageTitleLabel.setText("Review Registration");
        orgTitleLabel.setText(r.name);
        orgNameLabel.setText(r.name);
        nameHeaderLabel.setText(switch (r.role) {
            case BUSINESS_DONOR -> "BUSINESS NAME";
            case CASUAL_DONOR -> "NAME OR HOUSEHOLD NAME";
            default -> "ORGANIZATION NAME";
        });
        rolePillLabel.setText(AdminTable.roleName(r.role));
        rolePillLabel.getStyleClass().setAll(AdminTable.roleStyle(r.role));
        contactPersonLabel.setText(r.contactPerson + " · " + r.contactPhone);   // the admin may see the number (spec 6.2)
        emailLabel.setText(r.email);
        cityLabel.setText(r.city.name);
        statusPillLabel.setText(r.status == Registration.Status.PENDING
                ? (detail.registeredAgain() ? "🟠 PENDING · REGISTERED AGAIN" : "🟠 PENDING") : r.status.name());
        registeredDateLabel.setText("📅 Registered " + AdminTable.date(r.submittedAt));

        documentsCountHeaderLabel.setText("DOCUMENTS (" + documents.size() + ")");
        showDocument(0, doc1TitleLabel, doc1MetaLabel, doc1FileLabel, doc1Checks, doc1VerifyBtn, doc1VerifiedBadge, doc1UndoLink);
        boolean second = documents.size() > 1;
        doc2Box.setVisible(second);
        doc2Box.setManaged(second);
        if (second) showDocument(1, doc2TitleLabel, doc2MetaLabel, doc2FileLabel, doc2Checks, doc2VerifyBtn, doc2VerifiedBadge, doc2UndoLink);
    }

    private void showDocument(int index, Label title, Label meta, Label file, VBox checks, Button verify,
                              Label verifiedBadge, Hyperlink undo) {
        if (index >= documents.size()) return;
        Document d = documents.get(index);
        title.setText(d.type.label());
        String expiry = d.expiresOn == null ? "No expiry (marked by applicant)"
                : "Expires " + d.expiresOn.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")) + " (entered by applicant)";
        meta.setText("Uploaded " + AdminTable.date(d.uploadedAt) + " · " + expiry
                + (d.review == Document.Review.FLAGGED ? " · Flagged: " + d.flagReason : ""));
        file.setText(new File(d.filePath).getName());

        boolean verified = d.review == Document.Review.OK;
        // the third reminder names the applicant's city
        if (checks.getChildren().size() > 2 && checks.getChildren().get(2) instanceof Label cityNote) {
            cityNote.setText("• Issued for " + cityLabel.getText());
        }
        // Verify until the document is verified; then a "Verified" tag with an Undo link (as in the design)
        show(verify, !verified);
        show(verifiedBadge, verified);
        show(undo, verified);
    }

    private static void show(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    @FXML private void handleVerifyDoc1() { verify(0); }
    @FXML private void handleVerifyDoc2() { verify(1); }
    @FXML private void handleUndoDoc1() { run(() -> registrationService.undoVerification(documents.get(0).id)); }
    @FXML private void handleUndoDoc2() { run(() -> registrationService.undoVerification(documents.get(1).id)); }
    @FXML private void handleViewDoc1() { open(0); }
    @FXML private void handleViewDoc2() { open(1); }

    /** R1.4: the admin opens the file, compares it with the reminders, then verifies it. */
    private void verify(int index) {
        run(() -> registrationService.verifyDocument(documents.get(index).id));
    }

    /** Runs a service action, shows a broken rule (A8), then re-reads the registration so the screen matches the database. */
    private void run(Runnable action) {
        String problem = FormSupport.failure(action, "Could not save this. Check the database connection and try again.");
        if (problem != null) {
            Inline.error(problem);
            return;
        }
        load();
    }

    /** Opens the uploaded file in the system's viewer. */
    private void open(int index) {
        if (index >= documents.size()) return;
        File file = new FileStorageService().resolve(documents.get(index).filePath);
        try {
            if (!file.isFile() || !Desktop.isDesktopSupported()) {
                Inline.error("The file " + file.getName() + " is not in the uploads folder.");
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            Inline.error("Could not open " + file.getName() + ".");
        }
    }

    @FXML
    private void handleBack() {
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }

    @FXML
    private void handleReject() {
        dialogController.showRejectMode(registrationId, orgNameLabel.getText());   // the dialog rejects and emails
    }

    @FXML
    private void handleApprove() {
        if (documents.stream().anyMatch(d -> d.review != Document.Review.OK)) {
            Inline.show(errorLabel, "Verify every document before approving.");   // A8; the service checks this too
            return;
        }
        Inline.hide(errorLabel);
        dialogController.showApproveMode(registrationId, orgNameLabel.getText(), emailLabel.getText());   // the dialog approves and emails
    }
}
