package com.aniplato.controller.shared.dialog;

import com.aniplato.controller.FormSupport;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * The part every dialog shares: its card, opening and closing it, and running a confirm action.
 * A dialog is a subclass with its own FXML (view/shared/dialog/) whose root has fx:id="card".
 */
public abstract class DialogCard {

    @FXML protected VBox card;
    private DialogHost host;
    private Label message;

    /** Called once by DialogController when the dialog screen loads. */
    public void attach(DialogHost host) {
        this.host = host;
    }

    /** The dialog's own card, so the overlay can show and hide it. */
    public Node card() {
        return card;
    }

    protected void open() {
        clearMessage();
        host.show(card);
    }

    protected void close() {
        host.close();
    }

    /** A8: a broken rule is written inside the card, above its buttons, so the dialog stays open and nothing pops up. */
    protected void showProblem(String message) {
        message(message, "inline-error-text");
    }

    /** A plain confirmation inside the card (for a dialog that stays open after it worked). */
    protected void showNote(String message) {
        message(message, "inline-success-text");
    }

    protected void clearMessage() {
        if (message != null) {
            message.setText("");
            message.setVisible(false);
            message.setManaged(false);
        }
    }

    private void message(String text, String styleClass) {
        if (message == null) {
            message = new Label();
            message.setWrapText(true);
            card.getChildren().add(Math.max(0, card.getChildren().size() - 1), message);   // above the buttons row
        }
        message.getStyleClass().removeAll("inline-error-text", "inline-success-text");
        message.getStyleClass().add(styleClass);
        message.setText(text);
        message.setVisible(true);
        message.setManaged(true);
    }

    protected void openFlagSafetyConcern() {
        host.showFlagSafetyConcern();
    }

    // PATTERN:ACTION cancels dialog overlay
    @FXML
    protected void handleCancel() {
        close();
    }

    /** Runs a confirm action. A broken rule is shown and the dialog stays open; success closes it (A8). */
    protected void confirm(Runnable action) {
        String problem = FormSupport.failure(action, "Could not save this. Check the database connection and try again.");
        if (problem != null) {
            showProblem(problem);
            return;
        }
        close();
    }
}
