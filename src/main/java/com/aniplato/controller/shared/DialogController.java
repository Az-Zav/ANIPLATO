package com.aniplato.controller.shared;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.Inline;
import com.aniplato.controller.shared.DialogSpec.Block;
import com.aniplato.controller.shared.DialogSpec.Form;
import com.aniplato.controller.shared.DialogSpec.Style;
import com.aniplato.controller.shared.DialogSpec.Tone;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Document;
import com.aniplato.model.DocumentUpload;
import com.aniplato.service.AuthenticationService;
import com.aniplato.service.DocumentReviewService;
import com.aniplato.service.ProfileService;
import com.aniplato.service.RegistrationService;
import com.aniplato.service.SafetyService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.SvgIcon;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The popup overlay every screen includes. There is one dialog card (view/shared/dialog.fxml); a dialog is a
 * {@link DialogSpec}, so a new popup is a new spec, not a new FXML and controller (A: all popups go through
 * DialogController). The {@code showXxxMode} methods below are the specs, one per popup, each with its own wording.
 */
public class DialogController {

    private static final String SAVE_FAILED = "Could not save this. Check the database connection and try again.";

    @FXML private VBox overlay;
    @FXML private VBox card;
    @FXML private VBox headingBox;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label messageLabel;   // where the open dialog's errors and confirmations appear (Inline)
    @FXML private HBox header;
    @FXML private HBox buttonRow;

    private final RegistrationService registrationService = new RegistrationService();
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final ProfileService profileService = new ProfileService();
    private final DocumentReviewService documentReviewService = new DocumentReviewService();
    private final SafetyService safetyService = new SafetyService();

    @FXML
    private void initialize() {
        Inline.bindDialog(messageLabel, card::isVisible);
        Inline.hide(messageLabel);
        close();
    }

    //---THE CARD---

    /** Draws the spec into the card and shows it, replacing whatever dialog was open. */
    public void open(DialogSpec spec) {
        Form form = new Form();
        Inline.clearDialog();

        header.getChildren().set(0, icon(spec));
        headingBox.setSpacing(spec.wide ? 4 : 8);
        titleLabel.setText(spec.title);
        subtitleLabel.setText(spec.subtitle);
        card.setMaxWidth(spec.wide ? 520 : 480);

        // between the heading (first child) and the message line and buttons (last two) sit the blocks of this dialog
        card.getChildren().subList(1, card.getChildren().size() - 2).clear();
        int at = 1;
        for (int i = 0; i < spec.body.size(); i++) {
            List<Block> blocks = spec.body.get(i);
            if (blocks.size() == 1) {
                card.getChildren().add(at++, blocks.get(0).apply(form));
            } else {
                VBox group = new VBox(spec.gaps.get(i));
                blocks.forEach(block -> group.getChildren().add(block.apply(form)));
                card.getChildren().add(at++, group);
            }
        }

        buttonRow.getChildren().clear();
        if (!spec.noBack) {
            Button back = new Button("← Back");
            back.getStyleClass().add(Style.SECONDARY.css);
            back.setOnAction(e -> {
                if (spec.onBack != null) spec.onBack.run();
                close();
            });
            buttonRow.getChildren().add(back);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonRow.getChildren().add(spacer);
        for (DialogSpec.ButtonSpec choice : spec.actions) {
            Button button = new Button(choice.label());
            button.getStyleClass().add(choice.style().css);
            button.setMinWidth(choice.minWidth());
            button.setOnAction(e -> run(choice.action(), form));
            buttonRow.getChildren().add(button);
        }

        card.setVisible(true);
        card.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    public void close() {
        card.setVisible(false);
        card.setManaged(false);
        overlay.setVisible(false);
        overlay.setManaged(false);
        Inline.clearDialog();
    }

    /** A broken rule is shown in the dialog and it stays open so the entry can be fixed (A8); success closes it. */
    private void run(DialogSpec.Action action, Form form) {
        String problem = FormSupport.failure(() -> action.run(form), SAVE_FAILED);
        if (problem != null) {
            Inline.error(problem);
            return;
        }
        if (!form.staysOpen()) close();
    }

    private static Node icon(DialogSpec spec) {
        boolean green = spec.tone == Tone.GREEN;
        if (spec.emoji != null) {
            Label symbol = new Label(spec.emoji);
            symbol.getStyleClass().add(green ? "modal-icon-green" : "modal-icon-red");
            return symbol;
        }
        StackPane box = new StackPane(SvgIcon.load(green ? "/assets/Check_Button.svg" : "/assets/Reject_Button.svg", 24));
        box.getStyleClass().add(green ? "modal-icon-green-box" : "modal-icon-red-box");
        return box;
    }

    //---THE POPUPS---

    /** Reject Org: the admin's reason is emailed and no account is created. */
    public void showRejectMode(int registrationId, String orgName) {
        open(DialogSpec.of(Tone.RED, "Reject this organization?",
                        "We’ll email " + orgName + " your reason so they can fix their documents and register again. No account is created.")
                .body(Block.reason("Reason (sent by email) *", "e.g., The Mayor’s Permit is expired. Upload the 2026 permit."))
                .action("✕ Reject", Style.DANGER, 100, form -> {
                    boolean emailed = registrationService.reject(registrationId, form.text("reason"));
                    if (!emailed) Inline.notice("The registration was rejected, but the email could not be sent. Check the email log.");
                    Navigator.show(Screen.PENDING_REGISTRATIONS);
                }));
    }

    /** Approve Org: creates the account and emails the temporary login. */
    public void showApproveMode(int registrationId, String orgName, String orgEmail) {
        open(DialogSpec.of(Tone.GREEN, "Approve this organization?",
                        "We’ll create " + orgName + "’s account and email temporary login details to " + orgEmail + ".")
                .action("✓ Approve", Style.PRIMARY, 110, form -> {
                    boolean emailed = registrationService.approve(registrationId);
                    if (!emailed) {
                        Inline.notice("The account was created, but the email could not be sent. "
                                + "The user can get a temporary password with Forgot password.");
                    }
                    Navigator.show(Screen.PENDING_REGISTRATIONS);
                }));
    }

    /** Resolve Safety Notice: each button's callback gets the admin note. */
    public void showResolveSafetyNoticeMode(String raisedBy, String date, String concern, String flaggedOrg,
                                            Consumer<String> onDismiss, Consumer<String> onSuspendAccount) {
        open(DialogSpec.of(Tone.RED, "Resolve safety notice",
                        "Flagged by " + raisedBy + " on " + date + ": \"" + concern + "\" Close it as dismissed, or suspend " + flaggedOrg + ".")
                .emoji("⚠")
                .body(Block.reason("Admin note *", "Short note for the record"))
                .action("Dismiss", Style.SECONDARY, 90, form -> onDismiss.accept(form.text("reason")))
                .action("Suspend account", Style.DANGER, 140, form -> onSuspendAccount.accept(form.text("reason"))));
    }

    /** Suspend Acct: {@code onConfirm} gets the reason. */
    public void showSuspendAccountMode(String orgName, Consumer<String> onConfirm) {
        open(DialogSpec.of(Tone.RED, "Suspend this account?",
                        orgName + " can still log in, upload renewals and finish handoffs it already accepted. It can’t post, re-post, offer or accept, "
                                + "and its listings are hidden from food groups, until you reinstate it.")
                .body(Block.reason("Reason (shown to the organization) *", "e.g., Safety notice #12 confirmed."))
                .action("✕ Suspend", Style.DANGER, 110, form -> onConfirm.accept(form.text("reason"))));
    }

    /** Reinstate Acct */
    public void showReinstateAccountMode(String orgName, String details, Runnable onConfirm) {
        String more = details != null && !details.isEmpty() ? " " + details : "";
        open(DialogSpec.of(Tone.GREEN, "Reinstate this account?", orgName + " gets full access again." + more)
                .action("Reinstate", Style.PRIMARY, 110, form -> onConfirm.run()));
    }

    /** Cancel Listing or Request: {@code onConfirm} gets the reason. */
    public void showCancelListingMode(String listingTitle, String recipientName, Consumer<String> onConfirm) {
        String what = recipientName != null && !recipientName.isEmpty()
                ? listingTitle + " leaves the board. " + recipientName + "’s accepted offer is cancelled with it, and they see your reason."
                : listingTitle + " leaves the board and its listing is cancelled.";
        open(DialogSpec.of(Tone.RED, "Cancel this listing?", what)
                .body(Block.reason("Reason *", "e.g., Our staff already gave it away."))
                .action("✕ Cancel listing", Style.DANGER, 130, form -> onConfirm.accept(form.text("reason"))));
    }

    /** Confirm Release (Donor) */
    public void showConfirmReleaseMode(String food, String qty, String group, String pickup, String contact, int offerId, Runnable onConfirm) {
        open(DialogSpec.of(Tone.GREEN, "Confirm release?", "Confirm that this food was handed over to the food group.")
                .emoji("📦").wide()
                .body(Block.facts(new String[][] {{"Food", food}, {"Quantity", qty}, {"Food group", group}, {"Pickup", pickup}, {"Contact", contact}}))
                .body(Block.checks("Before releasing", "Packaging is sealed and clean", "Food was kept at its storage condition"))
                .body(Block.notice(group + " hasn’t confirmed receipt yet. The listing becomes Rescued once they do.",
                        "tag-reregistered", "required-star", "text-muted"))
                .body(flagLink(offerId))
                .action("Confirm Release", Style.PRIMARY, 130, form -> onConfirm.run()));
    }

    /** Flag Safety Concern about one offer: Send raises the concern. */
    public void showFlagSafetyConcernMode(int offerId) {
        open(DialogSpec.of(Tone.RED, "Flag a safety concern",
                        "Describe the safety concern. The other party and the ANIPLATO admin will be notified. "
                                + "Admin may dismiss the notice or suspend the account responsible.")
                .emoji("⚠")
                .body(Block.reason("Concern *", "What happened? e.g., Trays were warm and smelled sour at pickup."))
                .action("Send to admin", Style.DANGER, 120, form -> safetyService.raise(offerId, form.text("reason"))));
    }

    /** Accept Offer */
    public void showAcceptOfferMode(String groupName, String quantity, String pickupTime, int otherOffersCount, Runnable onConfirm) {
        String others = otherOffersCount > 0
                ? " The other " + otherOffersCount + " " + (otherOffersCount == 1 ? "offer is" : "offers are") + " declined automatically."
                : "";
        open(DialogSpec.of(Tone.GREEN, "Accept this offer?",
                        groupName + " picks up the full " + quantity + " " + pickupTime + "." + others
                                + " Your contact number and handover instructions are shared with " + groupName + ", and you’ll see theirs.")
                .action("Accept offer", Style.PRIMARY, 110, form -> onConfirm.run()));
    }

    /** Upload Renewal: the file and date go to DocumentReviewService; {@code onDone} runs after it saved. */
    public void showUploadRenewalMode(Document.Type type, Runnable onDone) {
        open(DialogSpec.of(Tone.GREEN, "Upload renewal: " + type.label(),
                        "Upload the renewed permit and type its new expiry date. Your current file stays on record until an admin verifies the new one.")
                .group(16,
                        Block.file("file", "Renewed document *", "Choose the renewed document"),
                        Block.dateOrNoExpiry("expiry", "New expiry date *", "noExpiry", !type.requiresExpiry()))
                .action("Submit for review", Style.PRIMARY, 150, form -> {
                    documentReviewService.uploadRenewal(type, form.file("file"), form.date("expiry"), form.checked("noExpiry"));
                    onDone.run();
                }));
    }

    /** Edit contact details; {@code onSaved} runs after a successful save. */
    public void showEditContactMode(String contactPerson, String contactPhone, Runnable onSaved) {
        open(DialogSpec.of(Tone.GREEN, "Edit contact details",
                        "Only you, the ANIPLATO admin and the other side of an accepted offer see the number.")
                .group(12,
                        Block.text("person", "Contact person *", null, contactPerson, false),
                        Block.text("phone", "Contact number *", "09171234567", contactPhone, false))
                .action("Save", Style.PRIMARY, 120, form -> {
                    profileService.updateContact(form.text("person"), form.text("phone"));
                    if (onSaved != null) onSaved.run();
                }));
    }

    /** Upgrade to Business Donor; {@code onDone} runs after the upgrade was submitted. */
    public void showUpgradeBusinessDonorMode(Runnable onDone) {
        open(DialogSpec.of(Tone.GREEN, "Upgrade to Business donor",
                        "Upload your Mayor's Permit and Sanitary Permit with their expiry dates. An admin reviews them, "
                                + "and you stay a Casual donor until they're approved.")
                .group(16,
                        Block.file("mayor", "Mayor's / Business Permit *", "Choose your Mayor's Permit"),
                        Block.date("mayorDate", "Expiry date *"),
                        Block.file("sanitary", "Sanitary Permit *", "Choose your Sanitary Permit"),
                        Block.date("sanitaryDate", "Expiry date *"),
                        Block.consent("consent", "I agree that ANIPLATO may store my details and documents to verify my registration. "
                                + "Only I and the ANIPLATO admin can see my files."))
                .action("Submit for review", Style.PRIMARY, 150, form -> {
                    List<DocumentUpload> uploads = new ArrayList<>();
                    if (form.file("mayor") != null) {
                        uploads.add(new DocumentUpload(Document.Type.MAYORS_PERMIT, form.file("mayor"), form.date("mayorDate"), false));
                    }
                    if (form.file("sanitary") != null) {
                        uploads.add(new DocumentUpload(Document.Type.SANITARY_PERMIT, form.file("sanitary"), form.date("sanitaryDate"), false));
                    }
                    profileService.submitUpgrade(uploads, form.checked("consent"));
                    onDone.run();
                }));
    }

    /** Set New Password; {@code currentPassword} is null when the user has to type it (R1.7). */
    public void showSetNewPasswordMode(String currentPassword, Runnable onDone) {
        boolean askCurrent = currentPassword == null;   // first login: the temporary password was just typed at login
        List<Block> fields = new ArrayList<>();
        if (askCurrent) fields.add(Block.text("current", "Current password *", "Your current password", null, true));
        fields.add(Block.text("new", "New password *", "At least 8 characters", null, true));
        fields.add(Block.text("confirm", "Confirm new password *", "Type it again", null, true));
        open(DialogSpec.of(Tone.GREEN, "Set a new password",
                        "You’re logged in with the temporary password from your approval email. Choose your own password to continue.")
                .group(16, fields.toArray(Block[]::new))
                .body(Block.hint("This step can’t be skipped. You’ll see it each time you log in with the temporary password until you set your own."))
                .onBack(() -> {
                    if (authenticationService.mustChangePassword()) authenticationService.logout();   // R1.7: going back ends the half-started session
                })
                .action("Save password", Style.PRIMARY, 140, form -> {
                    authenticationService.changePassword(askCurrent ? form.text("current") : currentPassword,
                            form.text("new"), form.text("confirm"));
                    if (onDone != null) onDone.run();
                }));
    }

    /** Withdraw Offer (Food Group) */
    public void showWithdrawOfferMode(String posterName, Runnable onConfirm) {
        open(DialogSpec.of(Tone.RED, "Withdraw your offer?",
                        posterName + " won’t see this offer anymore. You can send a new one while the listing is still open.")
                .action("✕ Withdraw", Style.DANGER, 120, form -> onConfirm.run()));
    }

    /** Confirm Receipt (Food Group) */
    public void showConfirmReceiptMode(String food, String qty, String donor, String pickup, String contact, String releaseTime, int offerId, Runnable onConfirm) {
        String notice = releaseTime != null && !releaseTime.isEmpty()
                ? donor + " confirmed release at " + releaseTime + ". Confirming receipt marks this listing Rescued."
                : "Confirming receipt marks this listing Rescued.";
        open(DialogSpec.of(Tone.GREEN, "Confirm receipt?", "Confirm that you received this food from the donor.")
                .emoji("📦").wide()
                .body(Block.facts(new String[][] {{"Food", food}, {"Quantity", qty}, {"Donor", donor}, {"Pickup", pickup}, {"Contact", contact}}))
                .body(Block.checks("Before confirming", "Quantity matches the listing", "Food arrived sealed and within its use-by time"))
                .body(Block.notice(notice, "pill-food-donor", "text-success-bold", "table-cell-text"))
                .body(flagLink(offerId))
                .action("Confirm Receipt", Style.PRIMARY, 130, form -> onConfirm.run()));
    }

    /** Forgot Password: the same answer for every email, so it never reveals which have accounts. */
    public void showForgotPasswordMode(String email) {
        open(DialogSpec.of(Tone.GREEN, "Forgot your password?",
                        "Enter your account's email address below. If an account matches, we'll send instructions to reset your password.")
                .group(8,
                        Block.text("email", "Enter your email *", "e.g., name@organization.ph", email != null ? email : "", false),
                        Block.hint("We'll send a password reset link or temporary access code to this address."))
                .action("Send reset email", Style.PRIMARY, 130, form -> {
                    String typed = form.text("email").trim();
                    if (typed.isEmpty()) throw new RuleViolationException("Please enter your email address.");
                    authenticationService.requestPasswordReset(typed);
                    Inline.success("If that email has an account, a temporary password is on its way.");
                    form.keepOpen();
                }));
    }

    /** A closed safety notice: how it was resolved ("Dismissed", "Account suspended") and the admin's note. */
    public void showNoticeResolvedMode(String how, String adminNote) {
        open(DialogSpec.of(Tone.GREEN, how, "Admin note: " + (adminNote == null ? "—" : adminNote)).infoOnly("Close"));
    }

    /** The link at the bottom of Release and Receipt that opens the Flag dialog for the same offer. */
    private Block flagLink(int offerId) {
        return Block.link("Something wrong with the food? Flag a safety concern", () -> showFlagSafetyConcernMode(offerId));
    }
}
