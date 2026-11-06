package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 6.3 suspend and reinstate, the All Accounts list, and the 6.6 document-expiry suspension. */
class AccountStatusTest extends DbTest {

    private final AccountStatusService accounts = new AccountStatusService();

    private String status(String email) {
        return TestDb.scalar("SELECT status FROM account WHERE email = ?", email);
    }

    //---SUSPEND AND REINSTATE---

    @Test
    void suspendStoresTheReasonAndLogsIt() {
        int adminId = loginAs(ADMIN).id;
        int business = accountId(BUSINESS);

        accounts.suspendAccount(business, "  Reported by two food groups  ");

        assertEquals("SUSPENDED", status(BUSINESS));
        assertEquals("Reported by two food groups", TestDb.scalar("SELECT suspend_reason FROM account WHERE account_id = ?", business));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT' AND entity_id = ? "
                + "AND from_status = 'ACTIVE' AND to_status = 'SUSPENDED' AND changed_by = ?", business, adminId));
    }

    @Test
    void suspendNeedsAReasonAndAnActiveAccount() {
        loginAs(ADMIN);
        int business = accountId(BUSINESS);
        assertRule("Write the reason for the suspension.", () -> accounts.suspendAccount(business, " "));
        accounts.suspendAccount(business, "first");
        assertRule("This account is not Active.", () -> accounts.suspendAccount(business, "second"));
    }

    @Test
    void reinstateGivesAccessBackAndLogsIt() {
        loginAs(ADMIN);
        int business = accountId(BUSINESS);
        accounts.suspendAccount(business, "test");

        accounts.reinstateAccount(business);

        assertEquals("ACTIVE", status(BUSINESS));
        assertNull(TestDb.scalar("SELECT suspend_reason FROM account WHERE account_id = ?", business));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT' AND entity_id = ? "
                + "AND from_status = 'SUSPENDED' AND to_status = 'ACTIVE'", business));
        assertRule("This account is not Suspended.", () -> accounts.reinstateAccount(business));
    }

    @Test
    void onlyAnAdminSuspendsOrReinstates() {
        loginAs(GROUP);
        int business = accountId(BUSINESS);
        assertRule("Only an admin can do this.", () -> accounts.suspendAccount(business, "x"));
        assertRule("Only an admin can do this.", () -> accounts.reinstateAccount(business));
        assertEquals("ACTIVE", status(BUSINESS));
    }

    //---THE ALL ACCOUNTS LIST---

    @Test
    void theAccountListCountsAreWorkedOutFromTheRows() {
        loginAs(ADMIN);
        LocalDate today = LocalDate.now();

        var overview = accounts.getAccounts(today);
        assertEquals(3, overview.rows().size(), "the admin is not an organization");
        assertEquals(3, overview.approved());
        assertEquals(0, overview.suspended());
        assertEquals(0, overview.documentsEnding());

        accounts.suspendAccount(accountId(GROUP), "test");
        TestDb.exec("UPDATE document SET expires_on = ? WHERE doc_type = 'sanitary_permit'", today.plusDays(10));

        var after = accounts.getAccounts(today);
        assertEquals(1, after.suspended());
        assertEquals(1, after.documentsEnding());
        var business = after.rows().stream().filter(r -> r.name().equals("Sunrise Bakery")).findFirst().orElseThrow();
        assertTrue(business.documentEnding());
        assertTrue(business.documentNote().startsWith("Sanitary Permit ends "), business.documentNote());
    }

    //---THE 6.6 EXPIRY SWEEP---

    private void expireBusinessPermit(String reviewValue) {
        TestDb.exec("UPDATE document SET expires_on = ?, review = " + reviewValue + " WHERE doc_type = 'mayors_permit'",
                LocalDate.now().minusDays(1));
    }

    @Test
    void anExpiredVerifiedDocumentSuspendsItsAccountAsTheSystem() {
        expireBusinessPermit("'OK'");

        accounts.suspendAccountsWithExpiredDocuments(LocalDate.now());

        assertEquals("SUSPENDED", status(BUSINESS));
        assertEquals("Document expired", TestDb.scalar("SELECT suspend_reason FROM account WHERE email = ?", BUSINESS));
        assertEquals("ACTIVE", status(CASUAL));
        assertEquals("ACTIVE", status(GROUP));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT' "
                + "AND to_status = 'SUSPENDED' AND changed_by IS NULL AND reason = 'Document expired'"));
    }

    @Test
    void aNewerVerifiedRenewalKeepsTheAccountActive() {
        expireBusinessPermit("'OK'");
        TestDb.exec("INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at) "
                + "SELECT registration_id, 'mayors_permit', 'renewed.pdf', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', "
                + "DATE_ADD(NOW(), INTERVAL 1 MINUTE) FROM registration WHERE email = ?", BUSINESS);

        accounts.suspendAccountsWithExpiredDocuments(LocalDate.now());

        assertEquals("ACTIVE", status(BUSINESS));
    }

    @Test
    void anUnverifiedOrFlaggedExpiredDocumentDoesNotSuspend() {
        expireBusinessPermit("NULL");
        accounts.suspendAccountsWithExpiredDocuments(LocalDate.now());
        assertEquals("ACTIVE", status(BUSINESS));

        expireBusinessPermit("'FLAGGED'");
        accounts.suspendAccountsWithExpiredDocuments(LocalDate.now());
        assertEquals("ACTIVE", status(BUSINESS));
    }

    @Test
    void theSweepIsQuietWhenNothingHasExpiredAndSkipsSuspendedAccounts() {
        accounts.suspendAccountsWithExpiredDocuments(LocalDate.now());
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT'"));

        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'manual' WHERE email = ?", BUSINESS);
        expireBusinessPermit("'OK'");
        accounts.suspendAccountsWithExpiredDocuments(LocalDate.now());
        assertEquals("manual", TestDb.scalar("SELECT suspend_reason FROM account WHERE email = ?", BUSINESS));
        assertFalse(TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT'") > 0);
    }
}
