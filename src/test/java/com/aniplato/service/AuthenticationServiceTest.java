package com.aniplato.service;

import com.aniplato.model.Account;
import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import com.aniplato.util.PasswordHasher;
import com.aniplato.util.Session;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R1.7: login, temporary passwords, change password, forgot password. */
class AuthenticationServiceTest extends DbTest {

    private final AuthenticationService auth = new AuthenticationService();

    @Test
    void loginStartsASessionForTheAccount() {
        Account account = auth.login(ADMIN, PASSWORD);
        assertEquals(Account.Role.ADMIN, account.role);
        assertEquals(account.id, Session.require().id);
    }

    @Test
    void loginIgnoresSpacesAroundTheEmail() {
        assertEquals(Account.Role.FOOD_GROUP, auth.login("  " + GROUP + " ", PASSWORD).role);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() {
        assertRule("Wrong email or password.", () -> auth.login(ADMIN, "not-the-password"));
        assertRule("Wrong email or password.", () -> auth.login("nobody@aniplato.test", PASSWORD));
        assertFalse(Session.isLoggedIn());
    }

    @Test
    void loginNeedsBothFields() {
        assertRule("Enter your email and password.", () -> auth.login("", PASSWORD));
        assertRule("Enter your email and password.", () -> auth.login(ADMIN, " "));
        assertRule("Enter your email and password.", () -> auth.login(null, null));
    }

    @Test
    void aSuspendedAccountCanStillLogIn() {
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'test' WHERE email = ?", BUSINESS);
        assertTrue(auth.login(BUSINESS, PASSWORD).isSuspended());
    }

    @Test
    void logoutClearsTheSession() {
        auth.login(ADMIN, PASSWORD);
        auth.logout();
        assertFalse(Session.isLoggedIn());
    }

    @Test
    void aTemporaryPasswordMustBeReplacedBeforeAnythingElse() {
        TestDb.exec("UPDATE account SET must_change_password = TRUE WHERE email = ?", GROUP);
        assertFalse(auth.mustChangePassword());   // nobody logged in yet
        auth.login(GROUP, PASSWORD);
        assertTrue(auth.mustChangePassword());
    }

    @Test
    void changePasswordSavesTheNewHashAndClearsTheTemporaryFlag() {
        TestDb.exec("UPDATE account SET must_change_password = TRUE WHERE email = ?", GROUP);
        auth.login(GROUP, PASSWORD);

        auth.changePassword(PASSWORD, "BrandNew123", "BrandNew123");

        assertFalse(auth.mustChangePassword());
        assertEquals(0L, TestDb.count("SELECT must_change_password FROM account WHERE email = ?", GROUP));
        String hash = TestDb.scalar("SELECT password_hash FROM account WHERE email = ?", GROUP);
        assertTrue(PasswordHasher.matches("BrandNew123", hash));
        assertFalse(PasswordHasher.matches(PASSWORD, hash));
        auth.logout();
        assertEquals(Account.Role.FOOD_GROUP, auth.login(GROUP, "BrandNew123").role);
    }

    @Test
    void changePasswordChecksEveryRuleAndChangesNothingWhenOneFails() {
        auth.login(GROUP, PASSWORD);
        String before = TestDb.scalar("SELECT password_hash FROM account WHERE email = ?", GROUP);

        assertRule("Your current password is wrong.", () -> auth.changePassword("wrong", "BrandNew123", "BrandNew123"));
        assertRule("The new password must be at least 8 characters.", () -> auth.changePassword(PASSWORD, "short1", "short1"));
        assertRule("The new password must be different from the current one.", () -> auth.changePassword(PASSWORD, PASSWORD, PASSWORD));
        assertRule("The two new passwords do not match.", () -> auth.changePassword(PASSWORD, "BrandNew123", "BrandNew124"));

        assertEquals(before, TestDb.scalar("SELECT password_hash FROM account WHERE email = ?", GROUP));
    }

    @Test
    void temporaryPasswordsAreTwelveCharactersWithoutLookAlikes() {
        for (int i = 0; i < 50; i++) {
            String password = auth.generateTemporaryPassword();
            assertEquals(12, password.length());
            assertTrue(password.matches("[A-HJ-NP-Za-km-z2-9]+"), password);
        }
    }

    @Test
    void forgotPasswordForAnUnknownEmailDoesNothingAndSaysNothing() {
        auth.requestPasswordReset("nobody@aniplato.test");
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM email_log"));
    }

    @Test
    void forgotPasswordNeedsAnEmail() {
        assertRule("Enter your email address.", () -> auth.requestPasswordReset(" "));
    }

    @Test
    void aFailedResetEmailLeavesTheCurrentPasswordWorking() {
        // the test config has no SMTP settings, so the send fails and is logged
        auth.requestPasswordReset(GROUP);

        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM email_log WHERE kind = 'PASSWORD_RESET' AND success = FALSE"));
        assertEquals(0L, TestDb.count("SELECT must_change_password FROM account WHERE email = ?", GROUP));
        assertEquals(Account.Role.FOOD_GROUP, auth.login(GROUP, PASSWORD).role);
    }
}
