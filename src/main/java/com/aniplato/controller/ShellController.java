package com.aniplato.controller;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.SvgIcon;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        Navigator.Role role = Navigator.currentRole();
        userRoleLabel.setText(role.displayName());
        String roleClass = "role-" + role.key();
        sidebar.getChildren().removeIf(item -> item instanceof Button button
                && !button.getStyleClass().contains(roleClass)
                && !button.getStyleClass().contains("role-all"));

        sidebar.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .forEach(this::configureIcon);

        // Highlight follows Navigator.currentScreen(): set now, and again after every in-shell navigation
        refreshActive();
        Navigator.setScreenListener(this::refreshActive);

        // PATTERN:DYNAMIC fills placeholders
        // TODO(service): userRoleLabel.setText(account.getRole().toString())
    }

    /** Highlights the sidebar button for the screen being shown (a detail screen highlights its parent). */
    private void refreshActive() {
        Screen current = Navigator.currentScreen();
        if (current == null) return;
        String activeClass = "nav-" + Navigator.sidebarScreenFor(current).key();
        sidebar.getChildren().stream()
                .filter(Button.class::isInstance)
                .forEach(button -> {
                    button.getStyleClass().remove(ACTIVE);
                    if (button.getStyleClass().contains(activeClass)) button.getStyleClass().add(ACTIVE);
                });
    }

    private void configureIcon(Button button) {
        String iconName = button.getStyleClass().stream()
                .filter(styleClass -> styleClass.startsWith("icon-"))
                .map(styleClass -> styleClass.substring("icon-".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sidebar button has no icon class: " + button.getText()));
        String filename = switch (iconName) {
            case "all-accounts" -> "AllAccountsIcon.svg";
            case "dashboard" -> "Dashboard Icon.svg";
            case "history" -> "HistoryIcon.svg";
            case "my-listings" -> "MyListingsIcon.svg";
            case "new-listing" -> "NewListingIcon.svg";
            case "pending-accounts" -> "PendingAccountsIcon.svg";
            case "profile" -> "ProfileIcon.svg";
            case "reports" -> "ReportsIcon.svg";
            case "safety-notices" -> "SafetyNotices.svg";
            default -> throw new IllegalStateException("Unknown sidebar icon: " + iconName);
        };
        Node icon = SvgIcon.load("/assets/" + filename, 17);
        Label label = new Label(button.getText());
        label.getStyleClass().add("sidebar-nav-label");

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefWidth(180);
        content.getChildren().addAll(icon, label);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        content.getChildren().add(spacer);

        button.getStyleClass().stream()
                .filter(styleClass -> styleClass.startsWith("badge-"))
                .findFirst()
                .ifPresent(styleClass -> {
                    Label badge = new Label(styleClass.substring("badge-".length()));
                    badge.getStyleClass().add("sidebar-count-badge");
                    content.getChildren().add(badge);
                });

        button.setText("");
        button.setGraphic(content);
        button.setGraphicTextGap(0);
    }

    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleLogout() {
        // TODO(service): Session.clear()
        Navigator.show(Screen.LOGIN);
    }
}