package com.aniplato.util;

/** The registration the admin opened from a list, carried to the Review screen. Navigator only switches screens, so the choice travels here. */
public final class AdminSelection {
    private static int registrationId;

    private AdminSelection() {}

    public static int registrationId() {
        return registrationId;
    }

    public static void selectRegistration(int id) {
        registrationId = id;
    }
}
