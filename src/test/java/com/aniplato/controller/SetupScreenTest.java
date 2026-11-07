package com.aniplato.controller;

import com.aniplato.support.Fx;
import com.aniplato.support.TestDb;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real Setup screen (Spec 6.8): its starting state, and Test connection against the test server. Create and Connect write
 * the user's own settings file, so the service tests cover them instead of this screen.
 */
class SetupScreenTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private record Loaded(Parent root, SetupController controller) { }

    private static Loaded load() {
        return Fx.call(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(SetupController.class.getResource("/com/aniplato/view/setup.fxml"));
                Parent root = loader.load();
                new Scene(root, 1280, 720);   // a ScrollPane shows its content only once it has a scene and a skin
                root.applyCss();
                root.layout();
                return new Loaded(root, loader.getController());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static Parent open() {
        return load().root();
    }

    private static <T extends Node> T find(Parent root, Class<T> type, String id) {
        return type.cast(root.lookup("#" + id));   // a plain lookup: callers already on the FX thread must not wait for it
    }

    private static boolean shown(Node node) {
        return node.isVisible() && node.isManaged();
    }

    private static void waitFor(Supplier<Boolean> condition) throws InterruptedException {
        for (int i = 0; i < 200 && !condition.get(); i++) Thread.sleep(50);
        assertTrue(condition.get(), "the screen did not answer in time");
    }

    @Test
    void itStartsWithTheUsualDefaultsAndNothingToContinueWith() {
        Parent screen = open();
        assertEquals("3306", find(screen, TextField.class, "portField").getText());
        assertEquals("aniplato", find(screen, TextField.class, "databaseField").getText());
        assertTrue(Fx.call(() -> find(screen, Button.class, "continueButton").isDisabled()), "nothing is created before a successful test");
        assertFalse(shown(find(screen, Label.class, "statusLabel")));
        assertFalse(shown(find(screen, javafx.scene.layout.VBox.class, "adminBox")), "the admin form waits for a test result");
    }

    @Test
    void pastingAnAddressFillsHostPortAndDatabase() {
        Parent screen = open();
        Fx.run(() -> {
            find(screen, TextField.class, "urlField").setText("jdbc:mysql://db.example.com:23021/shelter?ssl-mode=REQUIRED");
            ((Button) screen.lookupAll(".button").stream().filter(n -> n instanceof Button b && "Use address".equals(b.getText()))
                    .findFirst().orElseThrow()).fire();
        });
        assertEquals("db.example.com", find(screen, TextField.class, "hostField").getText());
        assertEquals("23021", find(screen, TextField.class, "portField").getText());
        assertEquals("shelter", find(screen, TextField.class, "databaseField").getText());
    }

    @Test
    void aWorkingLoginShowsTheAdminFormAndEnablesCreate() throws Exception {
        Assumptions.assumeFalse(TestDb.isOnline(), "needs a plain local test server");
        Parent screen = open();
        fillLogin(screen, TestDb.serverPassword());
        Fx.run(() -> find(screen, Button.class, "testButton").fire());
        Label status = find(screen, Label.class, "statusLabel");
        waitFor(() -> shown(status));
        assertTrue(Fx.call(status::getText).contains("will be created"), Fx.call(status::getText));
        assertTrue(shown(find(screen, javafx.scene.layout.VBox.class, "adminBox")));
        assertFalse(Fx.call(() -> find(screen, Button.class, "continueButton").isDisabled()));
        assertEquals("Create", Fx.call(() -> find(screen, Button.class, "continueButton").getText()));
        TestDb.dropScratch(TestDb.NAME + "_screen");   // Test connection creates nothing; this only proves it
    }

    @Test
    void aWrongPasswordIsShownInPlaceAndCreateStaysOff() throws Exception {
        Assumptions.assumeFalse(TestDb.isOnline(), "needs a plain local test server");
        Parent screen = open();
        fillLogin(screen, "definitely-wrong");
        Fx.run(() -> find(screen, Button.class, "testButton").fire());
        Label status = find(screen, Label.class, "statusLabel");
        waitFor(() -> shown(status));
        assertTrue(Fx.call(status::getText).contains("user name and password"), Fx.call(status::getText));
        assertTrue(Fx.call(() -> find(screen, Button.class, "continueButton").isDisabled()));
        assertFalse(shown(find(screen, javafx.scene.layout.VBox.class, "adminBox")));
    }

    @Test
    void changingAFieldAfterATestTakesTheResultBack() throws Exception {
        Assumptions.assumeFalse(TestDb.isOnline(), "needs a plain local test server");
        Parent screen = open();
        fillLogin(screen, TestDb.serverPassword());
        Fx.run(() -> find(screen, Button.class, "testButton").fire());
        waitFor(() -> shown(find(screen, Label.class, "statusLabel")));
        Fx.run(() -> find(screen, TextField.class, "hostField").setText("elsewhere"));
        assertFalse(shown(find(screen, Label.class, "statusLabel")), "a result for the old address is not shown for the new one");
        assertTrue(Fx.call(() -> find(screen, Button.class, "continueButton").isDisabled()));
    }

    private static void typeAdmin(Parent screen, String email, String password, String confirm) {
        Fx.run(() -> {
            find(screen, TextField.class, "adminEmailField").setText(email);
            find(screen, PasswordField.class, "adminPasswordField").setText(password);
            find(screen, PasswordField.class, "adminConfirmField").setText(confirm);
        });
    }

    @Test
    void aFullSetupThroughTheScreenCreatesTheDatabaseAndSavesTheSettings(@org.junit.jupiter.api.io.TempDir java.nio.file.Path folder) throws Exception {
        Assumptions.assumeFalse(TestDb.isOnline(), "needs a plain local test server");
        String database = TestDb.NAME + "_screen";
        TestDb.dropScratch(database);
        try {
            Loaded screen = load();
            java.nio.file.Path settings = folder.resolve("db.properties");
            java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1);
            Fx.run(() -> {
                screen.controller().setupService = new com.aniplato.service.SetupService(settings);   // never the user's own settings file
                screen.controller().afterSetup = finished::countDown;                                   // in place of opening Sign In
            });
            fillLogin(screen.root(), TestDb.serverPassword());
            Fx.run(() -> find(screen.root(), Button.class, "testButton").fire());
            waitFor(() -> shown(find(screen.root(), javafx.scene.layout.VBox.class, "adminBox")));
            typeAdmin(screen.root(), "owner@example.org", "a-good-password", "a-good-password");
            Fx.run(() -> find(screen.root(), Button.class, "continueButton").fire());
            assertTrue(finished.await(60, java.util.concurrent.TimeUnit.SECONDS), "Create did not finish");

            com.aniplato.service.SetupService check = new com.aniplato.service.SetupService(settings);
            String[] hostPort = TestDb.hostPort().split(":");
            var found = check.inspect(new com.aniplato.service.SetupDetails(hostPort[0], hostPort.length > 1 ? hostPort[1] : "3306",
                    database, TestDb.serverUser(), TestDb.serverPassword(), ""));
            assertEquals(com.aniplato.service.SetupService.Kind.ALREADY_SET_UP, found.kind(), found.message());
            java.util.Properties saved = new java.util.Properties();
            try (java.io.InputStream in = java.nio.file.Files.newInputStream(settings)) {
                saved.load(in);
            }
            assertTrue(saved.getProperty("url").contains("/" + database), saved.getProperty("url"));
            assertEquals(TestDb.serverUser(), saved.getProperty("user"));
        } finally {
            TestDb.dropScratch(database);
        }
    }

    @Test
    void passwordsThatDoNotMatchAreSaidInPlaceAndNothingIsCreated(@org.junit.jupiter.api.io.TempDir java.nio.file.Path folder) throws Exception {
        Assumptions.assumeFalse(TestDb.isOnline(), "needs a plain local test server");
        String database = TestDb.NAME + "_screen";
        TestDb.dropScratch(database);
        try {
            Loaded screen = load();
            java.nio.file.Path settings = folder.resolve("db.properties");
            Fx.run(() -> screen.controller().setupService = new com.aniplato.service.SetupService(settings));
            fillLogin(screen.root(), TestDb.serverPassword());
            Fx.run(() -> find(screen.root(), Button.class, "testButton").fire());
            waitFor(() -> shown(find(screen.root(), javafx.scene.layout.VBox.class, "adminBox")));
            typeAdmin(screen.root(), "owner@example.org", "a-good-password", "something-else");
            Fx.run(() -> find(screen.root(), Button.class, "continueButton").fire());
            Label error = find(screen.root(), Label.class, "errorLabel");
            waitFor(() -> shown(error));
            assertTrue(Fx.call(error::getText).contains("do not match"), Fx.call(error::getText));
            assertFalse(java.nio.file.Files.exists(settings), "no settings are saved after a refusal");
            String[] hostPort = TestDb.hostPort().split(":");
            assertEquals(com.aniplato.service.SetupService.Kind.EMPTY, new com.aniplato.service.SetupService(settings).inspect(
                    new com.aniplato.service.SetupDetails(hostPort[0], hostPort.length > 1 ? hostPort[1] : "3306", database,
                            TestDb.serverUser(), TestDb.serverPassword(), "")).kind(), "the database was not created");
        } finally {
            TestDb.dropScratch(database);
        }
    }

    private static void fillLogin(Parent screen, String password) {
        String[] hostPort = TestDb.hostPort().split(":");
        Fx.run(() -> {
            find(screen, TextField.class, "hostField").setText(hostPort[0]);
            find(screen, TextField.class, "portField").setText(hostPort.length > 1 ? hostPort[1] : "3306");
            find(screen, TextField.class, "databaseField").setText(TestDb.NAME + "_screen");
            find(screen, TextField.class, "userField").setText(TestDb.serverUser());
            find(screen, PasswordField.class, "passwordField").setText(password);
        });
    }
}
