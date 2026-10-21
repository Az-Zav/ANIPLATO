package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 6.6: the automatic checks. One test per check as the checks get built. */
class MaintenanceServiceTest extends DbTest {

    private final MaintenanceService maintenance = new MaintenanceService();

    private String status(String email) {
        return TestDb.scalar("SELECT status FROM account WHERE email = ?", email);
    }

    private void expireBusinessPermit() {
        TestDb.exec("UPDATE document SET expires_on = ? WHERE doc_type = 'mayors_permit'", LocalDate.now().minusDays(1));
    }

    @Test
    void anExpiredVerifiedDocumentSuspendsItsAccountWithNobodyLoggedIn() {
        expireBusinessPermit();

        maintenance.runMaintenance();

        assertEquals("SUSPENDED", status(BUSINESS));
        assertEquals("Document expired", TestDb.scalar("SELECT suspend_reason FROM account WHERE email = ?", BUSINESS));
        assertEquals("ACTIVE", status(CASUAL));
        assertEquals("ACTIVE", status(GROUP));
    }

    @Test
    void runningItTwiceChangesNothingTheSecondTime() {
        expireBusinessPermit();
        maintenance.runMaintenance();
        long logged = TestDb.count("SELECT COUNT(*) FROM status_change");

        maintenance.runMaintenance();

        assertEquals(logged, TestDb.count("SELECT COUNT(*) FROM status_change"));
    }

    @Test
    void whenNothingHasExpiredNothingChanges() {
        maintenance.runMaintenance();

        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM status_change"));
        assertEquals(3L, TestDb.count("SELECT COUNT(*) FROM account WHERE status = 'ACTIVE' AND role <> 'ADMIN'"));
    }
}
