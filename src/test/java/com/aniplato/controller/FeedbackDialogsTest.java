package com.aniplato.controller;

import com.aniplato.support.Fx;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Session;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The confirmations and the "posted" moments sit on the real screens, in the shared card format. */
class FeedbackDialogsTest extends ScreenTestBase {

    private Scene scene;

    @BeforeEach
    void freshWindow() {
        Fx.run(() -> {
            scene = new Scene(new Pane(), 1280, 720);
            Navigator.init(scene);
            Navigator.show(Screen.LOGIN);
        });
    }

    private List<Node> openCards() {
        return Fx.call(() -> scene.getRoot().lookupAll(".modal-card").stream().filter(n -> n.isVisible() && n.isManaged()).toList());
    }

    /** The visible button with this text. Looked up on the JavaFX thread, so fire it in a separate Fx.run. */
    private Button buttonIn(Node root, String text) {
        return Fx.call(() -> all(root, Button.class).stream().filter(b -> text.equals(b.getText()) && b.isVisible() && b.isManaged())
                .findFirst().orElseThrow(() -> new AssertionError("no button " + text)));
    }

    private void click(Node root, String text) {
        Button button = buttonIn(root, text);
        Fx.run(button::fire);
    }

    @Test
    void logOutAsksFirstAndOnlyLogsOutOnConfirm() {
        loginAs(GROUP);
        Fx.run(() -> Navigator.show(Screen.SHELL));
        Fx.run(() -> {
            scene.getRoot().applyCss();
            ((Parent) scene.getRoot()).layout();
        });

        click(scene.getRoot(), "Log Out");
        assertEquals(1, openCards().size(), "a confirmation opens");
        assertTrue(mentions(openCards().get(0), "Log out?"));
        assertTrue(Session.isLoggedIn(), "still signed in while it asks");

        click(openCards().get(0), "← Back");
        assertEquals(0, openCards().size());
        assertTrue(Session.isLoggedIn(), "Back keeps the session");

        click(scene.getRoot(), "Log Out");
        click(openCards().get(0), "Log out");
        assertEquals(Screen.LOGIN, Navigator.currentScreen());
        assertFalse(Session.isLoggedIn(), "signed out");
    }

    @Test
    void everyFormThatPostsCarriesThePostedCard() {
        loginAs(GROUP);
        for (String form : List.of("donor/new-listing.fxml", "foodgroup/new-request.fxml", "donor/make-offer.fxml",
                "foodgroup/make-offer.fxml", "foodgroup/submit-report.fxml")) {
            Parent screen = open(form);
            long cards = Fx.call(() -> all(screen, javafx.scene.layout.VBox.class).stream().filter(v -> v.getStyleClass().contains("modal-card")).count());
            assertEquals(17L, cards, form + " has the dialogs, so it can say that it posted");
            assertNotEquals(0, Fx.call(() -> all(screen, Button.class).stream().filter(b -> "View my listings".equals(b.getText())
                    || "View my requests".equals(b.getText()) || "View my offers".equals(b.getText())
                    || "View handoff record".equals(b.getText()) || "Continue".equals(b.getText())).count()));
        }
    }
}
