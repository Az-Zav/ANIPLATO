package com.aniplato.util;

import com.aniplato.model.Registration;
import com.aniplato.service.RegistrationService.DocumentUpload;

import java.util.List;

/** The registration form carried between Register, Register Step 2 and Submitted. Cleared when Submitted loads. */
public final class RegistrationDraft {
    private static Registration current;
    private static List<DocumentUpload> uploads = List.of();   // the files chosen on step 2, kept while going Back

    private RegistrationDraft() {}

    public static Registration get() {
        return current;
    }

    public static void set(Registration registration) {
        current = registration;
    }

    public static List<DocumentUpload> uploads() {
        return uploads;
    }

    public static void setUploads(List<DocumentUpload> chosen) {
        uploads = List.copyOf(chosen);
    }

    public static void clear() {
        current = null;
        uploads = List.of();
    }
}
