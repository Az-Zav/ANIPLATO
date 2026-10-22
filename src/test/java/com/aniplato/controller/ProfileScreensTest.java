package com.aniplato.controller;

import com.aniplato.support.Fixtures;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The profile and the accounts list: the upgrade and renewal buttons show only where they apply. */
class ProfileScreensTest extends ScreenTestBase {

    //---PROFILE: UPGRADE, EDIT, RENEWAL---

    @Test
    void onlyACasualDonorSeesTheUpgradeButtonOnTheProfile() {
        loginAs(CASUAL);
        assertTrue(all(open("profile.fxml"), Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged()
                && "Upgrade to Business donor".equals(b.getText())));
        loginAs(BUSINESS);
        assertFalse(all(open("profile.fxml"), Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged()
                && "Upgrade to Business donor".equals(b.getText())));
        loginAs(GROUP);
        assertFalse(all(open("profile.fxml"), Button.class).stream().anyMatch(b -> b.isVisible() && b.isManaged()
                && "Upgrade to Business donor".equals(b.getText())));
    }

    @Test
    void theAccountsListOffersReviewRenewalOnlyWhereAFileWaits() {
        loginAs(BUSINESS);
        new com.aniplato.service.DocumentReviewService().uploadRenewal(com.aniplato.model.Document.Type.MAYORS_PERMIT,
                Fixtures.photos(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")), 1).get(0), java.time.LocalDate.now().plusYears(1), false);
        loginAs(ADMIN);
        Parent screen = open("admin/accounts.fxml");
        assertEquals(1, all(screen, Button.class).stream().filter(b -> "Review renewal".equals(b.getText())).count());
    }
}
