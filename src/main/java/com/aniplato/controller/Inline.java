package com.aniplato.controller;

import javafx.scene.control.Label;

import java.util.function.BooleanSupplier;

/**
 * Where a message to the user is shown: in a label on the screen, never in a pop-up (A8). Three kinds of place:
 * the shell's bar above the content, the open dialog's own bar, and a screen's own label ({@link #show}).
 * An error goes to the dialog if one is open, otherwise to the bar; with nothing on screen it is only printed.
 */
public final class Inline {

    private static Label screenBar;
    private static Label dialogBar;
    private static BooleanSupplier dialogOpen = () -> false;
    private static String carried;   // an "it worked, but" notice that must survive the screen change that follows it

    private Inline() { }

    /** The shell's bar; set once by ShellController. */
    public static void bindScreen(Label bar) {
        screenBar = bar;
    }

    /** The bar under the dialogs; set by each DialogController, {@code isOpen} says whether one is showing. */
    public static void bindDialog(Label bar, BooleanSupplier isOpen) {
        dialogBar = bar;
        dialogOpen = isOpen;
    }

    public static void error(String message) {
        put(message, "error");
    }

    public static void success(String message) {
        put(message, "success");
    }

    /** A notice for after the next screen change, e.g. "the account was created, but the email could not be sent". */
    public static void notice(String message) {
        carried = message;
    }

    /** Called before each screen loads: the old message goes, a carried notice appears. */
    public static void clearScreen() {
        hide(screenBar);
        if (carried != null) {
            show(screenBar, carried, "error");
            carried = null;
        }
    }

    /** Called when a dialog opens or closes. */
    public static void clearDialog() {
        hide(dialogBar);
    }

    /** Shows or hides a screen's own message label (login, register, review). */
    public static void show(Label label, String message) {
        show(label, message, "error");
    }

    public static void hide(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    /** Hides the text but keeps its place, for a label sharing a row with something that must not move. */
    public static void fade(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
    }

    private static void put(String message, String kind) {
        Label target = dialogOpen.getAsBoolean() ? dialogBar : screenBar;
        if (target == null) {
            System.err.println("[" + kind + "] " + message);   // nothing on screen to show it in (a test, or a screen outside the shell)
            return;
        }
        show(target, message, kind);
    }

    private static void show(Label label, String message, String kind) {
        if (label == null) return;
        label.getStyleClass().removeAll("error-text", "success-text");
        label.getStyleClass().add("success".equals(kind) ? "success-text" : "error-text");
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }
}
