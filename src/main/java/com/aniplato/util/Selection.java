package com.aniplato.util;

/** The post, offer or registration the user opened from a list, carried to the next screen. Navigator only switches screens, so the choice travels here. */
public final class Selection {
    private static int postId;
    private static int offerId;
    private static int registrationId;

    private Selection() { }

    public static int postId() {
        return postId;
    }

    public static int offerId() {
        return offerId;
    }

    public static int registrationId() {
        return registrationId;
    }

    public static void selectRegistration(int id) {
        registrationId = id;
    }

    public static void selectPost(int id) {
        postId = id;
    }

    public static void selectOffer(int id) {
        offerId = id;
    }
}
