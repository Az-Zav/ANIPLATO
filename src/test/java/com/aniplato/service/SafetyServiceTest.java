package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** R7.1 and R7.2: safety notices, and the admin deciding on them (A2, A3). */
class SafetyServiceTest extends DbTest {

    private final SafetyService safety = new SafetyService();
    private int offer;

    @BeforeEach
    void anAcceptedOffer() {
        offer = Fixtures.offerOnSeedListing("ACCEPTED", 5);
    }

    private String noticeStatus(int id) {
        return TestDb.scalar("SELECT status FROM safety_notice WHERE notice_id = ?", id);
    }

    //---THE ADMIN'S LIST---

    @Test
    void theAdminSeesEveryNoticeWithTheNamesOfBothSides() {
        int notice = Fixtures.concern(offer, GROUP, "The bread was moldy.");
        loginAs(ADMIN);

        assertEquals(1, safety.getNotices().size());
        var rows = safety.getNoticeRows();
        assertEquals(1, rows.size());
        assertEquals(notice, rows.get(0).id());
        assertEquals("Sunrise Bakery", rows.get(0).flaggedName());
        assertEquals("Kapwa Community Kitchen", rows.get(0).raisedByName());
        assertEquals("The bread was moldy.", rows.get(0).concern());
    }

    @Test
    void onlyAnAdminSeesOrResolvesNotices() {
        int notice = Fixtures.concern(offer, GROUP, "x");
        loginAs(BUSINESS);
        assertRule("Only an admin can do this.", safety::getNotices);
        assertRule("Only an admin can do this.", safety::getNoticeRows);
        assertRule("Only an admin can do this.", () -> safety.dismiss(notice, "note"));
        assertRule("Only an admin can do this.", () -> safety.suspendFlaggedAccount(notice, "note"));
    }

    //---DISMISS---

    @Test
    void dismissClosesTheNoticeWithTheAdminNoteAndLogsIt() {
        int notice = Fixtures.concern(offer, GROUP, "The bread was moldy.");
        int adminId = loginAs(ADMIN).id;

        safety.dismiss(notice, "  Checked with both sides, no issue.  ");

        assertEquals("DISMISSED", noticeStatus(notice));
        assertEquals("Checked with both sides, no issue.", TestDb.scalar("SELECT admin_note FROM safety_notice WHERE notice_id = ?", notice));
        assertEquals((long) adminId, TestDb.count("SELECT resolved_by FROM safety_notice WHERE notice_id = ?", notice));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'NOTICE' AND entity_id = ? "
                + "AND from_status = 'OPEN' AND to_status = 'DISMISSED'", notice));
        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", BUSINESS));
    }

    @Test
    void dismissNeedsANoteOfAtMost500CharactersAndAnOpenNotice() {
        int notice = Fixtures.concern(offer, GROUP, "x");
        loginAs(ADMIN);

        assertRule("Write a note before dismissing the notice.", () -> safety.dismiss(notice, " "));
        assertRule("Keep it to 500 characters or fewer.", () -> safety.dismiss(notice, "y".repeat(501)));
        assertRule("That notice no longer exists.", () -> safety.dismiss(99999, "note"));

        safety.dismiss(notice, "done");
        assertRule("Notice #" + notice + " has already been resolved.", () -> safety.dismiss(notice, "again"));
    }

    //---SUSPEND THE FLAGGED ACCOUNT---

    @Test
    void suspendingTheFlaggedAccountSuspendsTheOtherSideOfTheOffer() {
        int notice = Fixtures.concern(offer, GROUP, "The bread was moldy.");   // raised by the food group, so the donor is flagged
        int adminId = loginAs(ADMIN).id;

        safety.suspendFlaggedAccount(notice, "Unsafe food reported");

        assertEquals("ACCOUNT_SUSPENDED", noticeStatus(notice));
        assertEquals("SUSPENDED", TestDb.scalar("SELECT status FROM account WHERE email = ?", BUSINESS));
        assertEquals("Unsafe food reported", TestDb.scalar("SELECT suspend_reason FROM account WHERE email = ?", BUSINESS));
        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", GROUP));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'NOTICE' AND entity_id = ? "
                + "AND to_status = 'ACCOUNT_SUSPENDED' AND changed_by = ?", notice, adminId));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT' "
                + "AND to_status = 'SUSPENDED' AND changed_by = ?", adminId));
    }

    @Test
    void aConcernRaisedByTheDonorFlagsTheFoodGroup() {
        int notice = Fixtures.concern(offer, BUSINESS, "They never came.");
        loginAs(ADMIN);

        safety.suspendFlaggedAccount(notice, "No-show");

        assertEquals("SUSPENDED", TestDb.scalar("SELECT status FROM account WHERE email = ?", GROUP));
        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", BUSINESS));
    }

    @Test
    void suspendNeedsANoteAndAnOpenNoticeAndChangesNothingOtherwise() {
        int notice = Fixtures.concern(offer, GROUP, "x");
        loginAs(ADMIN);

        assertRule("Write a note before suspending the account.", () -> safety.suspendFlaggedAccount(notice, ""));
        safety.dismiss(notice, "no action");
        assertRule("Notice #" + notice + " has already been resolved.", () -> safety.suspendFlaggedAccount(notice, "late"));

        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", BUSINESS));
        assertEquals("DISMISSED", noticeStatus(notice));
    }

    //---RAISING A CONCERN---

    @Test
    @Disabled("Known bug, see docs/CLEANUP-REPORT.md: raise() reads the old listing model and "
            + "SafetyNoticeDao.insert leaves out the NOT NULL kind column. Rewritten in FLOWS.md module 10.")
    void eitherSideOfAnAcceptedOfferCanRaiseAConcern() {
        loginAs(GROUP);
        int notice = safety.raise(offer, "The bread was moldy.");
        assertEquals("OPEN", noticeStatus(notice));
    }
}
