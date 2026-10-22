package com.aniplato.controller.shared;

import com.aniplato.controller.shared.dialog.AcceptOfferDialogController;
import com.aniplato.controller.shared.dialog.ApproveDialogController;
import com.aniplato.controller.shared.dialog.CancelListingDialogController;
import com.aniplato.controller.shared.dialog.ConfirmReceiptDialogController;
import com.aniplato.controller.shared.dialog.ConfirmReleaseDialogController;
import com.aniplato.controller.shared.dialog.DialogCard;
import com.aniplato.controller.shared.dialog.EditContactDialogController;
import com.aniplato.model.Document;
import com.aniplato.controller.shared.dialog.DialogHost;
import com.aniplato.controller.shared.dialog.FlagSafetyDialogController;
import com.aniplato.controller.shared.dialog.ForgotPasswordDialogController;
import com.aniplato.controller.shared.dialog.NoticeResolvedDialogController;
import com.aniplato.controller.shared.dialog.ReinstateAccountDialogController;
import com.aniplato.controller.shared.dialog.RejectDialogController;
import com.aniplato.controller.shared.dialog.ResolveSafetyDialogController;
import com.aniplato.controller.shared.dialog.SetNewPasswordDialogController;
import com.aniplato.controller.shared.dialog.SuspendAccountDialogController;
import com.aniplato.controller.shared.dialog.UpgradeBusinessDonorDialogController;
import com.aniplato.controller.shared.dialog.UploadRenewalDialogController;
import com.aniplato.controller.shared.dialog.WithdrawOfferDialogController;
import com.aniplato.util.SvgIcon;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * The popup overlay every screen includes. Each dialog is its own FXML (view/shared/dialog/) and its own
 * controller (controller/shared/dialog/); this class only opens and closes them, so a screen calls
 * {@code dialogController.showXxxMode(...)} (A: all popups go through DialogController). To add a dialog:
 * a new card FXML and DialogCard subclass, one include in dialog.fxml, and a show method here.
 */
public class DialogController implements DialogHost {

    @FXML private VBox overlay;

    @FXML private RejectDialogController rejectController;
    @FXML private ApproveDialogController approveController;
    @FXML private ResolveSafetyDialogController resolveSafetyController;
    @FXML private SuspendAccountDialogController suspendController;
    @FXML private ReinstateAccountDialogController reinstateController;
    @FXML private CancelListingDialogController cancelListingController;
    @FXML private ConfirmReleaseDialogController confirmReleaseController;
    @FXML private FlagSafetyDialogController flagSafetyController;
    private int flagOfferId;   // the offer the Release and Receipt dialogs' "Flag a concern" link is about
    @FXML private AcceptOfferDialogController acceptOfferController;
    @FXML private UploadRenewalDialogController uploadRenewalController;
    @FXML private SetNewPasswordDialogController setNewPasswordController;
    @FXML private WithdrawOfferDialogController withdrawOfferController;
    @FXML private ConfirmReceiptDialogController confirmReceiptController;
    @FXML private ForgotPasswordDialogController forgotPasswordController;
    @FXML private UpgradeBusinessDonorDialogController upgradeBusinessDonorController;
    @FXML private NoticeResolvedDialogController noticeResolvedController;
    @FXML private EditContactDialogController editContactController;

    private List<DialogCard> cards;

    @FXML
    private void initialize() {
        cards = List.of(rejectController, approveController, resolveSafetyController, suspendController,
                reinstateController, cancelListingController, confirmReleaseController, flagSafetyController,
                acceptOfferController, uploadRenewalController, setNewPasswordController, withdrawOfferController,
                confirmReceiptController, forgotPasswordController, upgradeBusinessDonorController, noticeResolvedController, editContactController);
        cards.forEach(card -> card.attach(this));
        hideAll();

        // Populate all green confirmation badge hosts with Check_Button.svg
        overlay.lookupAll(".modal-icon-green-box").forEach(node -> {
            if (node instanceof StackPane) {
                ((StackPane) node).getChildren().setAll(SvgIcon.load("/assets/Check_Button.svg", 24));
            }
        });

        // Populate all red rejection/cancellation badge hosts with Reject_Button.svg
        overlay.lookupAll(".modal-icon-red-box").forEach(node -> {
            if (node instanceof StackPane) {
                ((StackPane) node).getChildren().setAll(SvgIcon.load("/assets/Reject_Button.svg", 24));
            }
        });
    }

    //---THE OVERLAY---

    @Override
    public void show(Node card) {
        hideAll();
        card.setVisible(true);
        card.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    @Override
    public void close() {
        hideAll();
        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    @Override
    public void showFlagSafetyConcern() {
        flagSafetyController.show(flagOfferId);
    }

    private void hideAll() {
        for (DialogCard card : cards) {
            card.card().setVisible(false);
            card.card().setManaged(false);
        }
    }

    /** A8: shows a RuleViolationException message. Built in code because the dialogs have no error label. */
    public static void showError(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    //---OPENING A DIALOG---

    /** 1. Reject Org */
    public void showRejectMode(int registrationId, String orgName) {
        rejectController.show(registrationId, orgName);
    }

    /** 2. Approve Org */
    public void showApproveMode(int registrationId, String orgName, String orgEmail) {
        approveController.show(registrationId, orgName, orgEmail);
    }

    /** 3. Resolve Safety Notice */
    public void showResolveSafetyNoticeMode(String raisedBy, String date, String concern, String flaggedOrg) {
        resolveSafetyController.show(raisedBy, date, concern, flaggedOrg);
    }

    /** 3. Resolve Safety Notice, with real actions: each button's callback gets the admin note. */
    public void showResolveSafetyNoticeMode(String raisedBy, String date, String concern, String flaggedOrg,
                                            Consumer<String> onDismiss, Consumer<String> onSuspendAccount) {
        resolveSafetyController.show(raisedBy, date, concern, flaggedOrg, onDismiss, onSuspendAccount);
    }

    /** 4. Suspend Acct */
    public void showSuspendAccountMode(String orgName) {
        suspendController.show(orgName);
    }

    /** 4. Suspend Acct, with a real action: {@code onConfirm} gets the reason. */
    public void showSuspendAccountMode(String orgName, Consumer<String> onConfirm) {
        suspendController.show(orgName, onConfirm);
    }

    /** 5. Reinstate Acct */
    public void showReinstateAccountMode(String orgName, String details) {
        reinstateController.show(orgName, details);
    }

    /** 5. Reinstate Acct, with a real action. */
    public void showReinstateAccountMode(String orgName, String details, Runnable onConfirm) {
        reinstateController.show(orgName, details, onConfirm);
    }

    /** 6. Cancel Listing (Donor) */
    public void showCancelListingMode(String listingTitle, String recipientName) {
        cancelListingController.show(listingTitle, recipientName);
    }

    /** 6. Cancel Listing or Request, with a real action: {@code onConfirm} gets the reason. */
    public void showCancelListingMode(String listingTitle, String recipientName, Consumer<String> onConfirm) {
        cancelListingController.show(listingTitle, recipientName, onConfirm);
    }

    /** 7. Confirm Release (Donor), with a real action. */
    public void showConfirmReleaseMode(String food, String qty, String group, String pickup, String contact, int offerId, Runnable onConfirm) {
        flagOfferId = offerId;
        confirmReleaseController.show(food, qty, group, pickup, contact, onConfirm);
    }

    /** 7. Confirm Release (Donor) */
    public void showConfirmReleaseMode(String food, String qty, String group, String pickup, String contact) {
        confirmReleaseController.show(food, qty, group, pickup, contact);
    }

    /** 8. Flag Safety Concern */
    public void showFlagSafetyConcernMode() {
        flagSafetyController.show();
    }

    /** 8. Flag Safety Concern about one offer: Send raises the concern. */
    public void showFlagSafetyConcernMode(int offerId) {
        flagOfferId = offerId;
        flagSafetyController.show(offerId);
    }

    /** 9. Accept Offer (Donor) */
    public void showAcceptOfferMode(String groupName, String quantity, String pickupTime, int otherOffersCount) {
        acceptOfferController.show(groupName, quantity, pickupTime, otherOffersCount);
    }

    /** 9. Accept Offer, with a real action. */
    public void showAcceptOfferMode(String groupName, String quantity, String pickupTime, int otherOffersCount, Runnable onConfirm) {
        acceptOfferController.show(groupName, quantity, pickupTime, otherOffersCount, onConfirm);
    }

    /** 10. Upload Renewal, with a real action: the file and date go to DocumentReviewService; {@code onDone} runs after it saved. */
    public void showUploadRenewalMode(Document.Type type, Runnable onDone) {
        uploadRenewalController.show(type, onDone);
    }

    /** 17. Edit contact details; {@code onSaved} runs after a successful save. */
    public void showEditContactMode(String contactPerson, String contactPhone, Runnable onSaved) {
        editContactController.show(contactPerson, contactPhone, onSaved);
    }

    /** 15. Upgrade to Business Donor, with a real action; {@code onDone} runs after the upgrade was submitted. */
    public void showUpgradeBusinessDonorMode(Runnable onDone) {
        upgradeBusinessDonorController.show(onDone);
    }

    /** 10. Upload Renewal */
    public void showUploadRenewalMode(String documentName) {
        uploadRenewalController.show(documentName);
    }

    /** 11. Set New Password; {@code currentPassword} is null when the user has to type it. */
    public void showSetNewPasswordMode(String currentPassword, Runnable onDone) {
        setNewPasswordController.show(currentPassword, onDone);
    }

    /** 12. Withdraw Offer (Food Group) */
    public void showWithdrawOfferMode(String donorName) {
        withdrawOfferController.show(donorName);
    }

    /** 12. Withdraw Offer, with a real action. */
    public void showWithdrawOfferMode(String posterName, Runnable onConfirm) {
        withdrawOfferController.show(posterName, onConfirm);
    }

    /** 13. Confirm Receipt (Food Group), with a real action. */
    public void showConfirmReceiptMode(String food, String qty, String donor, String pickup, String contact, String releaseTime, int offerId, Runnable onConfirm) {
        flagOfferId = offerId;
        confirmReceiptController.show(food, qty, donor, pickup, contact, releaseTime, onConfirm);
    }

    /** 13. Confirm Receipt (Food Group) */
    public void showConfirmReceiptMode(String food, String qty, String donor, String pickup, String contact, String releaseTime) {
        confirmReceiptController.show(food, qty, donor, pickup, contact, releaseTime);
    }

    /** 14. Forgot Password */
    public void showForgotPasswordMode(String email) {
        forgotPasswordController.show(email);
    }

    /** 15. Upgrade to Business Donor */
    public void showUpgradeBusinessDonorMode() {
        upgradeBusinessDonorController.show();
    }

    /** 16. A closed safety notice: how it was resolved ("Dismissed", "Account suspended") and the admin's note. */
    public void showNoticeResolvedMode(String how, String adminNote) {
        noticeResolvedController.show(how, adminNote);
    }
}
