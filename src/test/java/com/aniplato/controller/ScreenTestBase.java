package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;

/** What the screen tests share: load a real screen, then read its labels and buttons. */
abstract class ScreenTestBase extends DbTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    protected static Parent open(String view) {
        return Fx.call(() -> {
            try {
                return new FXMLLoader(DialogController.class.getResource("/com/aniplato/view/" + view)).load();
            } catch (Exception e) {
                throw new IllegalStateException(view, e);
            }
        });
    }

    /** Every label and button text under the node. */
    protected static List<String> texts(Node node) {
        List<String> found = new ArrayList<>();
        collect(node, found);
        return found;
    }

    private static void collect(Node node, List<String> found) {
        if (node instanceof Labeled labeled && labeled.getText() != null) found.add(labeled.getText());
        if (node instanceof ScrollPane pane && pane.getContent() != null) collect(pane.getContent(), found);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> collect(child, found));
    }

    /** Every node of the type under the node. A ScrollPane shows its content only once skinned, so it is walked by hand. */
    protected static <T extends Node> List<T> all(Node node, Class<T> type) {
        List<T> found = new ArrayList<>();
        gather(node, type, found);
        return found;
    }

    private static <T extends Node> void gather(Node node, Class<T> type, List<T> found) {
        if (type.isInstance(node)) found.add(type.cast(node));
        if (node instanceof ScrollPane pane && pane.getContent() != null) gather(pane.getContent(), type, found);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> gather(child, type, found));
    }

    protected static boolean mentions(Node screen, String text) {
        return texts(screen).stream().anyMatch(t -> t.contains(text));
    }

    protected static Button button(Node screen, String text) {
        return all(screen, Button.class).stream().filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("no button " + text + " in " + texts(screen)));
    }
}
