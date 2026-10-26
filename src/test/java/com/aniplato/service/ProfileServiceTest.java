package com.aniplato.service;

import com.aniplato.model.Document;
import com.aniplato.model.DocumentUpload;
import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 6.3: editing contact details, and a Casual donor's upgrade to Business, from submission to approval. */
class ProfileServiceTest extends DbTest {

    private final RegistrationService profile = new RegistrationService();

    @TempDir
    Path tmp;

    private File pdf(String name) {
        try {
            return Files.write(tmp.resolve(name), new byte[]{1, 2, 3}).toFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<DocumentUpload> permits() {
        LocalDate next = LocalDate.now().plusYears(1);
        return List.of(new DocumentUpload(Document.Type.MAYORS_PERMIT, pdf("mayor.pdf"), next, false),
                new DocumentUpload(Document.Type.SANITARY_PERMIT, pdf("sanitary.pdf"), next, false));
    }

    //---CONTACT DETAILS---

    @Test
    void theContactIsSavedTrimmedAndTheSessionShowsIt() {
        int registration = registrationId(BUSINESS);
        var me = loginAs(BUSINESS);

        profile.updateContact("  Maria Cruz  ", " 09171112222 ");

        assertEquals("Maria Cruz", TestDb.scalar("SELECT contact_person FROM registration WHERE registration_id = ?", registration));
        assertEquals("09171112222", TestDb.scalar("SELECT contact_phone FROM registration WHERE registration_id = ?", registration));
        assertEquals("Maria Cruz", me.registration.contactPerson);
        assertEquals(null, me.registration.contactPhone, "the session never holds the phone (A7)");
    }

    @Test
    void aBadPhoneOrAMissingPersonIsRefusedAndNothingChanges() {
        loginAs(BUSINESS);
        assertRule("The contact number must be exactly 11 digits.", () -> profile.updateContact("Maria", "0917"));
        assertRule("The contact number must be exactly 11 digits.", () -> profile.updateContact("Maria", "0917abc2222"));
        assertRule("Enter a contact number.", () -> profile.updateContact("Maria", " "));
        assertRule("Enter the contact person.", () -> profile.updateContact(" ", "09171112222"));
        assertEquals("09171234567", TestDb.scalar("SELECT contact_phone FROM registration WHERE email = ?", BUSINESS));
        loginAs(ADMIN);
        assertRule("Only an organization has contact details.", () -> profile.updateContact("A", "09171112222"));
    }

    //---UPGRADE---

    @Test
    void anUpgradeIsANewPendingBusinessRegistrationForTheSameAccount() {
        int accountId = loginAs(CASUAL).id;

        profile.submitUpgrade(permits(), true);

        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM registration WHERE email = ? AND role = 'BUSINESS_DONOR' AND status = 'PENDING' "
                + "AND name = 'Santos Household'", CASUAL));
        int upgrade = ((Number) TestDb.scalar("SELECT registration_id FROM registration WHERE email = ? AND status = 'PENDING'", CASUAL)).intValue();
        assertEquals(2L, TestDb.count("SELECT COUNT(*) FROM document WHERE registration_id = ?", upgrade));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'REGISTRATION' AND entity_id = ? "
                + "AND to_status = 'PENDING' AND changed_by = ?", upgrade, accountId));
        assertEquals("CASUAL_DONOR", TestDb.scalar("SELECT role FROM account WHERE email = ?", CASUAL));   // nothing changes until approval
    }

    @Test
    void approvingTheUpgradeMakesTheAccountABusinessDonorAndKeepsItsPassword() {
        loginAs(CASUAL);
        profile.submitUpgrade(permits(), true);
        int upgrade = ((Number) TestDb.scalar("SELECT registration_id FROM registration WHERE email = ? AND status = 'PENDING'", CASUAL)).intValue();
        String hash = TestDb.scalar("SELECT password_hash FROM account WHERE email = ?", CASUAL);

        loginAs(ADMIN);
        for (String bound : List.of("MIN", "MAX")) {
            int id = ((Number) TestDb.scalar("SELECT " + bound + "(document_id) FROM document WHERE registration_id = ?", upgrade)).intValue();
            new RegistrationService().verifyDocument(id);
        }
        new RegistrationService().approve(upgrade);

        assertEquals("BUSINESS_DONOR", TestDb.scalar("SELECT role FROM account WHERE email = ?", CASUAL));
        assertEquals((long) upgrade, TestDb.count("SELECT registration_id FROM account WHERE email = ?", CASUAL));
        assertEquals(hash, TestDb.scalar("SELECT password_hash FROM account WHERE email = ?", CASUAL));
    }

    @Test
    void onlyACasualDonorUpgradesWithBothPermitsConsentAndNoEarlierUpgradeWaiting() {
        loginAs(BUSINESS);
        assertRule("Only a Casual donor can upgrade to Business.", () -> profile.submitUpgrade(permits(), true));
        loginAs(GROUP);
        assertRule("Only a Casual donor can upgrade to Business.", () -> profile.submitUpgrade(permits(), true));

        loginAs(CASUAL);
        assertRule("Agree to the consent text to continue.", () -> profile.submitUpgrade(permits(), false));
        assertRule("Upload your Sanitary Permit.", () -> profile.submitUpgrade(permits().subList(0, 1), true));
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM registration WHERE email = ? AND status = 'PENDING'", CASUAL));

        profile.submitUpgrade(permits(), true);
        assertRule("Your upgrade is already waiting for review.", () -> profile.submitUpgrade(permits(), true));
    }

    @Test
    void aRejectedUpgradeChangesNothingAndMayBeSentAgain() {
        loginAs(CASUAL);
        profile.submitUpgrade(permits(), true);
        int upgrade = ((Number) TestDb.scalar("SELECT registration_id FROM registration WHERE email = ? AND status = 'PENDING'", CASUAL)).intValue();
        loginAs(ADMIN);
        new RegistrationService().reject(upgrade, "Permit is unreadable");

        assertEquals("CASUAL_DONOR", TestDb.scalar("SELECT role FROM account WHERE email = ?", CASUAL));
        assertEquals((long) registrationId(CASUAL), TestDb.count("SELECT registration_id FROM account WHERE email = ?", CASUAL));
        loginAs(CASUAL);
        profile.submitUpgrade(permits(), true);   // allowed again
    }
}
