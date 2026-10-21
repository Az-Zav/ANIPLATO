package com.aniplato.service;

import com.aniplato.model.Account;
import com.aniplato.model.Document;
import com.aniplato.model.Registration;
import com.aniplato.service.RegistrationService.DocumentUpload;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 1, applying: the details form, the documents, and what a submission saves. */
class RegistrationApplyTest extends DbTest {

    private final RegistrationService registrations = new RegistrationService();

    @TempDir
    Path tmp;

    //---THE DETAILS FORM---

    private Registration form(Account.Role role, String email) {
        Registration r = new Registration();
        r.role = role;
        r.name = "Test Org";
        r.city = registrations.cities().get(0);
        r.contactPerson = "Tess Ter";
        r.contactPhone = "09171234567";
        r.email = email;
        return r;
    }

    @Test
    void theCityListComesFromTheDatabase() {
        assertTrue(registrations.cities().stream().anyMatch(c -> c.name.equals("Quezon City")));
    }

    @Test
    void aCompleteFormPassesAndIsTrimmed() {
        Registration r = form(Account.Role.CASUAL_DONOR, "  new@aniplato.test ");
        r.name = "  Test Org  ";
        registrations.checkDetails(r);
        assertEquals("new@aniplato.test", r.email);
        assertEquals("Test Org", r.name);
    }

    @Test
    void everyMissingDetailIsNamedInTheMessage() {
        Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        r.role = null;
        assertRule("Say whether you have a Mayor's Permit and a Sanitary Permit.", () -> registrations.checkDetails(r));

        Registration noName = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        noName.name = " ";
        assertRule("Enter your name or organization name.", () -> registrations.checkDetails(noName));

        Registration noCity = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        noCity.city = null;
        assertRule("Choose your city.", () -> registrations.checkDetails(noCity));

        Registration noPerson = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        noPerson.contactPerson = "";
        assertRule("Enter the contact person.", () -> registrations.checkDetails(noPerson));

        Registration noPhone = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        noPhone.contactPhone = "";
        assertRule("Enter a contact number.", () -> registrations.checkDetails(noPhone));
    }

    @Test
    void theContactNumberMustBeExactlyElevenDigits() {
        for (String bad : List.of("0917123456", "091712345678", "0917abc4567", "0917 123 4567")) {
            Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
            r.contactPhone = bad;
            assertRule("The contact number must be exactly 11 digits.", () -> registrations.checkDetails(r));
        }
    }

    @Test
    void theEmailMustLookLikeAnEmail() {
        for (String bad : List.of("nobody", "a@b", "a b@c.com", "@c.com")) {
            Registration r = form(Account.Role.CASUAL_DONOR, bad);
            assertRule("Enter a valid email address.", () -> registrations.checkDetails(r));
        }
    }

    @Test
    void anEmailThatAlreadyHasAnAccountIsRefused() {
        Registration r = form(Account.Role.CASUAL_DONOR, GROUP);
        assertRule("This email already has an account. Log in, or use a different email.", () -> registrations.checkDetails(r));
    }

    //---SUBMITTING---

    private DocumentUpload upload(Document.Type type, LocalDate expiresOn, boolean noExpiry) throws IOException {
        File file = Files.write(tmp.resolve(type.name().toLowerCase() + ".pdf"), new byte[] {'%', 'P', 'D', 'F'}).toFile();
        return new DocumentUpload(type, file, expiresOn, noExpiry);
    }

    private DocumentUpload future(Document.Type type) throws IOException {
        return upload(type, LocalDate.now().plusMonths(6), false);
    }

    @Test
    void submitSavesThePendingRegistrationItsDocumentsTheLogAndTheReceivedEmail() throws IOException {
        Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");

        registrations.submit(r, List.of(future(Document.Type.BARANGAY_CERTIFICATE)), true);

        assertEquals("PENDING", TestDb.scalar("SELECT status FROM registration WHERE email = ?", "new@aniplato.test"));
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM document WHERE registration_id = ?", r.id));
        String stored = TestDb.scalar("SELECT file_path FROM document WHERE registration_id = ?", r.id);
        assertTrue(new FileStorageService().resolve(stored).isFile(), "the uploaded file is stored");
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM status_change WHERE entity_type = 'REGISTRATION' "
                + "AND entity_id = ? AND from_status IS NULL AND to_status = 'PENDING'", r.id));
        // the test config has no SMTP settings, so the send fails; the registration still stands
        assertEquals(1L, TestDb.count("SELECT COUNT(*) FROM email_log WHERE kind = 'RECEIVED' AND to_email = ?", "new@aniplato.test"));
    }

    @Test
    void aBusinessDonorNeedsBothPermits() throws IOException {
        Registration r = form(Account.Role.BUSINESS_DONOR, "new@aniplato.test");
        assertRule("Upload your Sanitary Permit.",
                () -> registrations.submit(r, List.of(future(Document.Type.MAYORS_PERMIT)), true));

        registrations.submit(r, List.of(future(Document.Type.MAYORS_PERMIT), future(Document.Type.SANITARY_PERMIT)), true);
        assertEquals(2L, TestDb.count("SELECT COUNT(*) FROM document WHERE registration_id = ?", r.id));
    }

    @Test
    void aFoodGroupNeedsOneCredentialAndTheAuthorizationLetterWhichHasNoExpiry() throws IOException {
        Registration noCredential = form(Account.Role.FOOD_GROUP, "group2@aniplato.test");
        assertRule("Choose a credential and upload its file.", () -> registrations.submit(noCredential,
                List.of(upload(Document.Type.AUTHORIZATION_LETTER, null, true)), true));

        Registration r = form(Account.Role.FOOD_GROUP, "group2@aniplato.test");
        registrations.submit(r, List.of(future(Document.Type.DSWD), upload(Document.Type.AUTHORIZATION_LETTER, null, true)), true);
        assertEquals(2L, TestDb.count("SELECT COUNT(*) FROM document WHERE registration_id = ?", r.id));
    }

    @Test
    void onlyTheRequiredDocumentsAreAccepted() throws IOException {
        Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        assertRule("Upload only the documents required for your registration.", () -> registrations.submit(r,
                List.of(future(Document.Type.BARANGAY_CERTIFICATE), future(Document.Type.DSWD)), true));
    }

    @Test
    void permitsAndTheBarangayCertificateAlwaysNeedAnExpiryDate() throws IOException {
        Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        assertRule("Enter the expiry date of your Barangay Certificate.", () -> registrations.submit(r,
                List.of(upload(Document.Type.BARANGAY_CERTIFICATE, null, false)), true));
        assertRule("Enter the expiry date of your Barangay Certificate.", () -> registrations.submit(r,
                List.of(upload(Document.Type.BARANGAY_CERTIFICATE, null, true)), true));
    }

    @Test
    void anExpiredDocumentIsRefused() throws IOException {
        Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        assertRule("Your Barangay Certificate has expired. Upload a current one.", () -> registrations.submit(r,
                List.of(upload(Document.Type.BARANGAY_CERTIFICATE, LocalDate.now().minusDays(1), false)), true));
    }

    @Test
    void onlyPdfJpgAndPngUnderTheSizeLimitAreAccepted() throws IOException {
        Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        File text = Files.write(tmp.resolve("notes.txt"), new byte[] {'x'}).toFile();
        assertRule("Upload a PDF, JPG or PNG file.", () -> registrations.submit(r, List.of(
                new DocumentUpload(Document.Type.BARANGAY_CERTIFICATE, text, LocalDate.now().plusMonths(6), false)), true));

        File big = Files.write(tmp.resolve("big.pdf"), new byte[5 * 1024 * 1024 + 1]).toFile();
        assertRule("Each file must be 5 MB or smaller.", () -> registrations.submit(r, List.of(
                new DocumentUpload(Document.Type.BARANGAY_CERTIFICATE, big, LocalDate.now().plusMonths(6), false)), true));
    }

    @Test
    void submitNeedsTheConsentTickedAndSavesNothingWhenARuleFails() throws IOException {
        Registration r = form(Account.Role.CASUAL_DONOR, "new@aniplato.test");
        long before = TestDb.count("SELECT COUNT(*) FROM registration");

        assertRule("Agree to the consent text to continue.",
                () -> registrations.submit(r, List.of(future(Document.Type.BARANGAY_CERTIFICATE)), false));
        assertRefused(() -> registrations.submit(r, List.of(), true));

        assertEquals(before, TestDb.count("SELECT COUNT(*) FROM registration"));
        assertEquals(0L, TestDb.count("SELECT COUNT(*) FROM email_log"));
    }
}
