package com.aniplato.support;

import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.util.Database;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Database.read: the value comes back, a database error is wrapped with the caller's text, a rule error passes through. */
class DatabaseReadTest extends DbTest {

    @Test
    void returnsWhatTheWorkReturns() {
        assertEquals("aniplato_test", Database.read("Could not read", c -> TestDb.scalar("SELECT DATABASE()")));
    }

    @Test
    void wrapsADatabaseErrorWithTheGivenMessage() {
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> Database.read("Could not read", c -> { throw new SQLException("boom"); }));
        assertEquals("Could not read", e.getMessage());
    }

    @Test
    void letsARuleErrorThroughUnchanged() {
        assertThrows(RuleViolationException.class,
                () -> Database.read("Could not read", c -> { throw new RuleViolationException("No."); }));
    }
}
