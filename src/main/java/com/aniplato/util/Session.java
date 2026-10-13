package com.aniplato.util;

import com.aniplato.model.Account;

/** Who is logged in. Set by AuthenticationService on login, cleared on logout. */
public final class Session {

    private static Account current;

    private Session() {}

    public static void start(Account account) {
        current = account;
    }

    public static void clear() {
        current = null;
    }

    public static boolean isLoggedIn() {
        return current != null;
    }

    /** The logged-in account, or null on the login and register screens. */
    public static Account current() {
        return current;
    }

    /** The logged-in account. Services call this; reaching it with nobody logged in is a bug. */
    public static Account require() {
        if (current == null) throw new IllegalStateException("No one is logged in");
        return current;
    }
}
