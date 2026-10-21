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

/** 6.6: the automatic checks run each time the shell loads a screen, before that screen reads its data. */
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
    void theLoginScreenDoesNotRunThem() {
        expireBusinessPermit();

        Fx.run(() -> Navigator.show(Screen.LOGIN));

        assertEquals("ACTIVE", status(BUSINESS));
    }
}
