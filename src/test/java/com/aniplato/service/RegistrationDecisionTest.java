package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 1, deciding: the admin approves or rejects a Pending registration (R1.4-R1.6, A2, A3). */
class RegistrationDecisionTest extends DbTest {

    private final RegistrationService registrations = new RegistrationService();
    private final DocumentReviewService documents = new DocumentReviewService();
    private int pending;      // Ana Cruz, a Casual donor with one unreviewed Barangay Certificate
    private int adminId;

    @BeforeEach
    void admin() {
        adminId = loginAs(ADMIN).id;
        pending = registrationId(PENDING);
    }

    private int pendingDocument() {
        return ((Number) TestDb.scalar("SELECT document_id FROM document WHERE registration_id = ?", pending)).intValue();
    }

    private void verifyIt() {
        documents.verifyDocument(pendingDocument());
    }

    private String status(int registrationId) {
        return TestDb.scalar("SELECT status FROM registration WHERE registration_id = ?", registrationId);
    }

    //---APPROVE---

    @Test
    void approveCreatesTheAccountWithATemporaryPasswordAndLogsEveryChange() {
        verifyIt();

        boolean emailSent = registrations.approve(pending);

        assertFalse(emailSent, "the test config has no SMTP settings");
        assertEquals("APPROVED", status(pending));
        assertEquals((long) adminId, TestDb.count("SELECT reviewed_by FROM registration WHERE registration_id = ?", pending));
        assertEquals("CASUAL_DONOR", TestDb.scalar("SELECT role FROM account WHERE email = ?", PENDING));
        assertEquals("ACTIVE", TestDb.scalar("SELECT status FROM account WHERE email = ?", PENDING));
        assertEquals(1L, TestDb.count("SELECT must_change_password FROM account WHERE email = ?", PENDING));
        assertTrue(((String) TestDb.scalar("SELECT password_hash FROM account WHERE email = ?", PENDING)).startsWith("$2"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'REGISTRATION' "
                + "AND entity_id = ? AND from_status = 'PENDING' AND to_status = 'APPROVED' AND changed_by = ?", pending, adminId));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT' "
                + "AND to_status = 'ACTIVE' AND from_status IS NULL"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM email_log WHERE kind = 'APPROVED' AND to_email = ?", PENDING));
    }

    @Test
    void approveNeedsEveryDocumentVerifiedFirst() {
        assertRule("Verify the Barangay Certificate before approving.", () -> registrations.approve(pending));
        assertEquals("PENDING", status(pending));
    }

    @Test
    void approveRefusesAFlaggedDocument() {
        TestDb.exec("UPDATE document SET review = 'FLAGGED', flag_reason = 'Name does not match' WHERE document_id = ?", pendingDocument());
        assertRule("The Barangay Certificate is flagged. Reject the registration instead.", () -> registrations.approve(pending));
        assertEquals("PENDING", status(pending));
    }

    @Test
    void approveRefusesAnExpiredDocument() {
        verifyIt();
        TestDb.exec("UPDATE document SET expires_on = ? WHERE document_id = ?", LocalDate.now().minusDays(1), pendingDocument());
        assertRule("The Barangay Certificate has expired. Reject the registration instead.", () -> registrations.approve(pending));
        assertEquals("PENDING", status(pending));
    }

    @Test
    void aRegistrationCanBeDecidedOnlyOnce() {
        verifyIt();
        registrations.approve(pending);
        assertRule("This registration was already reviewed.", () -> registrations.approve(pending));
        assertRule("This registration was already reviewed.", () -> registrations.reject(pending, "too late"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM account WHERE email = ?", PENDING));
    }

    @Test
    void onlyAnAdminCanDecide() {
        verifyIt();
        loginAs(BUSINESS);
        assertRule("Only an admin can do this.", () -> registrations.approve(pending));
        assertRule("Only an admin can do this.", () -> registrations.reject(pending, "no"));
        assertEquals("PENDING", status(pending));
    }

    @Test
    void approvingAnUpgradeMovesTheCasualAccountToTheBusinessRegistration() {
        int casual = accountId(CASUAL);
        int upgrade = pendingUpgrade(CASUAL, "BUSINESS_DONOR");
        TestDb.exec("INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at) VALUES "
                + "(?, 'mayors_permit', 'x.pdf', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW()), "
                + "(?, 'sanitary_permit', 'y.pdf', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW())", upgrade, upgrade);

        boolean result = registrations.approve(upgrade);

        assertTrue(result, "an upgrade keeps its password, so there is no email to fail");
        assertEquals("BUSINESS_DONOR", TestDb.scalar("SELECT role FROM account WHERE account_id = ?", casual));
        assertEquals((long) upgrade, TestDb.count("SELECT registration_id FROM account WHERE account_id = ?", casual));
        assertEquals(4L, TestDb.count("SELECT COUNT(*) FROM account"), "no second account is created");
    }

    @Test
    void aFailedApprovalRollsBackEverythingItDid() {
        // this email already belongs to a Business donor, so approve gets as far as the account step and refuses
        int clash = pendingUpgrade(BUSINESS, "BUSINESS_DONOR");
        TestDb.exec("INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at) VALUES "
                + "(?, 'mayors_permit', 'x.pdf', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW())", clash);

        assertRule("This email already has an account. Reject this registration.", () -> registrations.approve(clash));

        assertEquals("PENDING", status(clash));
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'REGISTRATION' AND entity_id = ?", clash));
    }

    /** A second Pending registration for an email that already has an account. */
    private int pendingUpgrade(String email, String role) {
        TestDb.exec("INSERT INTO registration (role, name, city_id, contact_person, contact_phone, email, status, submitted_at) "
                + "SELECT ?, 'Upgrade Org', city_id, 'Up Grade', '09171234567', ?, 'PENDING', NOW() FROM city LIMIT 1", role, email);
        return ((Number) TestDb.scalar("SELECT MAX(registration_id) FROM registration")).intValue();
    }

    //---REJECT---

    @Test
    void rejectSavesTheReasonCreatesNoAccountAndLogsIt() {
        boolean emailSent = registrations.reject(pending, "  The certificate is blurry.  ");

        assertFalse(emailSent, "the test config has no SMTP settings");
        assertEquals("REJECTED", status(pending));
        assertEquals("The certificate is blurry.", TestDb.scalar("SELECT rejection_reason FROM registration WHERE registration_id = ?", pending));
        assertEquals((long) adminId, TestDb.count("SELECT reviewed_by FROM registration WHERE registration_id = ?", pending));
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM account WHERE email = ?", PENDING));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'REGISTRATION' AND entity_id = ? "
                + "AND to_status = 'REJECTED' AND reason = 'The certificate is blurry.'", pending));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM email_log WHERE kind = 'NOT_APPROVED' AND to_email = ?", PENDING));
    }

    @Test
    void rejectNeedsAReasonOfAtMost500Characters() {
        assertRule("Write the reason for rejecting.", () -> registrations.reject(pending, "   "));
        assertRule("Write the reason for rejecting.", () -> registrations.reject(pending, null));
        assertRule("Keep the reason under 500 characters.", () -> registrations.reject(pending, "x".repeat(501)));
        registrations.reject(pending, "x".repeat(500));
        assertEquals("REJECTED", status(pending));
    }

    //---THE REVIEW QUEUE---

    @Test
    void thePendingListHoldsOnlyPendingRegistrations() {
        var rows = registrations.getPending();
        assertEquals(1, rows.size());
        assertEquals("Ana Cruz", rows.get(0).name());
        assertFalse(rows.get(0).registeredAgain());

        registrations.reject(pending, "no");
        assertEquals(0, registrations.getPending().size());
    }

    @Test
    void anApplicantRejectedBeforeIsMarkedRegisteredAgain() {
        registrations.reject(pending, "no");
        int again = pendingUpgrade(PENDING, "CASUAL_DONOR");
        TestDb.exec("UPDATE registration SET submitted_at = DATE_ADD(NOW(), INTERVAL 1 MINUTE) WHERE registration_id = ?", again);

        var rows = registrations.getPending();

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).registeredAgain());
    }

    @Test
    void reviewShowsTheRegistrationWithItsDocuments() {
        var detail = registrations.getForReview(pending);
        assertEquals("Ana Cruz", detail.registration().name);
        assertEquals(1, detail.registration().documents.size());
        assertRule("This registration no longer exists.", () -> registrations.getForReview(99999));
    }

    @Test
    void onlyAnAdminSeesTheQueue() {
        loginAs(GROUP);
        assertRule("Only an admin can do this.", registrations::getPending);
        assertRule("Only an admin can do this.", () -> registrations.getForReview(pending));
    }
}
