package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.support.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The real login screen: its Forgot password link opens the dialog with the email already typed (R1.7). */
class LoginScreenTest {

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

    @Test
    void theForgotPasswordLinkOpensTheDialogWithTheTypedEmail() {
        Parent login = Fx.call(() -> {
            try {
                return new FXMLLoader(DialogController.class.getResource("/com/aniplato/view/login.fxml")).load();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        Fx.run(() -> {
            new Scene(login, 1280, 720);   // a ScrollPane shows its content only once it has a scene and a skin
            login.applyCss();
            login.layout();
            TextField email = login.lookupAll(".text-field").stream()
                    .filter(n -> n instanceof TextField && !(n instanceof PasswordField))
                    .map(n -> (TextField) n).findFirst().orElseThrow(() -> new AssertionError("no email field"));
            email.setText("me@aniplato.test");
            login.lookupAll(".hyperlink").stream()
                    .filter(n -> n instanceof Hyperlink l && "Forgot password?".equals(l.getText()))
                    .map(n -> (Hyperlink) n).findFirst().orElseThrow(() -> new AssertionError("no Forgot password link")).fire();
        });

        List<Node> open = Fx.call(() -> login.lookupAll(".modal-card").stream().filter(LoginScreenTest::isShown).toList());
        assertEquals(1, open.size(), "exactly one dialog is open");
        boolean prefilled = Fx.call(() -> open.get(0).lookupAll(".text-field").stream()
                .anyMatch(n -> n instanceof TextField f && "me@aniplato.test".equals(f.getText())));
        assertTrue(prefilled, "the dialog shows the email typed on the login screen");
    }
}
