package com.aniplato.util;

/** The post or offer the user opened from a list, carried to the next screen. Navigator only switches screens, so the choice travels here. */
public final class Selection {
    private static int postId;
    private static int offerId;

    private Selection() { }

    public static int postId() {
        return postId;
    }

    public static int offerId() {
        return offerId;
    }

    public static void selectPost(int id) {
        postId = id;
    }

    public static void selectOffer(int id) {
        offerId = id;
    }
}
