package com.aniplato.controller;

import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void anythingElseIsNotHidden() {
        assertThrows(NullPointerException.class, () -> FormSupport.failure(() -> { throw new NullPointerException(); }, "db"));
    }
}
