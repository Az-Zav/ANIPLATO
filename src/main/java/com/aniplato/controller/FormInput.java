package com.aniplato.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads a form's numbers and times without stopping at the first one that is not readable. An unreadable value becomes
 * a placeholder the service refuses with its usual message for that field, so the service's checks run in the form's
 * own order; the refusal is then shown as the more helpful "Enter a number for the weight." instead. No rule lives here:
 * the placeholder messages below are the services' own, and FormInputTest proves they still are.
 */
public final class FormInput {

    /** What the services say when a weight, a time or a head count is missing or unreadable. */
    public static final String WEIGHT = "The weight must be more than 0 kg.";
    public static final String USE_BY = "Use-by must be in the future.";
    public static final String PICKUP_WINDOW = "The pickup window must end after it starts.";
    public static final String NEEDED_BY = "Needed-by must be in the future.";
    public static final String HEADCOUNT = "How many people it feeds must be more than 0.";
    public static final String PICKUP_TIME = "Pick a pickup time inside the listing's pickup window and before its use-by.";
    public static final String PEOPLE_SERVED = "People served cannot be negative.";

    private final Map<String, String> friendly = new HashMap<>();

    /** The number typed, or NaN (refused with {@code serviceMessage}) when it is not a number. */
    public double number(String text, String field, String serviceMessage) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            friendly.putIfAbsent(serviceMessage, "Enter a number for " + field + ".");
            return Double.NaN;
        }
    }

    /** The whole number typed, or {@code placeholder} (a value the service refuses with {@code serviceMessage}) when it is not one. */
    public int whole(String text, String field, String serviceMessage, int placeholder) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            friendly.putIfAbsent(serviceMessage, "Enter a whole number for " + field + ".");
            return placeholder;
        }
    }

    /** The moment chosen, or null (refused with {@code serviceMessage}) when the date or the time is missing or unreadable. */
    public LocalDateTime when(DateTimeField field, String name, String serviceMessage) {
        String problem = field.problem(name);
        if (problem != null) {
            friendly.putIfAbsent(serviceMessage, problem);
            return null;
        }
        return field.value(name);
    }

    /** The text to show for a refusal: the helpful reading problem when the refusal is only the placeholder's, else the rule's own. */
    String say(String refusal) {
        return friendly.getOrDefault(refusal, refusal);
    }
}
