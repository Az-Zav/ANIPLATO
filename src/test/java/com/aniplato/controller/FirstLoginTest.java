package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fx;
import com.aniplato.support.TestDb;
import com.aniplato.util.Navigator;
import com.aniplato.util.Session;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R1.7: after a temporary password the app stays closed until the user sets their own, even if the dialog is dismissed. */
class FirstLoginTest extends DbTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private static boolean isShown(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (!n.isVisible() || !n.isManaged()) return false;
        }
        return true;
    }

    /** Presses the button whose text contains the given text; a dialog's button wins over a hidden twin on another dialog. */
    private static void click(Parent screen, String text) {
        Fx.run(() -> {
            List<Button> found = screen.lookupAll(".button").stream()
                    .filter(n -> n instanceof Labeled l && l.getText() != null && l.getText().contains(text))
                    .map(n -> (Button) n).toList();
            found.stream().filter(FirstLoginTest::isShown).findFirst()
                    .or(() -> found.stream().findFirst())
                    .orElseThrow(() -> new AssertionError("no button " + text)).fire();
        });
    }

    /** The login screen with the temporary-password account's details typed in, in a scene the Navigator controls. */
    private Parent loginScreenFilledIn() {
        TestDb.exec("UPDATE account SET must_change_password = TRUE WHERE email = ?", GROUP);
        Parent login = Fx.call(() -> {
            try {
                return new FXMLLoader(DialogController.class.getResource("/com/aniplato/view/login.fxml")).load();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        Fx.run(() -> {
            Navigator.init(new Scene(login, 1280, 720));
            login.applyCss();
            login.layout();
            login.lookupAll(".text-field").stream().filter(n -> n instanceof TextField && !(n instanceof PasswordField))
                    .map(n -> (TextField) n).findFirst().orElseThrow().setText(GROUP);
            login.lookupAll(".password-field").stream().map(n -> (PasswordField) n).findFirst().orElseThrow().setText(PASSWORD);
        });
        return login;
    }

    @Test
    void aTemporaryPasswordOpensTheSetNewPasswordDialogInsteadOfTheApp() {
        Parent login = loginScreenFilledIn();

        click(login, "Log In");

        List<Node> open = Fx.call(() -> login.lookupAll(".modal-card").stream().filter(FirstLoginTest::isShown).toList());
        assertEquals(1, open.size());
        assertTrue(Fx.call(() -> open.get(0).lookupAll(".label").stream()
                .anyMatch(n -> n instanceof Labeled l && "Set a new password".equals(l.getText()))));
        assertSame(login, Fx.call(() -> login.getScene().getRoot()), "the app did not open");
    }

    @Test
    void goingBackFromTheDialogDoesNotOpenTheAppAndEndsTheSession() {
        Parent login = loginScreenFilledIn();
        click(login, "Log In");
        assertTrue(Session.isLoggedIn(), "the login itself succeeded");

        click(login, "← Back");

        assertSame(login, Fx.call(() -> login.getScene().getRoot()), "still on the login screen");
        assertFalse(Session.isLoggedIn(), "nobody is half logged in");
    }

    @Test
    void theShellRefusesToOpenWhileThePasswordIsStillTemporary() {
        TestDb.exec("UPDATE account SET must_change_password = TRUE WHERE email = ?", GROUP);
        loginAs(GROUP);
        Fx.run(() -> Navigator.init(new Scene(new StackPane(), 1280, 720)));

        Fx.run(() -> Navigator.show(Navigator.Screen.SHELL));
        assertFalse(Session.isLoggedIn());
        assertEquals(Navigator.Screen.LOGIN, Navigator.currentScreen());

        loginAs(GROUP);
        Fx.run(() -> Navigator.show(Navigator.Screen.PROFILE));
        assertFalse(Session.isLoggedIn(), "a screen inside the shell is refused too");
        assertEquals(Navigator.Screen.LOGIN, Navigator.currentScreen());
    }

    @Test
    void anAccountWithItsOwnPasswordOpensTheAppNormally() {
        loginAs(GROUP);
        Fx.run(() -> Navigator.init(new Scene(new StackPane(), 1280, 720)));
        Fx.run(() -> Navigator.show(Navigator.Screen.PROFILE));
        assertTrue(Session.isLoggedIn());
        assertEquals(Navigator.Screen.PROFILE, Navigator.currentScreen());
    }
}
