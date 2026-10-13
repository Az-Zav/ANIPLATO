package com.aniplato.util;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt hash and check. */
public final class PasswordHasher {

    private PasswordHasher() {}

    /** Hashes a plain password. The result is 60 characters, the size of account.password_hash. */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }

    /** True if the plain password matches the stored hash. A missing or malformed hash never matches. */
    public static boolean matches(String plain, String hash) {
        if (plain == null || hash == null) return false;
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (IllegalArgumentException e) {
            return false;   // not a BCrypt hash
        }
    }
}
