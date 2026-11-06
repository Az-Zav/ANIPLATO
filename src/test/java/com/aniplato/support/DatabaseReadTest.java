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
        assertEquals(TestDb.NAME, Database.read("Could not read", c -> TestDb.scalar("SELECT DATABASE()")));
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

    // ---------- the pool ----------

    /** How many connections the server has ever accepted, read on a connection of its own (not the pool's). */
    private static long serverConnections() throws SQLException {
        try (java.sql.Connection c = java.sql.DriverManager.getConnection(System.getProperty("aniplato.url"),
                System.getProperty("aniplato.user"), System.getProperty("aniplato.password"));
             java.sql.Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SHOW GLOBAL STATUS LIKE 'Connections'")) {
            rs.next();
            return rs.getLong(2);
        }
    }

    @Test
    void manyCallsShareAFewConnectionsInsteadOfOpeningOneEach() throws SQLException {
        Database.read("warm the pool", c -> TestDb.scalar("SELECT 1"));
        long before = serverConnections();
        for (int i = 0; i < 25; i++) Database.read("Could not read", c -> TestDb.scalar("SELECT 1"));
        long opened = serverConnections() - before - 1;   // minus the one this check opened itself
        assertEquals(0, opened, "25 reads opened " + opened + " new connections");
    }

    @Test
    void aTransactionReturnsItsConnectionToAutocommitForTheNextUser() {
        Database.inTransaction("Could not save", c -> { TestDb.scalar("SELECT 1"); return null; });
        boolean autocommit = Database.read("Could not read", c -> c.getAutoCommit());
        assertEquals(true, autocommit, "a pooled connection comes back in its normal mode");
    }

    @Test
    void closingThePoolLetsTheNextCallOpenFreshConnections() {
        Database.closePool();
        assertEquals(TestDb.NAME, Database.read("Could not read", c -> TestDb.scalar("SELECT DATABASE()")));
    }
}
