package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** What every form and list screen needs: run a service call and show a broken rule (A8), read numbers, format dates. */
public final class FormSupport {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private FormSupport() { }

    /** Runs the action. A broken rule or a database problem is shown in a message and false is returned; true means it worked. */
    public static boolean attempt(Runnable action) {
        String problem = failure(action, "Could not complete this. Check the database connection and try again.");
        if (problem != null) DialogController.showError(problem);
        return problem == null;
    }

    /**
     * Runs the action and returns null if it worked, else the text to show: the rule's own message for a broken rule (A8),
     * {@code databaseMessage} for a database problem. The one place that decides which failures are shown and how.
     */
    public static String failure(Runnable action, String databaseMessage) {
        try {
            action.run();
            return null;
        } catch (RuleViolationException e) {
            return e.getMessage();
        } catch (DataAccessException | IllegalStateException | ExceptionInInitializerError e) {
            return databaseMessage;
        }
    }

    /** Runs a read and returns its result, or the fallback after showing the problem. */
    public static <T> T load(Supplier<T> read, T fallback) {
        List<T> result = new ArrayList<>(1);
        String problem = failure(() -> result.add(read.get()), "Could not load this screen. Check the database connection and try again.");
        if (problem == null) return result.get(0);
        DialogController.showError(problem);
        return fallback;
    }

    /** A number typed by the user; anything else is refused with a message naming the field. */
    public static double number(String text, String fieldName) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new RuleViolationException("Enter a number for " + fieldName + ".");
        }
    }

    /** A whole number typed by the user. */
    public static int wholeNumber(String text, String fieldName) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new RuleViolationException("Enter a whole number for " + fieldName + ".");
        }
    }

    /** "Oct 22, 2026 4:30 PM". */
    public static String when(LocalDateTime time) {
        return time == null ? "" : time.format(WHEN);
    }

    /** "12.5 kg" (no trailing zeros). */
    public static String kg(double kg) {
        return (kg == Math.rint(kg) ? String.valueOf((long) kg) : String.valueOf(kg)) + " kg";
    }
}
