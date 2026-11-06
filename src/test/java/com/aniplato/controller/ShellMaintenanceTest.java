package com.aniplato.controller;

import com.aniplato.support.DbTest;
import com.aniplato.support.Fx;
import com.aniplato.support.TestDb;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 6.6: the automatic checks run before a screen reads its data: on the first screen after sign-in, and then each time or at most once per interval. */
class ShellMaintenanceTest extends DbTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    @BeforeEach
    void freshNavigator() {
        // the login screen is a window screen, so it clears whatever shell an earlier test left in Navigator
        Fx.run(() -> {
            Navigator.init(new Scene(new Pane(), 1280, 720));
            Navigator.show(Screen.LOGIN);
        });
    }

    private String status(String email) {
        return TestDb.scalar("SELECT status FROM account WHERE email = ?", email);
    }

    private void expireBusinessPermit() {
        TestDb.exec("UPDATE document SET expires_on = ? WHERE doc_type = 'mayors_permit'", LocalDate.now().minusDays(1));
    }

    @Test
    void openingTheShellRunsTheChecksBeforeTheFirstScreen() {
        expireBusinessPermit();
        loginAs(ADMIN);

        Fx.run(() -> Navigator.show(Screen.SHELL));

        assertEquals("SUSPENDED", status(BUSINESS));
    }

    @Test
    void everyLaterScreenRunsThemAgain() {
        loginAs(ADMIN);
        Fx.run(() -> Navigator.show(Screen.SHELL));
        assertEquals("ACTIVE", status(BUSINESS));

        expireBusinessPermit();
        Fx.run(() -> Navigator.show(Screen.ACCOUNTS));

        assertEquals("SUSPENDED", status(BUSINESS));
    }

    @Test
    void withAnIntervalALaterScreenInsideItSkipsTheChecksButTheFirstScreenAfterSignInRunsThem() {
        System.setProperty("aniplato.maintenance.interval.seconds", "600");
        try {
            loginAs(ADMIN);
            Fx.run(() -> Navigator.show(Screen.SHELL));          // first screen after signing in: the checks run
            assertEquals("ACTIVE", status(BUSINESS));

            expireBusinessPermit();
            Fx.run(() -> Navigator.show(Screen.ACCOUNTS));       // 10 minutes have not passed: skipped
            assertEquals("ACTIVE", status(BUSINESS), "the checks were skipped inside the interval");

            Fx.run(() -> Navigator.show(Screen.LOGIN));          // sign in again: the first screen runs them
            loginAs(ADMIN);
            Fx.run(() -> Navigator.show(Screen.SHELL));
            assertEquals("SUSPENDED", status(BUSINESS), "a new sign-in always runs them");
        } finally {
            System.setProperty("aniplato.maintenance.interval.seconds", "0");
        }
    }

    @Test
    void theLoginScreenDoesNotRunThem() {
        expireBusinessPermit();

        Fx.run(() -> Navigator.show(Screen.LOGIN));

        assertEquals("ACTIVE", status(BUSINESS));
    }
}
