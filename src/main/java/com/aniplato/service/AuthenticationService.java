package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.util.Database;
import com.aniplato.util.PasswordHasher;
import com.aniplato.util.Session;
import com.aniplato.util.Settings;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Login, logout, change password, forgot password, temporary password (R1.7). */
public class AuthenticationService {

    private static final int TEMP_PASSWORD_LENGTH = 12;
    // no 0/O, 1/l/I, so a password read from an email is typed correctly
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountDao accountDao = new AccountDao();
    private final EmailService emailService = new EmailService();

    /**
     * Checks email and password and starts the session. A Suspended account can still log in (6.3).
     * The caller must check {@link #mustChangePassword()} next and show only the Change Password screen if true (R1.7).
     */
    public Account login(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            throw new RuleViolationException("Enter your email and password.");
        }
        Credentials found = Database.read("Could not load the account", c -> {
            Account account = accountDao.findByEmail(c, email.trim());
            return new Credentials(account, account == null ? null : accountDao.findPasswordHash(c, account.id));
        });
        // same message for both cases, so the screen does not reveal which emails have accounts
        if (found.account() == null || !PasswordHasher.matches(password, found.hash())) {
            throw new RuleViolationException("Wrong email or password.");
        }
        Session.start(found.account());
        return found.account();
    }

    public void logout() {
        Session.clear();
    }

    /** R1.7: true while the logged-in user still has a temporary password. It has no time limit. */
    public boolean mustChangePassword() {
        return Session.isLoggedIn() && Session.current().mustChangePassword;
    }

    /** Sets a new password for the logged-in user: at least 8 characters and different from the current one (7.3). */
    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        Account account = Session.require();
        if (!PasswordHasher.matches(currentPassword, loadPasswordHash(account.id))) {
            throw new RuleViolationException("Your current password is wrong.");
        }
        if (newPassword == null || newPassword.length() < Settings.MIN_PASSWORD_LENGTH) {
            throw new RuleViolationException("The new password must be at least " + Settings.MIN_PASSWORD_LENGTH + " characters.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new RuleViolationException("The new password must be different from the current one.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuleViolationException("The two new passwords do not match.");
        }

        String hash = PasswordHasher.hash(newPassword);
        LocalDateTime now = LocalDateTime.now();
        try (Connection c = Database.getConnection()) {
            accountDao.updatePassword(c, account.id, hash, now);   // also clears must_change_password (R1.7)
        } catch (SQLException e) {
            throw new DataAccessException("Could not change the password", e);
        }
        account.mustChangePassword = false;
        account.passwordChangedAt = now;
    }

    /**
     * Forgot password: emails a new temporary password and sets must_change_password (R1.7).
     * Returns nothing and acts the same whether or not the email has an account, so the screen
     * does not reveal which emails have accounts. The password is replaced only after the email
     * is sent, so a failed send never locks the user out of their current password.
     */
    public void requestPasswordReset(String email) {
        if (isBlank(email)) {
            throw new RuleViolationException("Enter your email address.");
        }
        Account account = Database.read("Could not load the account", c -> accountDao.findByEmail(c, email.trim()));
        if (account == null) return;

        String temporaryPassword = generateTemporaryPassword();
        if (!emailService.sendPasswordReset(account.email, temporaryPassword)) return;   // logged in email_log
        try (Connection c = Database.getConnection()) {
            accountDao.setTemporaryPassword(c, account.id, PasswordHasher.hash(temporaryPassword));
        } catch (SQLException e) {
            throw new DataAccessException("Could not reset the password", e);
        }
    }

    /** A random temporary password for an approved registration (R1.5) or a password reset. The caller hashes and emails it. */
    public String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /** The hash is read only when needed and never kept on the Account (see Account). */
    private String loadPasswordHash(int accountId) {
        return Database.read("Could not load the account", c -> accountDao.findPasswordHash(c, accountId));
    }

    /** An account and its password hash, read together at login. */
    private record Credentials(Account account, String hash) { }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}