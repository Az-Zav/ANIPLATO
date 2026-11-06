package com.aniplato.support;

import com.aniplato.dao.AccountDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.util.Database;
import com.aniplato.util.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Base class: a fresh seeded aniplato_test and nobody logged in before each test. */
public abstract class DbTest {

    public static final String PASSWORD = "Aniplato123!";   // every seed account
    public static final String ADMIN = "admin@aniplato.test";
    public static final String BUSINESS = "business@aniplato.test";
    public static final String CASUAL = "casual@aniplato.test";
    public static final String GROUP = "group@aniplato.test";
    public static final String PENDING = "pending@aniplato.test";   // a registration, not an account

    @BeforeEach
    void freshDatabase() {
        Session.clear();
        TestDb.reset();
    }

    @AfterEach
    void logOut() {
        Session.clear();
    }

    /** Starts a session for a seeded account without going through the login rules. */
    protected static Account loginAs(String email) {
        try (Connection c = Database.getConnection()) {
            Account account = new AccountDao().findByEmail(c, email);
            Session.start(account);
            return account;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static int accountId(String email) {
        return ((Number) TestDb.scalar("SELECT account_id FROM account WHERE email = ?", email)).intValue();
    }

    protected static int registrationId(String email) {
        return ((Number) TestDb.scalar("SELECT registration_id FROM registration WHERE email = ?", email)).intValue();
    }

    /** Asserts the code throws the rule message shown to the user. */
    protected static void assertRule(String expectedMessage, Executable code) {
        RuleViolationException e = assertThrows(RuleViolationException.class, code);
        assertEquals(expectedMessage, e.getMessage());
    }

    /** Asserts the code is refused with a rule message, whatever its wording. */
    protected static void assertRefused(Executable code) {
        assertThrows(RuleViolationException.class, code);
    }
}
