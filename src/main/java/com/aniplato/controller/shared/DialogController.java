package com.aniplato.controller.shared;

import com.aniplato.controller.shared.dialog.AcceptOfferDialogController;
import com.aniplato.controller.shared.dialog.ApproveDialogController;
import com.aniplato.controller.shared.dialog.CancelListingDialogController;
import com.aniplato.controller.shared.dialog.ConfirmReceiptDialogController;
import com.aniplato.controller.shared.dialog.ConfirmReleaseDialogController;
import com.aniplato.controller.shared.dialog.DialogCard;
import com.aniplato.model.Document;
import com.aniplato.controller.shared.dialog.DialogHost;
import com.aniplato.controller.shared.dialog.FlagSafetyDialogController;
import com.aniplato.controller.shared.dialog.ForgotPasswordDialogController;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
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

    private List<DialogCard> cards;

    // While a card is open the overlay is lifted out of its screen and laid over the whole window (see liftOverWindow)
    private Pane home;
    private int homeIndex;
    private Scene liftedScene;
    private Parent screenRoot;
    private StackPane windowLayer;

    @FXML
    private void initialize() {
        cards = List.of(rejectController, approveController, resolveSafetyController, suspendController,
                reinstateController, cancelListingController, confirmReleaseController, flagSafetyController,
                acceptOfferController, uploadRenewalController, setNewPasswordController, withdrawOfferController,
                confirmReceiptController, forgotPasswordController, upgradeBusinessDonorController);
        cards.forEach(card -> card.attach(this));
        hideAll();
        overlay.setVisible(false);   // closed until a card opens, whatever the including screen's FXML says
        overlay.setManaged(false);

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
        liftOverWindow();
    }

    @Override
    public void close() {
        hideAll();
        overlay.setVisible(false);
        overlay.setManaged(false);
        dropBackIntoScreen();
    }

    /**
     * A popup is an overlay over the whole window, not a block inside the screen (a screen puts the include in its own
     * layout, so left alone it would show as a grey section under the content). Wraps the window's root in a stack with
     * the overlay on top; {@link #dropBackIntoScreen} undoes it. With no scene (a test) nothing moves.
     */
    private void liftOverWindow() {
        Scene scene = overlay.getScene();
        if (scene == null || liftedScene != null || !(overlay.getParent() instanceof Pane parent)) return;
        home = parent;
        homeIndex = parent.getChildren().indexOf(overlay);
        parent.getChildren().remove(overlay);
        liftedScene = scene;
        screenRoot = scene.getRoot();
        scene.setRoot(new Pane());   // frees the old root so it can be a child of the stack
        windowLayer = new StackPane(screenRoot, overlay);
        windowLayer.getStylesheets().add(DialogController.class.getResource("/com/aniplato/view/aniplato.css").toExternalForm());
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scene.setRoot(windowLayer);
    }

    private void dropBackIntoScreen() {
        if (liftedScene == null) return;
        windowLayer.getChildren().remove(overlay);
        if (liftedScene.getRoot() == windowLayer) {   // the screen did not change meanwhile: give it its root back
            windowLayer.getChildren().remove(screenRoot);
            liftedScene.setRoot(screenRoot);
        }
        home.getChildren().add(Math.min(homeIndex, home.getChildren().size()), overlay);
        liftedScene = null;
        screenRoot = null;
        windowLayer = null;
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

    //---OPENING A DIALOG---

    /** 1. Reject Org */
    public void showRejectMode(int registrationId, String orgName) {
        rejectController.show(registrationId, orgName);
    }

    /** 2. Approve Org */
    public void showApproveMode(int registrationId, String orgName, String orgEmail) {
        approveController.show(registrationId, orgName, orgEmail);
    }

    /** 3. Resolve Safety Notice: each button's callback gets the admin note. */
    public void showResolveSafetyNoticeMode(String raisedBy, String date, String concern, String flaggedOrg,
                                            Consumer<String> onDismiss, Consumer<String> onSuspendAccount) {
        resolveSafetyController.show(raisedBy, date, concern, flaggedOrg, onDismiss, onSuspendAccount);
    }

    /** 4. Suspend Acct: {@code onConfirm} gets the reason. */
    public void showSuspendAccountMode(String orgName, Consumer<String> onConfirm) {
        suspendController.show(orgName, onConfirm);
    }

    /** 5. Reinstate Acct */
    public void showReinstateAccountMode(String orgName, String details, Runnable onConfirm) {
        reinstateController.show(orgName, details, onConfirm);
    }

    /** 6. Cancel Listing or Request: {@code onConfirm} gets the reason. */
    public void showCancelListingMode(String listingTitle, String recipientName, Consumer<String> onConfirm) {
        cancelListingController.show(listingTitle, recipientName, onConfirm);
    }

    /** 7. Confirm Release (Donor) */
    public void showConfirmReleaseMode(String food, String qty, String group, String pickup, String contact, int offerId, Runnable onConfirm) {
        flagOfferId = offerId;
        confirmReleaseController.show(food, qty, group, pickup, contact, onConfirm);
    }

    /** 8. Flag Safety Concern about one offer: Send raises the concern. */
    public void showFlagSafetyConcernMode(int offerId) {
        flagOfferId = offerId;
        flagSafetyController.show(offerId);
    }

    /** 9. Accept Offer */
    public void showAcceptOfferMode(String groupName, String quantity, String pickupTime, int otherOffersCount, Runnable onConfirm) {
        acceptOfferController.show(groupName, quantity, pickupTime, otherOffersCount, onConfirm);
    }

    /** 10. Upload Renewal: the file and date go to DocumentReviewService; {@code onDone} runs after it saved. */
    public void showUploadRenewalMode(Document.Type type, Runnable onDone) {
        uploadRenewalController.show(type, onDone);
    }

    /** 15. Upgrade to Business Donor; {@code onDone} runs after the upgrade was submitted. */
    public void showUpgradeBusinessDonorMode(Runnable onDone) {
        upgradeBusinessDonorController.show(onDone);
    }

    /** 11. Set New Password; {@code currentPassword} is null when the user has to type it. */
    public void showSetNewPasswordMode(String currentPassword, Runnable onDone) {
        setNewPasswordController.show(currentPassword, onDone);
    }

    /** 12. Withdraw Offer (Food Group) */
    public void showWithdrawOfferMode(String posterName, Runnable onConfirm) {
        withdrawOfferController.show(posterName, onConfirm);
    }

    /** 13. Confirm Receipt (Food Group) */
    public void showConfirmReceiptMode(String food, String qty, String donor, String pickup, String contact, String releaseTime, int offerId, Runnable onConfirm) {
        flagOfferId = offerId;
        confirmReceiptController.show(food, qty, donor, pickup, contact, releaseTime, onConfirm);
    }

    /** 14. Forgot Password */
    public void showForgotPasswordMode(String email) {
        forgotPasswordController.show(email);
    }

}
