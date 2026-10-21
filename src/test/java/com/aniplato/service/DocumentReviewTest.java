package com.aniplato.service;

import com.aniplato.model.Document;
import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R1.4 document verification, and the document lists the Profile screen shows. */
class DocumentReviewTest extends DbTest {

    private final RegistrationService registrations = new RegistrationService();

    private int pendingDocument() {
        return ((Number) TestDb.scalar("SELECT document_id FROM document WHERE registration_id = ?",
                registrationId(PENDING))).intValue();
    }

    @Test
    void verifyMarksTheDocumentOkAndLogsWhoDidIt() {
        int adminId = loginAs(ADMIN).id;
        int doc = pendingDocument();

        registrations.verifyDocument(doc);

        assertEquals("OK", TestDb.scalar("SELECT review FROM document WHERE document_id = ?", doc));
        assertEquals((long) adminId, TestDb.count("SELECT reviewed_by FROM document WHERE document_id = ?", doc));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'DOCUMENT' AND entity_id = ? "
                + "AND from_status = 'AWAITING_REVIEW' AND to_status = 'OK' AND changed_by = ?", doc, adminId));
    }

    @Test
    void undoPutsTheDocumentBackToAwaitingReview() {
        loginAs(ADMIN);
        int doc = pendingDocument();
        registrations.verifyDocument(doc);

        registrations.undoVerification(doc);

        assertNull(TestDb.scalar("SELECT review FROM document WHERE document_id = ?", doc));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'DOCUMENT' AND entity_id = ? "
                + "AND from_status = 'OK' AND to_status = 'AWAITING_REVIEW'", doc));
    }

    @Test
    void undoNeedsAVerifiedDocumentAndTheDocumentMustExist() {
        loginAs(ADMIN);
        assertRule("This document is not verified.", () -> registrations.undoVerification(pendingDocument()));
        assertRule("This document no longer exists.", () -> registrations.verifyDocument(99999));
    }

    @Test
    void onlyAnAdminReviewsDocuments() {
        loginAs(BUSINESS);
        assertRule("Only an admin can do this.", () -> registrations.verifyDocument(pendingDocument()));
    }

    @Test
    void myDocumentsAreTheLatestUploadOfEachType() {
        loginAs(BUSINESS);
        assertEquals(2, registrations.getMyDocuments().size());

        // a renewal is a new row; only the newest of that type is listed
        TestDb.exec("INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at) "
                + "SELECT registration_id, 'mayors_permit', 'renewed.pdf', DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NULL, "
                + "DATE_ADD(NOW(), INTERVAL 1 MINUTE) FROM registration WHERE email = ?", BUSINESS);
        List<Document> docs = registrations.getMyDocuments();

        assertEquals(2, docs.size());
        assertTrue(docs.stream().anyMatch(d -> d.type == Document.Type.MAYORS_PERMIT && d.filePath.equals("renewed.pdf")));
    }

    @Test
    void anAdminHasNoDocuments() {
        loginAs(ADMIN);
        assertTrue(registrations.getMyDocuments().isEmpty());
        assertTrue(registrations.getCurrentDocumentAlerts(LocalDate.now()).isEmpty());
    }

    @Test
    void alertsListVerifiedDocumentsEndingWithinThirtyDaysOrAlreadyEnded() {
        loginAs(BUSINESS);
        LocalDate today = LocalDate.now();
        assertTrue(registrations.getCurrentDocumentAlerts(today).isEmpty());

        TestDb.exec("UPDATE document SET expires_on = ? WHERE registration_id = (SELECT registration_id FROM registration WHERE email = ?) "
                + "AND doc_type = 'sanitary_permit'", today.plusDays(10), BUSINESS);
        List<Document> soon = registrations.getCurrentDocumentAlerts(today);
        assertEquals(1, soon.size());
        assertEquals(Document.Type.SANITARY_PERMIT, soon.get(0).type);

        TestDb.exec("UPDATE document SET expires_on = ? WHERE registration_id = (SELECT registration_id FROM registration WHERE email = ?) "
                + "AND doc_type = 'mayors_permit'", today.minusDays(3), BUSINESS);
        assertEquals(2, registrations.getCurrentDocumentAlerts(today).size());
    }
}
