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

    private static Parent open() {
        return Fx.call(() -> {
            try {
                Parent root = new FXMLLoader(SetupController.class.getResource("/com/aniplato/view/setup.fxml")).load();
                new Scene(root, 1280, 720);   // a ScrollPane shows its content only once it has a scene and a skin
                root.applyCss();
                root.layout();
                return root;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
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
