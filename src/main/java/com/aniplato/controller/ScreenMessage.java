package com.aniplato.controller;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * The inline message bar at the top of every in-shell screen (A8). A broken rule or a database problem is written here
 * instead of in a popup. The shell binds the bar when it loads and clears it before each screen loads.
 */
public final class ScreenMessage {

    private static HBox bar;
    private static Label text;

    private ScreenMessage() { }

    /** Called once by ShellController with the bar from shell.fxml. */
    static void bind(HBox messageBar, Label messageText) {
        bar = messageBar;
        text = messageText;
        clear();
    }

    /** Shows the message in the bar. With no shell on screen (login, register, tests) it goes to the console. */
    public static void error(String message) {
        if (bar == null || bar.getScene() == null) {
            System.err.println("Not shown (no shell): " + message);
            return;
        }
        text.setText(message);
        bar.setVisible(true);
        bar.setManaged(true);
    }

    /** Hides the bar. Runs before each screen loads and when the user dismisses it. */
    public static void clear() {
        if (bar == null) return;
        text.setText("");
        bar.setVisible(false);
        bar.setManaged(false);
    }

    /** The text now in the bar, or null when it is hidden. Tests read this. */
    public static String current() {
        return bar != null && bar.isVisible() ? text.getText() : null;
    }
}
