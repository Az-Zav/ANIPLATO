package com.aniplato.util;

import com.aniplato.model.Registration;

/** The registration form carried between Register, Register Step 2 and Submitted. Cleared when Submitted loads. */
public final class RegistrationDraft {
    private static Registration current;

    private RegistrationDraft() {}

    public static Registration get() {
        return current;
    }

    public static void set(Registration registration) {
        current = registration;
    }

    public static void clear() {
        current = null;
    }
}
