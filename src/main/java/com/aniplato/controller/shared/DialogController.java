package com.aniplato.controller.shared;

import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.AuthenticationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.SvgIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class DialogController {

    // 1. REJECT ORG CONTAINER & FIELDS
    @FXML private VBox rejectDialogBox;
    @FXML private Label rejectDescriptionLabel;
    @FXML private TextArea rejectReasonTextArea;

    // 2. APPROVE ORG CONTAINER & FIELDS
    @FXML private VBox approveDialogBox;
    @FXML private Label approveDescriptionLabel;

    // 3. RESOLVE SAFETY NOTICE CONTAINER & FIELDS
    @FXML private VBox resolveSafetyDialogBox;
    @FXML private Label safetyNoticeDescriptionLabel;
    @FXML private TextArea safetyAdminNoteTextArea;

    // 4. SUSPEND ACCT CONTAINER & FIELDS
    @FXML private VBox suspendAccountDialogBox;
    @FXML private Label suspendDescriptionLabel;
    @FXML private TextArea suspendReasonTextArea;

    // 5. REINSTATE ACCT CONTAINER & FIELDS
    @FXML private VBox reinstateAccountDialogBox;
    @FXML private Label reinstateDescriptionLabel;

    // 6. CANCEL LISTING CONTAINER & FIELDS
    @FXML private VBox cancelListingDialogBox;
    @FXML private Label cancelListingDescriptionLabel;
    @FXML private TextArea cancelListingReasonTextArea;

    // 7. CONFIRM RELEASE CONTAINER & FIELDS
    @FXML private VBox confirmReleaseDialogBox;
    @FXML private Label releaseFoodLabel;
    @FXML private Label releaseQuantityLabel;
    @FXML private Label releaseFoodGroupLabel;
    @FXML private Label releasePickupLabel;
    @FXML private Label releaseContactLabel;
    @FXML private CheckBox releaseSealedCheck;
    @FXML private CheckBox releaseStorageCheck;
    @FXML private Label releaseNoticeLabel;

    // 8. FLAG SAFETY CONCERN CONTAINER & FIELDS
    @FXML private VBox flagSafetyDialogBox;
    @FXML private TextArea flagSafetyConcernTextArea;

    // 9. ACCEPT OFFER CONTAINER & FIELDS
    @FXML private VBox acceptOfferDialogBox;
    @FXML private Label acceptOfferDescriptionLabel;

    // 10. UPLOAD RENEWAL CONTAINER & FIELDS
    @FXML private VBox uploadRenewalDialogBox;
    @FXML private Label uploadRenewalTitleLabel;
    @FXML private Label selectedRenewalFileLabel;
    @FXML private TextField newExpiryDateField;

    // 11. SET NEW PASSWORD CONTAINER & FIELDS
    @FXML private VBox setNewPasswordDialogBox;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    private final AuthenticationService authenticationService = new AuthenticationService();
    private Runnable afterPasswordSet;  // what to do once the new password is saved

    // What the admin dialogs do on confirm. A callback may throw RuleViolationException: the message is shown
    // and the dialog stays open (A8). A dialog opened without a callback keeps its old behaviour.
    private Runnable onApprove;
    private Consumer<String> onReject;           // gets the reason typed in the dialog
    private Consumer<String> onSuspend;          // gets the reason typed in the dialog
    private Runnable onReinstate;
    private Consumer<String> onDismissNotice;    // gets the admin note
    private Consumer<String> onSuspendFromNotice;

    // 12. WITHDRAW OFFER CONTAINER & FIELDS
    @FXML private VBox withdrawOfferDialogBox;
    @FXML private Label withdrawOfferDescriptionLabel;

    // 13. CONFIRM RECEIPT CONTAINER & FIELDS
    @FXML private VBox confirmReceiptDialogBox;
    @FXML private Label receiptFoodLabel;
    @FXML private Label receiptQuantityLabel;
    @FXML private Label receiptDonorLabel;
    @FXML private Label receiptPickupLabel;
    @FXML private Label receiptContactLabel;
    @FXML private CheckBox receiptQuantityCheck;
    @FXML private CheckBox receiptConditionCheck;
    @FXML private Label receiptNoticeLabel;

    // 14. FORGOT PASSWORD CONTAINER & FIELDS
    @FXML private VBox forgotPasswordDialogBox;
    @FXML private TextField forgotPasswordEmailField;

    // 15. UPGRADE TO BUSINESS DONOR CONTAINER & FIELDS
    @FXML private VBox upgradeBusinessDonorDialogBox;
    @FXML private Label selectedMayorPermitFileLabel;
    @FXML private TextField mayorPermitExpiryDateField;
    @FXML private Label selectedSanitaryPermitFileLabel;
    @FXML private TextField sanitaryPermitExpiryDateField;

    @FXML
    private void initialize() {
        hideAllDialogs();

        // Populate all green confirmation badge hosts with Check_Button.svg
        if (rejectDialogBox != null && rejectDialogBox.getParent() != null) {
            rejectDialogBox.getParent().lookupAll(".modal-icon-green-box").forEach(node -> {
                if (node instanceof StackPane) {
                    ((StackPane) node).getChildren().setAll(SvgIcon.load("/assets/Check_Button.svg", 24));
                }
            });

            // Populate all red rejection/cancellation badge hosts with Reject_Button.svg
            rejectDialogBox.getParent().lookupAll(".modal-icon-red-box").forEach(node -> {
                if (node instanceof StackPane) {
                    ((StackPane) node).getChildren().setAll(SvgIcon.load("/assets/Reject_Button.svg", 24));
                }
            });
        }
    }

    /** Reject with a real action: {@code onConfirm} gets the reason. */
    public void showRejectMode(String orgName, Consumer<String> onConfirm) {
        showRejectMode(orgName);
        onReject = onConfirm;
    }

    /** Approve with a real action. */
    public void showApproveMode(String orgName, String orgEmail, Runnable onConfirm) {
        showApproveMode(orgName, orgEmail);
        onApprove = onConfirm;
    }

    /** Suspend with a real action: {@code onConfirm} gets the reason. */
    public void showSuspendAccountMode(String orgName, Consumer<String> onConfirm) {
        showSuspendAccountMode(orgName);
        onSuspend = onConfirm;
    }

    /** Reinstate with a real action. */
    public void showReinstateAccountMode(String orgName, String details, Runnable onConfirm) {
        showReinstateAccountMode(orgName, details);
        onReinstate = onConfirm;
    }

    /** Resolve a safety notice: each button's callback gets the admin note. */
    public void showResolveSafetyNoticeMode(String raisedBy, String date, String concern, String flaggedOrg,
                                            Consumer<String> onDismiss, Consumer<String> onSuspendAccount) {
        showResolveSafetyNoticeMode(raisedBy, date, concern, flaggedOrg);
        onDismissNotice = onDismiss;
        onSuspendFromNotice = onSuspendAccount;
    }

    /** 1. Prepares dialog for Reject Org mode */
    public void showRejectMode(String orgName) {
        onReject = null;
        showDialog(rejectDialogBox);
        rejectDescriptionLabel.setText("We’ll email " + orgName + " your reason so they can fix their documents and register again. No account is created.");
        rejectReasonTextArea.clear();
    }

    /** 2. Prepares dialog for Approve Org mode */
    public void showApproveMode(String orgName, String orgEmail) {
        onApprove = null;
        showDialog(approveDialogBox);
        approveDescriptionLabel.setText("We’ll create " + orgName + "’s account and email temporary login details to " + orgEmail + ".");
    }

    /** 3. Prepares dialog for Resolve Safety Notice mode */
    public void showResolveSafetyNoticeMode(String raisedBy, String date, String concern, String flaggedOrg) {
        onDismissNotice = null;
        onSuspendFromNotice = null;
        showDialog(resolveSafetyDialogBox);
        safetyNoticeDescriptionLabel.setText("Flagged by " + raisedBy + " on " + date + ": \"" + concern + "\" Close it as dismissed, or suspend " + flaggedOrg + ".");
        safetyAdminNoteTextArea.clear();
    }

    /** 4. Prepares dialog for Suspend Acct mode */
    public void showSuspendAccountMode(String orgName) {
        onSuspend = null;
        showDialog(suspendAccountDialogBox);
        suspendDescriptionLabel.setText(orgName + " can still log in, upload renewals and finish handoffs it already accepted. It can’t post, re-post, offer or accept, and its listings are hidden from food groups, until you reinstate it.");
        suspendReasonTextArea.clear();
    }

    /** 5. Prepares dialog for Reinstate Acct mode */
    public void showReinstateAccountMode(String orgName, String details) {
        onReinstate = null;
        showDialog(reinstateAccountDialogBox);
        if (details != null && !details.isEmpty()) {
            reinstateDescriptionLabel.setText(orgName + " gets full access again. " + details);
        } else {
            reinstateDescriptionLabel.setText(orgName + " gets full access again.");
        }
    }

    /** 6. Prepares dialog for Cancel Listing mode (Donor) */
    public void showCancelListingMode(String listingTitle, String recipientName) {
        showDialog(cancelListingDialogBox);
        if (recipientName != null && !recipientName.isEmpty()) {
            cancelListingDescriptionLabel.setText(listingTitle + " leaves the board. " + recipientName + "’s accepted offer is cancelled with it, and they see your reason.");
        } else {
            cancelListingDescriptionLabel.setText(listingTitle + " leaves the board and its listing is cancelled.");
        }
        cancelListingReasonTextArea.clear();
    }

    /** 7. Prepares dialog for Confirm Release mode (Donor) */
    public void showConfirmReleaseMode(String food, String qty, String group, String pickup, String contact) {
        showDialog(confirmReleaseDialogBox);
        releaseFoodLabel.setText(food);
        releaseQuantityLabel.setText(qty);
        releaseFoodGroupLabel.setText(group);
        releasePickupLabel.setText(pickup);
        releaseContactLabel.setText(contact);
        releaseSealedCheck.setSelected(true);
        releaseStorageCheck.setSelected(true);
        releaseNoticeLabel.setText(group + " hasn’t confirmed receipt yet. The listing becomes Rescued once they do.");
    }

    /** 8. Prepares dialog for Flag Safety Concern mode */
    public void showFlagSafetyConcernMode() {
        showDialog(flagSafetyDialogBox);
        flagSafetyConcernTextArea.clear();
    }

    /** 9. Prepares dialog for Accept Offer mode (Donor) */
    public void showAcceptOfferMode(String groupName, String quantity, String pickupTime, int otherOffersCount) {
        showDialog(acceptOfferDialogBox);

        String otherOffersText = (otherOffersCount > 0)
                ? " The other " + otherOffersCount + " " + (otherOffersCount == 1 ? "offer is" : "offers are") + " declined automatically."
                : "";

        acceptOfferDescriptionLabel.setText(
                groupName + " picks up the full " + quantity + " " + pickupTime + "." + otherOffersText +
                        " Your contact number and handover instructions are shared with " + groupName + ", and you’ll see theirs."
        );
    }

    /** 10. Prepares dialog for Upload Renewal mode */
    public void showUploadRenewalMode(String documentName) {
        showDialog(uploadRenewalDialogBox);
        uploadRenewalTitleLabel.setText("Upload renewal: " + documentName);
        selectedRenewalFileLabel.setText("No file chosen");
        newExpiryDateField.clear();
    }

    /** 11. Prepares dialog for Set New Password mode */
    public void showSetNewPasswordMode(String currentPassword, Runnable onDone) {
        this.afterPasswordSet = onDone;
        showDialog(setNewPasswordDialogBox);
        currentPasswordField.setText(currentPassword == null ? "" : currentPassword);
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    /** 12. Prepares dialog for Withdraw Offer mode (Food Group) */
    public void showWithdrawOfferMode(String donorName) {
        showDialog(withdrawOfferDialogBox);
        withdrawOfferDescriptionLabel.setText(donorName + " won’t see this offer anymore. You can send a new one while the listing is still open.");
    }

    /** 13. Prepares dialog for Confirm Receipt mode (Food Group) */
    public void showConfirmReceiptMode(String food, String qty, String donor, String pickup, String contact, String releaseTime) {
        showDialog(confirmReceiptDialogBox);
        receiptFoodLabel.setText(food);
        receiptQuantityLabel.setText(qty);
        receiptDonorLabel.setText(donor);
        receiptPickupLabel.setText(pickup);
        receiptContactLabel.setText(contact);
        receiptQuantityCheck.setSelected(true);
        receiptConditionCheck.setSelected(true);

        if (releaseTime != null && !releaseTime.isEmpty()) {
            receiptNoticeLabel.setText(donor + " confirmed release at " + releaseTime + ". Confirming receipt marks this listing Rescued.");
        } else {
            receiptNoticeLabel.setText("Confirming receipt marks this listing Rescued.");
        }
    }

    /** 14. Prepares dialog for Forgot Password mode */
    public void showForgotPasswordMode(String email) {
        showDialog(forgotPasswordDialogBox);
        if (forgotPasswordEmailField != null) {
            forgotPasswordEmailField.setText(email != null ? email : "");
        }
    }

    /** 15. Prepares dialog for Upgrade to Business Donor mode */
    public void showUpgradeBusinessDonorMode() {
        showDialog(upgradeBusinessDonorDialogBox);
        if (selectedMayorPermitFileLabel != null) selectedMayorPermitFileLabel.setText("No file chosen");
        if (mayorPermitExpiryDateField != null) mayorPermitExpiryDateField.clear();
        if (selectedSanitaryPermitFileLabel != null) selectedSanitaryPermitFileLabel.setText("No file chosen");
        if (sanitaryPermitExpiryDateField != null) sanitaryPermitExpiryDateField.clear();
    }

    @FXML
    private void handleSwitchToFlagSafety() {
        showFlagSafetyConcernMode();
    }

    @FXML
    private void handleChooseRenewalFile() {
        // TODO(file-picker): Open FileChooser and update selectedRenewalFileLabel
    }

    @FXML
    private void handleChooseMayorPermitFile() {
        // TODO(file-picker): Open FileChooser and update selectedMayorPermitFileLabel
        if (selectedMayorPermitFileLabel != null) {
            selectedMayorPermitFileLabel.setText("mayors-permit-2026.pdf");
        }
    }

    @FXML
    private void handleChooseSanitaryPermitFile() {
        // TODO(file-picker): Open FileChooser and update selectedSanitaryPermitFileLabel
        if (selectedSanitaryPermitFileLabel != null) {
            selectedSanitaryPermitFileLabel.setText("sanitary-permit-2026.pdf");
        }
    }

    // PATTERN:ACTION cancels dialog overlay
    @FXML
    private void handleCancel() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms rejection
    @FXML
    private void handleConfirmReject() {
        if (onReject != null) {
            confirm(() -> onReject.accept(rejectReasonTextArea.getText()));
            return;
        }
        closeOverlay();
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }

    // PATTERN:ACTION confirms approval
    @FXML
    private void handleConfirmApprove() {
        if (onApprove != null) {
            confirm(onApprove);
            return;
        }
        closeOverlay();
        Navigator.show(Screen.PENDING_REGISTRATIONS);
    }

    // PATTERN:ACTION dismisses safety notice
    @FXML
    private void handleDismissSafetyNotice() {
        if (onDismissNotice != null) {
            confirm(() -> onDismissNotice.accept(safetyAdminNoteTextArea.getText()));
            return;
        }
        closeOverlay();
    }

    // PATTERN:ACTION suspends account from safety notice
    @FXML
    private void handleSuspendFromSafetyNotice() {
        if (onSuspendFromNotice != null) {
            confirm(() -> onSuspendFromNotice.accept(safetyAdminNoteTextArea.getText()));
            return;
        }
        closeOverlay();
    }

    // PATTERN:ACTION confirms account suspension
    @FXML
    private void handleConfirmSuspend() {
        if (onSuspend != null) {
            confirm(() -> onSuspend.accept(suspendReasonTextArea.getText()));
            return;
        }
        closeOverlay();
    }

    // PATTERN:ACTION confirms account reinstatement
    @FXML
    private void handleConfirmReinstate() {
        if (onReinstate != null) {
            confirm(onReinstate);
            return;
        }
        closeOverlay();
    }

    // PATTERN:ACTION confirms listing cancellation
    @FXML
    private void handleConfirmCancelListing() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms food release
    @FXML
    private void handleConfirmRelease() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms flagging safety concern
    @FXML
    private void handleConfirmFlagSafety() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms accepting offer
    @FXML
    private void handleConfirmAcceptOffer() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms uploading renewal document
    @FXML
    private void handleConfirmUploadRenewal() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms setting new password
    @FXML
    private void handleConfirmSetNewPassword() {
        try {
            authenticationService.changePassword(currentPasswordField.getText(), newPasswordField.getText(), confirmPasswordField.getText());
        } catch (RuleViolationException e) {
            showError(e.getMessage());   // A8: stay open until the password is accepted
            return;
        }
        closeOverlay();
        if (afterPasswordSet != null) afterPasswordSet.run();
    }

    // PATTERN:ACTION confirms withdrawing offer
    @FXML
    private void handleConfirmWithdrawOffer() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms receiving food
    @FXML
    private void handleConfirmReceipt() {
        closeOverlay();
    }

    // PATTERN:ACTION confirms forgot password request
    @FXML
    private void handleConfirmForgotPassword() {
        String email = forgotPasswordEmailField != null ? forgotPasswordEmailField.getText().trim() : "";
        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }
        try {
            authenticationService.requestPasswordReset(email);
        } catch (RuleViolationException e) {
            showError(e.getMessage());   // A8
            return;
        }
        closeOverlay();
        // same answer either way, so the dialog does not reveal which emails have accounts
        new Alert(Alert.AlertType.INFORMATION, "If that email has an account, a temporary password is on its way.").showAndWait();
    }

    // PATTERN:ACTION confirms upgrading to business donor
    @FXML
    private void handleConfirmUpgradeBusinessDonor() {
        closeOverlay();
    }

    private void showDialog(VBox dialogBox) {
        hideAllDialogs();
        if (dialogBox != null) {
            dialogBox.setVisible(true);
            dialogBox.setManaged(true);
            if (dialogBox.getParent() != null) {
                dialogBox.getParent().setVisible(true);
                dialogBox.getParent().setManaged(true);
            }
        }
    }

    private void hideAllDialogs() {
        if (rejectDialogBox != null) { rejectDialogBox.setVisible(false); rejectDialogBox.setManaged(false); }
        if (approveDialogBox != null) { approveDialogBox.setVisible(false); approveDialogBox.setManaged(false); }
        if (resolveSafetyDialogBox != null) { resolveSafetyDialogBox.setVisible(false); resolveSafetyDialogBox.setManaged(false); }
        if (suspendAccountDialogBox != null) { suspendAccountDialogBox.setVisible(false); suspendAccountDialogBox.setManaged(false); }
        if (reinstateAccountDialogBox != null) { reinstateAccountDialogBox.setVisible(false); reinstateAccountDialogBox.setManaged(false); }
        if (cancelListingDialogBox != null) { cancelListingDialogBox.setVisible(false); cancelListingDialogBox.setManaged(false); }
        if (confirmReleaseDialogBox != null) { confirmReleaseDialogBox.setVisible(false); confirmReleaseDialogBox.setManaged(false); }
        if (flagSafetyDialogBox != null) { flagSafetyDialogBox.setVisible(false); flagSafetyDialogBox.setManaged(false); }
        if (acceptOfferDialogBox != null) { acceptOfferDialogBox.setVisible(false); acceptOfferDialogBox.setManaged(false); }
        if (uploadRenewalDialogBox != null) { uploadRenewalDialogBox.setVisible(false); uploadRenewalDialogBox.setManaged(false); }
        if (setNewPasswordDialogBox != null) { setNewPasswordDialogBox.setVisible(false); setNewPasswordDialogBox.setManaged(false); }
        if (withdrawOfferDialogBox != null) { withdrawOfferDialogBox.setVisible(false); withdrawOfferDialogBox.setManaged(false); }
        if (confirmReceiptDialogBox != null) { confirmReceiptDialogBox.setVisible(false); confirmReceiptDialogBox.setManaged(false); }
        if (forgotPasswordDialogBox != null) { forgotPasswordDialogBox.setVisible(false); forgotPasswordDialogBox.setManaged(false); }
        if (upgradeBusinessDonorDialogBox != null) { upgradeBusinessDonorDialogBox.setVisible(false); upgradeBusinessDonorDialogBox.setManaged(false); }
    }

    /** Runs a confirm action. A broken rule is shown and the dialog stays open; success closes it (A8). */
    private void confirm(Runnable action) {
        try {
            action.run();
        } catch (RuleViolationException e) {
            showError(e.getMessage());
            return;
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            showError("Could not save this. Check the database connection and try again.");
            return;
        }
        closeOverlay();
    }

    private void closeOverlay() {
        hideAllDialogs();
        if (rejectDialogBox != null && rejectDialogBox.getParent() != null) {
            rejectDialogBox.getParent().setVisible(false);
            rejectDialogBox.getParent().setManaged(false);
        }
    }

    /** A8: shows a RuleViolationException message. Built in code because dialog.fxml has no error label. */
    public static void showError(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }
}