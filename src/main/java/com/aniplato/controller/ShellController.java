package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Account;
import com.aniplato.service.AuthenticationService;
import com.aniplato.service.MaintenanceService;
import com.aniplato.util.AppConfig;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Session;
import com.aniplato.util.SvgIcon;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;


/** Header, sidebar and content area shared by every logged-in screen. */
public class ShellController {

    private static final String ACTIVE = "sidebar-button-active";
    @FXML private StackPane contentArea;
    @FXML private HBox messageBar;
    @FXML private Label messageText;
    @FXML private VBox sidebar;
    // FXML: fx:id="userRoleLabel" shows the logged-in account's role
    @FXML private Label userRoleLabel;
    @FXML private DialogController dialogController;
    @FXML private VBox assistantPanel;
    @FXML private VBox assistantMessages;
    @FXML private ScrollPane assistantScrollPane;
    @FXML private FlowPane assistantSuggestions;
    @FXML private TextField assistantInput;
    @FXML private Button assistantSendButton;
    @FXML private Button assistantToggle;

    private AssistantPanel assistant;
    /** When the automatic checks last ran, in this sign-in; 0 until the first screen after signing in. */
    private static long lastMaintenanceMillis;

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final MaintenanceService maintenanceService = new MaintenanceService();
    private final SidebarBadges badges = new SidebarBadges();

    @FXML
    private void initialize() {
        Navigator.setContent(contentArea);
        assistant = new AssistantPanel(assistantPanel, assistantMessages, assistantScrollPane, assistantSuggestions,
                assistantInput, assistantSendButton, assistantToggle);
        lastMaintenanceMillis = 0;   // a new sign-in always runs the checks on its first screen
        ScreenMessage.bind(messageBar, messageText);
        Account.Role role = Session.require().role;
        // the header names the organization (the admin has none, so it says Admin)
        userRoleLabel.setText(role == Account.Role.ADMIN ? role.displayName() : Session.require().registration.name);
        // FXML: each sidebar button carries "role-<key>" (admin, donor, foodgroup) or "role-all"
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
        badges.refresh();
        assistant.refreshSuggestions();
        Navigator.setBeforeScreenListener(this::runMaintenance);
        Navigator.setScreenListener(() -> {
            refreshActive();
            badges.refresh();   // an approval or resolved notice changes the counts
            assistant.refreshSuggestions();
        });
    }

    /**
     * 6.6: the automatic checks run before a screen loads, on the first screen after signing in and then at most once per
     * {@code maintenance.interval.seconds} (default 60; 0 = before every screen). Against a remote database the checks
     * cost about twenty round trips, too many to pay on every click. A failing check never blocks the screen.
     */
    private void runMaintenance() {
        ScreenMessage.clear();   // a message belongs to the screen that raised it
        long interval = Long.parseLong(AppConfig.get("maintenance.interval.seconds", "60")) * 1000;
        long now = System.currentTimeMillis();
        if (interval > 0 && lastMaintenanceMillis != 0 && now - lastMaintenanceMillis < interval) return;
        lastMaintenanceMillis = now;
        try {
            maintenanceService.runMaintenance();
        } catch (RuntimeException e) {
            System.err.println("Automatic checks failed: " + e.getMessage());
        }
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

    private static String iconFile(String iconName) {
        return switch (iconName) {
            case "all-accounts" -> "AllAccountsIcon.svg";
            case "browse-requests" -> "BrowseRequest.svg";
            case "dashboard" -> "Dashboard Icon.svg";
            case "history" -> "HistoryIcon.svg";
            case "my-offers" -> "MyOffers.svg";
            case "my-listings" -> "MyListingsIcon.svg";
            case "new-listing" -> "NewListingIcon.svg";
            case "pending-accounts" -> "PendingAccountsIcon.svg";
            case "profile" -> "ProfileIcon.svg";
            case "reports" -> "ReportsIcon.svg";
            case "safety-notices" -> "SafetyNotices.svg";
            default -> throw new IllegalStateException("Unknown sidebar icon: " + iconName);
        };
    }

    private void configureIcon(Button button) {
        String iconName = button.getStyleClass().stream()
                .filter(styleClass -> styleClass.startsWith("icon-"))
                .map(styleClass -> styleClass.substring("icon-".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sidebar button has no icon class: " + button.getText()));
        Node icon = SvgIcon.load("/assets/" + iconFile(iconName), 17);
        Label label = new Label(button.getText());
        label.getStyleClass().add("sidebar-nav-label");

        label.setMinWidth(Region.USE_PREF_SIZE);   // never cut "Pending Accounts" to "Pending Accou..."
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.prefWidthProperty().bind(button.widthProperty().subtract(32));   // the button's width less its padding
        content.getChildren().addAll(icon, label);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        content.getChildren().add(spacer);

        button.getStyleClass().stream()
                .filter(styleClass -> styleClass.startsWith("badge-"))
                .findFirst()
                .ifPresentOrElse(styleClass -> {
                    content.getChildren().add(badges.create(styleClass.substring("badge-".length())));
                }, () -> {
                    if (button.getStyleClass().contains("role-admin")) {   // the admin's items without a count carry a chevron, as in the design
                        Label chevron = new Label("›");
                        chevron.getStyleClass().add("sidebar-chevron");
                        content.getChildren().add(chevron);
                    }
                });

        button.setText("");
        button.setGraphic(content);
        button.setGraphicTextGap(0);
    }

    @FXML
    private void dismissMessage() {
        ScreenMessage.clear();
    }

    @FXML
    private void toggleAssistant() {
        assistant.toggle();
    }

    @FXML
    private void handleSendMessage() {
        assistant.sendTyped();
    }

    // FXML: logoutBtn onAction="#handleLogout"
    // PATTERN:ACTION
    @FXML
    private void handleLogout() {
        dialogController.showConfirmActionMode("Log out?", "You will need to sign in again to use ANIPLATO.", "Log out", false, () -> {
            assistant.stop();
            authenticationService.logout();
            Navigator.show(Screen.LOGIN);
        });
    }
}