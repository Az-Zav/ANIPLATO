package com.aniplato.controller;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Header, sidebar and content area shared by every logged-in screen. */
public class ShellController {

    private static final String ACTIVE = "sidebar-button-active";

    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Label userRoleLabel;

    @FXML
    private void initialize() {
        Navigator.setContent(contentArea);
        // Sidebar buttons navigate via "nav-*" classes; this only moves the highlight
        sidebar.addEventHandler(ActionEvent.ACTION, e -> {
            if (e.getTarget() instanceof Node item) {
                sidebar.getChildren().forEach(n -> n.getStyleClass().remove(ACTIVE));
                item.getStyleClass().add(ACTIVE);
            }
        });

        // PATTERN:DYNAMIC fills placeholders
        // TODO(service): userRoleLabel.setText(...) from the logged-in account
        // TODO(service): build the sidebar from Navigator.menuFor(role) instead of the FXML admin items
    }

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleLogout() {
        // TODO(service): Session.clear()
        Navigator.show(Screen.LOGIN);
    }
}
