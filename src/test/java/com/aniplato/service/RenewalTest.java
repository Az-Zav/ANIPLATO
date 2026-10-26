package com.aniplato.service;

import com.aniplato.model.Document;
import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 6.3: an organization renews a document, the admin verifies it, and a document-expiry suspension ends. */
class RenewalTest extends DbTest {

    private final RegistrationService documents = new RegistrationService();
    private final MaintenanceService maintenance = new MaintenanceService();

    @TempDir
    Path tmp;

    private File pdf(String name) {
        try {
            return Files.write(tmp.resolve(name), new byte[]{1, 2, 3}).toFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private int uploadsInFolder() {
        return new File(System.getProperty("aniplato.uploads.dir")).list().length;
    }

    private String accountStatus(String email) {
        return TestDb.scalar("SELECT status FROM account WHERE email = ?", email);
    }

    private int newestDocument(String docType) {
        return ((Number) TestDb.scalar("SELECT MAX(document_id) FROM document WHERE doc_type = ?", docType)).intValue();
    }

    private void expire(String docType) {
        TestDb.exec("UPDATE document SET expires_on = DATE_SUB(CURDATE(), INTERVAL 1 DAY) WHERE doc_type = ?", docType);
    }

    //---UPLOADING---

    @Test
    void aRenewalIsANewRowAwaitingReviewAndTheOldRowIsUntouched() {
        int before = ((Number) TestDb.scalar("SELECT MAX(document_id) FROM document WHERE doc_type = 'mayors_permit'")).intValue();
        int accountId = loginAs(BUSINESS).id;
        LocalDate next = LocalDate.now().plusYears(1);

        documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("permit.pdf"), next, false);

        int renewal = newestDocument("mayors_permit");
        assertTrue(renewal > before);
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM document WHERE document_id = ? AND review IS NULL AND expires_on = ?", renewal, next));
        assertEquals("OK", TestDb.scalar("SELECT review FROM document WHERE document_id = ?", before));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'DOCUMENT' AND entity_id = ? "
                + "AND to_status = 'AWAITING_REVIEW' AND changed_by = ?", renewal, accountId));
    }

    @Test
    void aRenewalNeedsAnExistingTypeAFutureDateAndAGoodFile() {
        loginAs(BUSINESS);
        LocalDate next = LocalDate.now().plusYears(1);
        int files = uploadsInFolder();

        assertRule("Your registration has no DSWD Accreditation to renew.", () -> documents.uploadRenewal(Document.Type.DSWD, pdf("a.pdf"), next, false));
        assertRule("Enter the expiry date of your Mayor's Permit.", () -> documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("b.pdf"), null, false));
        assertRule("Enter the expiry date of your Mayor's Permit.", () -> documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("b.pdf"), null, true));
        assertRule("Your Mayor's Permit has expired. Upload a current one.", () -> documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("c.pdf"), LocalDate.now().minusDays(1), false));
        assertRule("Upload a PDF, JPG or PNG file.", () -> documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("notes.txt"), next, false));
        assertEquals(files, uploadsInFolder(), "a refused renewal leaves no file behind");
    }

    @Test
    void aDocumentWithNoExpiryMayBeRenewedWithoutADate() {
        loginAs(GROUP);
        documents.uploadRenewal(Document.Type.AUTHORIZATION_LETTER, pdf("letter.pdf"), null, true);
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM document WHERE doc_type = 'authorization_letter' AND review IS NULL AND expires_on IS NULL"));
    }

    @Test
    void onlyAnOrganizationRenews() {
        loginAs(ADMIN);
        assertRule("Only an organization can upload a renewal.", () -> documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("a.pdf"), LocalDate.now().plusDays(5), false));
    }

    //---VERIFYING---

    @Test
    void verifyingTheRenewalEndsADocumentExpirySuspension() {
        expire("mayors_permit");
        maintenance.runMaintenance();
        assertEquals("SUSPENDED", accountStatus(BUSINESS));
        loginAs(BUSINESS);   // a Suspended account may still renew
        documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("permit.pdf"), LocalDate.now().plusYears(1), false);
        int renewal = newestDocument("mayors_permit");
        assertEquals("SUSPENDED", accountStatus(BUSINESS), "uploading is not enough");

        loginAs(ADMIN);
        documents.verifyDocument(renewal);

        assertEquals("ACTIVE", accountStatus(BUSINESS));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'ACCOUNT' AND to_status = 'ACTIVE' "
                + "AND reason = 'Renewal verified' AND changed_by IS NULL"));
        maintenance.runMaintenance();
        assertEquals("ACTIVE", accountStatus(BUSINESS), "the next check does not suspend it again");
    }

    @Test
    void anAccountStaysSuspendedWhileAnotherDocumentIsStillExpired() {
        expire("mayors_permit");
        expire("sanitary_permit");
        maintenance.runMaintenance();
        loginAs(BUSINESS);
        documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("a.pdf"), LocalDate.now().plusYears(1), false);
        int first = newestDocument("mayors_permit");
        documents.uploadRenewal(Document.Type.SANITARY_PERMIT, pdf("b.pdf"), LocalDate.now().plusYears(1), false);
        int second = newestDocument("sanitary_permit");

        loginAs(ADMIN);
        documents.verifyDocument(first);
        assertEquals("SUSPENDED", accountStatus(BUSINESS));
        documents.verifyDocument(second);
        assertEquals("ACTIVE", accountStatus(BUSINESS));
    }

    @Test
    void aSuspensionForAnotherReasonIsNotLiftedByAVerification() {
        TestDb.exec("UPDATE account SET status = 'SUSPENDED', suspend_reason = 'Unsafe food reported' WHERE email = ?", BUSINESS);
        loginAs(BUSINESS);
        documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("a.pdf"), LocalDate.now().plusYears(1), false);
        int renewal = newestDocument("mayors_permit");
        loginAs(ADMIN);
        documents.verifyDocument(renewal);
        assertEquals("SUSPENDED", accountStatus(BUSINESS));
    }

    //---THE ADMIN'S WAY TO A RENEWAL---

    @Test
    void theAccountsListPointsToARenewalWaitingForReviewAndTheReviewShowsIt() {
        loginAs(BUSINESS);
        documents.uploadRenewal(Document.Type.MAYORS_PERMIT, pdf("a.pdf"), LocalDate.now().plusYears(1), false);
        int renewal = newestDocument("mayors_permit");
        loginAs(ADMIN);

        var row = new RegistrationService().getAccounts(LocalDate.now()).rows().stream()
                .filter(r -> r.name().equals("Sunrise Bakery")).findFirst().orElseThrow();
        assertTrue(row.renewalToReview());
        assertTrue(new RegistrationService().getAccounts(LocalDate.now()).rows().stream()
                .filter(r -> !r.name().equals("Sunrise Bakery")).noneMatch(r -> r.renewalToReview()));

        var shown = new RegistrationService().getForReview(row.registrationId()).registration().documents;
        assertEquals(2, shown.size(), "one file of each type: the renewal replaces the old mayor's permit");
        assertTrue(shown.stream().anyMatch(d -> d.id == renewal));

        documents.verifyDocument(renewal);
        assertTrue(new RegistrationService().getAccounts(LocalDate.now()).rows().stream().noneMatch(r -> r.renewalToReview()));
    }
}
