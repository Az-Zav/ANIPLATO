package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.support.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Labeled;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the real dialog screen and drives its public methods, so the dialog code can be reorganised
 * without anyone clicking through 15 popups. Needs a desktop for the JavaFX toolkit; skipped without one.
 * Error paths are not covered here: they open a modal Alert, which would block the test.
 */
class DialogControllerTest {

    private static final String FXML = "/com/aniplato/view/shared/dialog.fxml";

    private Parent root;
    private DialogController dialogs;

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    @BeforeEach
    void loadDialogs() {
        fx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(DialogController.class.getResource(FXML));
                root = loader.load();
                dialogs = loader.getController();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    //---HELPERS---

    private static void fx(Runnable action) {
        fx(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T fx(Supplier<T> action) {
        return Fx.call(action);
    }

    private List<Node> shownCards() {
        return fx(() -> root.lookupAll(".modal-card").stream().filter(n -> n.isVisible() && n.isManaged()).toList());
    }

    private Node onlyShownCard() {
        List<Node> shown = shownCards();
        assertEquals(1, shown.size(), "exactly one dialog is open");
        return shown.get(0);
    }

    private static String textIn(Node node) {
        StringBuilder sb = new StringBuilder();
        collectText(node, sb);
        return sb.toString();
    }

    private static void collectText(Node node, StringBuilder sb) {
        if (node instanceof Labeled labeled && labeled.getText() != null) sb.append(labeled.getText()).append('\n');
        if (node instanceof TextInputControl input && input.getText() != null) sb.append(input.getText()).append('\n');
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> collectText(child, sb));
    }

    private static <T extends Node> List<T> all(Node card, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(card, type, found);
        return found;
    }

    private static <T extends Node> void collect(Node node, Class<T> type, List<T> found) {
        if (type.isInstance(node)) found.add(type.cast(node));
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> collect(child, type, found));
    }

    private Button buttonStartingWith(Node card, String text) {
        return all(card, Button.class).stream().filter(b -> b.getText() != null && b.getText().startsWith(text))
                .findFirst().orElseThrow(() -> new AssertionError("no button \"" + text + "\" in:\n" + textIn(card)));
    }

    private void click(Node card, String buttonText) {
        fx(() -> buttonStartingWith(card, buttonText).fire());
    }

    private void assertAllClosed() {
        assertEquals(0, shownCards().size(), "no dialog is open");
        assertFalse(fx(() -> root.isVisible()), "the overlay is hidden");
        assertFalse(fx(() -> root.isManaged()));
    }

    //---SCREENS THAT EMBED THE DIALOGS---

    @Test
    void screensThatIncludeTheDialogsStillLoadThem() {
        for (String screen : List.of("login", "register", "register-step2")) {
            Parent loaded = fx(() -> {
                try {
                    return new FXMLLoader(DialogController.class.getResource("/com/aniplato/view/" + screen + ".fxml")).load();
                } catch (Exception e) {
                    throw new IllegalStateException(screen, e);
                }
            });
            assertEquals(15, fx(() -> loaded.lookupAll(".modal-card").size()), screen + " shows all fifteen dialogs");
        }
    }

    //---OPENING---

    @Test
    void everyDialogStartsHidden() {
        assertEquals(0, shownCards().size());
        assertEquals(15, fx(() -> root.lookupAll(".modal-card").size()), "all fifteen dialogs are on the screen");
    }

    @Test
    void rejectShowsItsDescriptionAndAnEmptyReason() {
        fx(() -> dialogs.showRejectMode(7, "Sunrise Bakery"));
        Node card = onlyShownCard();
        assertTrue(textIn(card).contains("We’ll email Sunrise Bakery your reason"));
        assertEquals("", all(card, TextArea.class).get(0).getText());
        assertTrue(fx(() -> root.isVisible() && root.isManaged()));
    }

    @Test
    void approveShowsTheOrganizationAndEmail() {
        fx(() -> dialogs.showApproveMode(7, "Sunrise Bakery", "a@b.test"));
        assertTrue(textIn(onlyShownCard()).contains("We’ll create Sunrise Bakery’s account and email temporary login details to a@b.test."));
    }

    @Test
    void suspendShowsTheOrganizationAndAnEmptyReason() {
        fx(() -> dialogs.showSuspendAccountMode("Sunrise Bakery"));
        Node card = onlyShownCard();
        assertTrue(textIn(card).contains("Sunrise Bakery can still log in"));
        assertEquals("", all(card, TextArea.class).get(0).getText());
    }

    @Test
    void reinstateMentionsTheDetailsOnlyWhenThereAreSome() {
        fx(() -> dialogs.showReinstateAccountMode("Sunrise Bakery", "Documents are current."));
        assertTrue(textIn(onlyShownCard()).contains("Sunrise Bakery gets full access again. Documents are current."));
        fx(() -> dialogs.showReinstateAccountMode("Sunrise Bakery", ""));
        assertTrue(textIn(onlyShownCard()).contains("Sunrise Bakery gets full access again.\n"));
    }

    @Test
    void resolveSafetyNoticeShowsWhoFlaggedWhat() {
        fx(() -> dialogs.showResolveSafetyNoticeMode("Kapwa Kitchen", "Oct 21", "Moldy bread", "Sunrise Bakery"));
        assertTrue(textIn(onlyShownCard()).contains("Flagged by Kapwa Kitchen on Oct 21: \"Moldy bread\" Close it as dismissed, or suspend Sunrise Bakery."));
    }

    @Test
    void cancelListingNamesTheRecipientOnlyWhenThereIsOne() {
        fx(() -> dialogs.showCancelListingMode("Day-old bread", "Kapwa Kitchen"));
        assertTrue(textIn(onlyShownCard()).contains("Day-old bread leaves the board. Kapwa Kitchen’s accepted offer is cancelled with it"));
        fx(() -> dialogs.showCancelListingMode("Day-old bread", null));
        assertTrue(textIn(onlyShownCard()).contains("Day-old bread leaves the board and its listing is cancelled."));
    }

    @Test
    void confirmReleaseFillsTheHandoffAndTicksTheChecks() {
        fx(() -> dialogs.showConfirmReleaseMode("Bread", "12.5 kg", "Kapwa Kitchen", "Oct 22, 9 AM", "0917 123 4567"));
        Node card = onlyShownCard();
        String text = textIn(card);
        for (String expected : List.of("Bread", "12.5 kg", "Kapwa Kitchen", "Oct 22, 9 AM", "0917 123 4567",
                "Kapwa Kitchen hasn’t confirmed receipt yet. The listing becomes Rescued once they do.")) {
            assertTrue(text.contains(expected), expected);
        }
        assertTrue(all(card, CheckBox.class).stream().allMatch(CheckBox::isSelected));
    }

    @Test
    void confirmReceiptMentionsTheReleaseTimeOnlyWhenKnown() {
        fx(() -> dialogs.showConfirmReceiptMode("Bread", "12.5 kg", "Sunrise Bakery", "Oct 22, 9 AM", "0917 123 4567", "9:05 AM"));
        Node card = onlyShownCard();
        assertTrue(textIn(card).contains("Sunrise Bakery confirmed release at 9:05 AM. Confirming receipt marks this listing Rescued."));
        assertTrue(all(card, CheckBox.class).stream().allMatch(CheckBox::isSelected));
        fx(() -> dialogs.showConfirmReceiptMode("Bread", "12.5 kg", "Sunrise Bakery", "Oct 22, 9 AM", "0917 123 4567", null));
        assertTrue(textIn(onlyShownCard()).contains("Confirming receipt marks this listing Rescued."));
    }

    @Test
    void acceptOfferCountsTheOtherOffers() {
        fx(() -> dialogs.showAcceptOfferMode("Kapwa Kitchen", "12.5 kg", "Oct 22 at 9 AM", 1));
        assertTrue(textIn(onlyShownCard()).contains("Kapwa Kitchen picks up the full 12.5 kg Oct 22 at 9 AM. The other 1 offer is declined automatically."));
        fx(() -> dialogs.showAcceptOfferMode("Kapwa Kitchen", "12.5 kg", "Oct 22 at 9 AM", 3));
        assertTrue(textIn(onlyShownCard()).contains("The other 3 offers are declined automatically."));
        fx(() -> dialogs.showAcceptOfferMode("Kapwa Kitchen", "12.5 kg", "Oct 22 at 9 AM", 0));
        assertFalse(textIn(onlyShownCard()).contains("declined automatically"));
    }

    @Test
    void uploadRenewalNamesTheDocumentAndStartsEmpty() {
        fx(() -> dialogs.showUploadRenewalMode("Mayor's Permit"));
        Node card = onlyShownCard();
        assertTrue(textIn(card).contains("Upload renewal: Mayor's Permit"));
        assertTrue(textIn(card).contains("No file chosen"));
    }

    @Test
    void setNewPasswordAsksForTheCurrentPasswordOnlyWhenItIsNotKnown() {
        fx(() -> dialogs.showSetNewPasswordMode("Temp1234", () -> { }));
        assertEquals(2, visiblePasswordFields(onlyShownCard()), "new and confirm only");
        fx(() -> dialogs.showSetNewPasswordMode(null, () -> { }));
        assertEquals(3, visiblePasswordFields(onlyShownCard()), "current, new and confirm");
    }

    private long visiblePasswordFields(Node card) {
        return fx(() -> all(card, PasswordField.class).stream().filter(f -> isShown(f)).count());
    }

    private static boolean isShown(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (!n.isVisible() || !n.isManaged()) return false;
        }
        return true;
    }

    @Test
    void withdrawOfferNamesTheDonor() {
        fx(() -> dialogs.showWithdrawOfferMode("Sunrise Bakery"));
        assertTrue(textIn(onlyShownCard()).contains("Sunrise Bakery won’t see this offer anymore."));
    }

    @Test
    void forgotPasswordPrefillsTheEmail() {
        fx(() -> dialogs.showForgotPasswordMode("me@aniplato.test"));
        assertTrue(all(onlyShownCard(), TextField.class).stream().anyMatch(f -> "me@aniplato.test".equals(f.getText())));
        fx(() -> dialogs.showForgotPasswordMode(null));
        assertTrue(all(onlyShownCard(), TextField.class).stream().allMatch(f -> f.getText().isEmpty()));
    }

    @Test
    void upgradeToBusinessDonorStartsWithNoFilesChosen() {
        fx(() -> dialogs.showUpgradeBusinessDonorMode());
        String text = textIn(onlyShownCard());
        assertEquals(2, text.split("No file chosen", -1).length - 1, "one per permit");
    }

    @Test
    void flagSafetyConcernStartsEmpty() {
        fx(() -> dialogs.showFlagSafetyConcernMode());
        assertEquals("", all(onlyShownCard(), TextArea.class).get(0).getText());
    }

    //---SWITCHING AND CLOSING---

    @Test
    void openingAnotherDialogClosesTheFirst() {
        fx(() -> dialogs.showRejectMode(1, "A"));
        fx(() -> dialogs.showApproveMode(1, "B", "b@b.test"));
        Node card = onlyShownCard();
        assertTrue(textIn(card).contains("We’ll create B’s account"));
        assertFalse(textIn(card).contains("We’ll email"));
    }

    @Test
    void backClosesTheDialogAndTheOverlay() {
        fx(() -> dialogs.showRejectMode(1, "A"));
        click(onlyShownCard(), "← Back");
        assertAllClosed();

        fx(() -> dialogs.showSuspendAccountMode("A"));
        click(onlyShownCard(), "← Back");
        assertAllClosed();

        fx(() -> dialogs.showUpgradeBusinessDonorMode());
        click(onlyShownCard(), "← Back");
        assertAllClosed();
    }

    @Test
    void theSafetyLinkOnReleaseAndReceiptOpensTheFlagConcernDialog() {
        fx(() -> dialogs.showConfirmReleaseMode("Bread", "1 kg", "G", "P", "C"));
        Node release = onlyShownCard();
        fx(() -> all(release, Hyperlink.class).get(0).fire());
        assertEquals(1, all(onlyShownCard(), TextArea.class).size());
        assertTrue(textIn(onlyShownCard()).contains("Send to admin"));

        fx(() -> dialogs.showConfirmReceiptMode("Bread", "1 kg", "D", "P", "C", null));
        Node receipt = onlyShownCard();
        fx(() -> all(receipt, Hyperlink.class).get(0).fire());
        assertTrue(textIn(onlyShownCard()).contains("Send to admin"));
    }

    @Test
    void dialogsWithoutAnActionJustClose() {
        fx(() -> dialogs.showCancelListingMode("Bread", null));
        click(onlyShownCard(), "✕ Cancel listing");
        assertAllClosed();

        fx(() -> dialogs.showConfirmReleaseMode("Bread", "1 kg", "G", "P", "C"));
        click(onlyShownCard(), "Confirm Release");
        assertAllClosed();

        fx(() -> dialogs.showFlagSafetyConcernMode());
        click(onlyShownCard(), "Send to admin");
        assertAllClosed();

        fx(() -> dialogs.showAcceptOfferMode("G", "1 kg", "now", 0));
        click(onlyShownCard(), "Accept offer");
        assertAllClosed();

        fx(() -> dialogs.showWithdrawOfferMode("D"));
        click(onlyShownCard(), "✕ Withdraw");
        assertAllClosed();

        fx(() -> dialogs.showConfirmReceiptMode("Bread", "1 kg", "D", "P", "C", null));
        click(onlyShownCard(), "Confirm Receipt");
        assertAllClosed();

        fx(() -> dialogs.showUploadRenewalMode("Permit"));
        click(onlyShownCard(), "Submit for review");
        assertAllClosed();

        fx(() -> dialogs.showUpgradeBusinessDonorMode());
        click(onlyShownCard(), "Submit for review");
        assertAllClosed();
    }

    //---ADMIN CALLBACKS---

    @Test
    void suspendHandsTheTypedReasonToTheCallbackAndCloses() {
        AtomicReference<String> reason = new AtomicReference<>();
        fx(() -> dialogs.showSuspendAccountMode("Sunrise Bakery", reason::set));
        Node card = onlyShownCard();
        fx(() -> all(card, TextArea.class).get(0).setText("Reported twice"));
        click(card, "✕ Suspend");
        assertEquals("Reported twice", reason.get());
        assertAllClosed();
    }

    @Test
    void suspendWithoutACallbackJustCloses() {
        fx(() -> dialogs.showSuspendAccountMode("Sunrise Bakery"));
        click(onlyShownCard(), "✕ Suspend");
        assertAllClosed();
    }

    @Test
    void reinstateRunsTheCallbackAndCloses() {
        AtomicReference<Boolean> ran = new AtomicReference<>(false);
        fx(() -> dialogs.showReinstateAccountMode("Sunrise Bakery", "", () -> ran.set(true)));
        click(onlyShownCard(), "Reinstate");
        assertTrue(ran.get());
        assertAllClosed();
    }

    @Test
    void aCallbackIsForgottenWhenTheDialogIsOpenedAgainWithoutOne() {
        AtomicReference<String> reason = new AtomicReference<>();
        fx(() -> dialogs.showSuspendAccountMode("A", reason::set));
        fx(() -> dialogs.showSuspendAccountMode("B"));
        click(onlyShownCard(), "✕ Suspend");
        assertNull(reason.get());
    }

    @Test
    void resolveSafetyNoticeHandsTheNoteToTheMatchingCallback() {
        AtomicReference<String> dismissed = new AtomicReference<>();
        AtomicReference<String> suspended = new AtomicReference<>();
        fx(() -> dialogs.showResolveSafetyNoticeMode("G", "Oct 21", "Moldy", "Sunrise", dismissed::set, suspended::set));
        Node card = onlyShownCard();
        fx(() -> all(card, TextArea.class).get(0).setText("Checked"));
        click(card, "Dismiss");
        assertEquals("Checked", dismissed.get());
        assertNull(suspended.get());
        assertAllClosed();

        fx(() -> dialogs.showResolveSafetyNoticeMode("G", "Oct 21", "Moldy", "Sunrise", dismissed::set, suspended::set));
        Node again = onlyShownCard();
        fx(() -> all(again, TextArea.class).get(0).setText("Unsafe"));
        click(again, "Suspend account");
        assertEquals("Unsafe", suspended.get());
        assertAllClosed();
    }

    @Test
    void resolveSafetyNoticeWithoutCallbacksJustCloses() {
        fx(() -> dialogs.showResolveSafetyNoticeMode("G", "Oct 21", "Moldy", "Sunrise"));
        click(onlyShownCard(), "Dismiss");
        assertAllClosed();
    }
}
