package com.aniplato.controller;

import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.Fx;
import com.aniplato.support.TestDb;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sidebar counts: offers waiting for a donor's decision, accepted handoffs for a food group (Figma V2). */
class ShellBadgeTest extends DbTest {

    private Scene scene;

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    @BeforeEach
    void freshNavigator() {
        Fx.run(() -> {
            scene = new Scene(new Pane(), 1280, 720);
            Navigator.init(scene);
            Navigator.show(Screen.LOGIN);
        });
    }

    /** The texts of the badges that are showing. */
    private List<String> shownBadges() {
        List<String> shown = new ArrayList<>();
        Fx.run(() -> gather(scene.getRoot(), shown));
        return shown;
    }

    private static void gather(Node node, List<String> shown) {
        if (node instanceof Label label && label.getStyleClass().contains("sidebar-count-badge") && label.isVisible() && label.isManaged()) {
            shown.add(label.getText());
        }
        if (node instanceof Labeled labeled && labeled.getGraphic() != null) gather(labeled.getGraphic(), shown);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> gather(child, shown));
    }

    private List<String> sidebarTexts() {
        List<String> texts = new ArrayList<>();
        Fx.run(() -> collectNames(scene.getRoot(), texts));
        return texts;
    }

    private static void collectNames(Node node, List<String> texts) {
        if (node instanceof Button button && button.getGraphic() != null) {
            List<Label> labels = new ArrayList<>();
            gatherLabels(button.getGraphic(), labels);
            labels.stream().filter(l -> l.getStyleClass().contains("sidebar-nav-label")).forEach(l -> texts.add(l.getText()));
        }
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> collectNames(child, texts));
    }

    private static void gatherLabels(Node node, List<Label> labels) {
        if (node instanceof Label label) labels.add(label);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> gatherLabels(child, labels));
    }

    @Test
    void theDonorSeesHowManyOffersWaitForADecisionAndItClearsWhenDecided() {
        int offer = Fixtures.offerOnSeedListing("OFFERED", 12.5);
        loginAs(BUSINESS);

        Fx.run(() -> Navigator.show(Screen.SHELL));
        assertEquals(List.of("1"), shownBadges());

        TestDb.exec("UPDATE offer SET status = 'DECLINED' WHERE offer_id = ?", offer);
        Fx.run(() -> Navigator.show(Screen.MY_LISTINGS));
        assertTrue(shownBadges().isEmpty());
    }

    @Test
    void theFoodGroupSeesAcceptedHandoffsOnMyOffers() {
        Fixtures.offerOnSeedListing("ACCEPTED", 12.5);
        loginAs(GROUP);

        Fx.run(() -> Navigator.show(Screen.SHELL));

        assertEquals(List.of("1"), shownBadges());
    }

    @Test
    void theFoodGroupSidebarHasTheSevenItemsOfTheDesign() {
        loginAs(GROUP);
        Fx.run(() -> Navigator.show(Screen.SHELL));
        assertEquals(List.of("Dashboard", "Browse Listings", "My Offers", "New Request", "My Requests", "History", "Profile"), sidebarTexts());
    }

    @Test
    void anAdminSeesOnlyItsOwnCounts() {
        Fixtures.offerOnSeedListing("OFFERED", 12.5);   // a donor's waiting offer must not show on the admin's sidebar
        loginAs(ADMIN);

        Fx.run(() -> Navigator.show(Screen.SHELL));

        assertEquals(List.of("1"), shownBadges());   // the one pending registration in the seed
    }
}
