package com.aniplato.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDbTest extends DbTest {

    @Test
    void theSeedIsLoadedIntoTheTestDatabaseOnly() {
        assertEquals(TestDb.NAME, TestDb.scalar("SELECT DATABASE()"));
        assertEquals(4L, TestDb.count("SELECT COUNT(*) FROM account"));
        assertEquals(4L, TestDb.count("SELECT COUNT(*) FROM registration"));
    }
}
