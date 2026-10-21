package com.aniplato.controller.shared.dialog;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * The part every dialog shares: its card, opening and closing it, and running a confirm action.
 * A dialog is a subclass with its own FXML (view/shared/dialog/) whose root has fx:id="card".
 */
public abstract class DialogCard {

    @FXML protected VBox card;
    private DialogHost host;

    /** Called once by DialogController when the dialog screen loads. */
    public void attach(DialogHost host) {
        this.host = host;
    }

    /** The dialog's own card, so the overlay can show and hide it. */
    public Node card() {
        return card;
    }

    protected void open() {
        host.show(card);
    }

    protected void close() {
        host.close();
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
        try {
            action.run();
        } catch (RuleViolationException e) {
            DialogController.showError(e.getMessage());
            return;
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            DialogController.showError("Could not save this. Check the database connection and try again.");
            return;
        }
        close();
    }
}
