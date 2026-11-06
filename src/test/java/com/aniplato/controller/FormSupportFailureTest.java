package com.aniplato.controller;

import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A8: a broken rule shows its own text, a database problem shows the caller's text, anything else is not swallowed. */
class FormSupportFailureTest {

    @Test
    void noProblemGivesNull() {
        assertNull(FormSupport.failure(() -> { }, "db"));
    }

    @Test
    void aBrokenRuleShowsItsOwnMessage() {
        assertEquals("Not allowed.", FormSupport.failure(() -> { throw new RuleViolationException("Not allowed."); }, "db"));
    }

    @Test
    void aDatabaseProblemShowsTheCallersMessage() {
        assertEquals("db", FormSupport.failure(() -> { throw new DataAccessException("down", new RuntimeException()); }, "db"));
        assertEquals("db", FormSupport.failure(() -> { throw new IllegalStateException("no connection"); }, "db"));
    }

    @Test
    void aMissingColumnOrTableSaysTheDatabaseIsOutOfDate() {
        String oldSchema = FormSupport.failure(() -> {
            throw new DataAccessException("Could not load", new java.sql.SQLSyntaxErrorException("Unknown column 'ud.food_needed'", "42S22", 1054));
        }, "db");
        assertTrue(oldSchema.startsWith("The database is out of date."), oldSchema);
        assertTrue(oldSchema.contains("migrate"), "it names what to run");
        assertTrue(FormSupport.failure(() -> {
            throw new DataAccessException("Could not load", new java.sql.SQLSyntaxErrorException("Table 'aniplato.x' doesn't exist", "42S02", 1146));
        }, "db").startsWith("The database is out of date."));
    }

    @Test
    void anyOtherDatabaseFailureKeepsTheCallersText() {
        assertEquals("db", FormSupport.failure(() -> {
            throw new DataAccessException("down", new java.sql.SQLException("Communications link failure", "08S01", 0));
        }, "db"));
        assertEquals("fallback", FormSupport.databaseProblem(new RuntimeException("x"), "fallback"));
    }

    @Test
    void anythingElseIsNotHidden() {
        assertThrows(NullPointerException.class, () -> FormSupport.failure(() -> { throw new NullPointerException(); }, "db"));
    }
}
