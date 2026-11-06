package com.aniplato.controller;

import com.aniplato.support.DbTest;
import com.aniplato.support.Fx;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Settings;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The floating assistant button must never sit over the last row of a page: every in-shell page keeps room under it. */
class AssistantRoomTest extends DbTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private static void collectPages(Node node, List<ScrollPane> found) {
        if (node instanceof ScrollPane pane && pane.getContent() instanceof Region) found.add(pane);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> collectPages(child, found));
    }

    private double bottomPaddingOf(Screen screen) {
        return Fx.call(() -> {
            Scene scene = new Scene(new Pane(), 1280, 720);
            Navigator.init(scene);
            Navigator.show(Screen.LOGIN);   // forget the shell of an earlier test, so this scene gets its own
            Navigator.show(screen);
            scene.getRoot().applyCss();   // a scroll pane only shows its content once styled
            scene.getRoot().layout();
            List<ScrollPane> pages = new ArrayList<>();
            collectPages(scene.getRoot(), pages);
            ScrollPane page = pages.stream().filter(p -> p.getContent() instanceof Region r && r.getStyleClass().contains("dashboard-bg"))
                    .findFirst().orElseThrow(() -> new AssertionError("no page body on " + screen));
            return ((Region) page.getContent()).getPadding().getBottom();
        });
    }

    @Test
    void everyKindOfInShellPageLeavesRoomUnderTheLastRow() {
        loginAs(BUSINESS);
        for (Screen screen : List.of(Screen.DONOR_DASHBOARD, Screen.LISTING_FORM, Screen.MY_LISTINGS, Screen.PROFILE, Screen.HISTORY)) {
            assertTrue(bottomPaddingOf(screen) >= Settings.ASSISTANT_CLEARANCE, screen + " keeps room for the assistant button");
        }
        loginAs(GROUP);
        assertTrue(bottomPaddingOf(Screen.MY_REQUESTS) >= Settings.ASSISTANT_CLEARANCE);
        loginAs(ADMIN);
        for (Screen screen : List.of(Screen.ADMIN_DASHBOARD, Screen.ACCOUNTS, Screen.REPORTS)) {
            assertTrue(bottomPaddingOf(screen) >= Settings.ASSISTANT_CLEARANCE, screen + " keeps room for the assistant button");
        }
    }
}
