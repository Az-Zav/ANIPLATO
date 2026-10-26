package com.aniplato.controller;

import com.aniplato.support.Fx;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a message goes (A8): the dialog if one is open, else the screen's bar; a notice survives one screen change. */
class InlineTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    @Test
    void anErrorGoesToTheScreenBarOrToTheOpenDialog() {
        Fx.run(() -> {
            Label bar = new Label();
            Label dialog = new Label();
            boolean[] open = {false};
            Inline.bindScreen(bar);
            Inline.bindDialog(dialog, () -> open[0]);
            Inline.hide(dialog);   // DialogController hides its line when it loads

            Inline.error("on the screen");
            assertEquals("on the screen", bar.getText());
            assertTrue(bar.isVisible() && bar.isManaged());
            assertFalse(dialog.isVisible());

            open[0] = true;
            Inline.error("in the dialog");
            assertEquals("in the dialog", dialog.getText());
            assertEquals("on the screen", bar.getText(), "the bar is left alone while a dialog is open");

            Inline.clearDialog();
            assertFalse(dialog.isVisible());
        });
    }

    @Test
    void aNoticeAppearsAfterTheNextScreenChangeOnlyOnce() {
        Fx.run(() -> {
            Label bar = new Label();
            Inline.bindScreen(bar);
            Inline.bindDialog(new Label(), () -> false);

            Inline.error("old problem");
            Inline.notice("The account was created, but the email could not be sent.");
            Inline.clearScreen();   // the screen change
            assertEquals("The account was created, but the email could not be sent.", bar.getText());
            assertTrue(bar.isVisible());

            Inline.clearScreen();   // the next one
            assertFalse(bar.isVisible());
        });
    }

    @Test
    void successUsesItsOwnStyleAndErrorTakesItBack() {
        Fx.run(() -> {
            Label bar = new Label();
            Inline.bindScreen(bar);
            Inline.bindDialog(new Label(), () -> false);
            Inline.success("Saved.");
            assertTrue(bar.getStyleClass().contains("success-text"));
            Inline.error("Not saved.");
            assertTrue(bar.getStyleClass().contains("error-text"));
            assertFalse(bar.getStyleClass().contains("success-text"));
        });
    }

    @Test
    void fadeKeepsThePlaceAndHideGivesItBack() {
        Fx.run(() -> {
            Label label = new Label("x");
            Inline.show(label, "problem");
            Inline.fade(label);
            assertFalse(label.isVisible());
            assertTrue(label.isManaged());
            Inline.hide(label);
            assertFalse(label.isManaged());
        });
    }
}
