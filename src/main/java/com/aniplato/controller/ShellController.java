package com.aniplato.controller;

import com.aniplato.model.Account;
import com.aniplato.service.AuthenticationService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Session;
import com.aniplato.util.SvgIcon;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Locale;

/** Header, sidebar and content area shared by every logged-in screen. */
public class ShellController {

    private static final String ACTIVE = "sidebar-button-active";

    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    // FXML: fx:id="userRoleLabel" shows the logged-in account's role
    @FXML private Label userRoleLabel;
    @FXML private VBox assistantPanel;
    @FXML private VBox assistantMessages;
    @FXML private ScrollPane assistantScrollPane;
    @FXML private TextField assistantInput;
    @FXML private Button assistantToggle;

    private Timeline assistantPulse;

    private final AuthenticationService authenticationService = new AuthenticationService();

    @FXML
    private void initialize() {
        Navigator.setContent(contentArea);
        Account.Role role = Session.require().role;
        userRoleLabel.setText(role.displayName());
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
        Navigator.setScreenListener(this::refreshActive);
        startAssistantPulse();

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

    @FXML
    private void toggleAssistant() {
        boolean open = !assistantPanel.isVisible();
        assistantPanel.setVisible(open);
        assistantPanel.setManaged(open);
        assistantToggle.setText(open ? "×" : "✦");
        assistantToggle.setAccessibleText(open ? "Close ANIPLATO Assistant" : "Open ANIPLATO Assistant");

        if (open) {
            assistantPulse.stop();
            assistantPanel.setOpacity(0);
            assistantPanel.setScaleX(0.88);
            assistantPanel.setScaleY(0.88);
            FadeTransition fade = new FadeTransition(Duration.millis(260), assistantPanel);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(320), assistantPanel);
            scale.setToX(1);
            scale.setToY(1);
            new ParallelTransition(fade, scale).play();
            assistantInput.requestFocus();
        } else {
            startAssistantPulse();
        }

        RotateTransition rotation = new RotateTransition(Duration.millis(320), assistantToggle);
        rotation.setByAngle(open ? 180 : -180);
        rotation.play();
    }

    @FXML
    private void handleSendMessage() {
        String question = assistantInput.getText().trim();
        if (question.isEmpty()) return;

        addAssistantMessage(question, true);
        addAssistantMessage(answerFor(question), false);
        assistantInput.clear();
        Platform.runLater(() -> assistantScrollPane.setVvalue(1));
    }

    private void addAssistantMessage(String text, boolean fromUser) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.setMaxWidth(270);
        message.getStyleClass().addAll("assistant-message", fromUser ? "assistant-question" : "assistant-reply");

        HBox row = new HBox(message);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.getStyleClass().add("assistant-message-row");
        assistantMessages.getChildren().add(row);
    }

    private String answerFor(String question) {
        String text = question.toLowerCase(Locale.ROOT);
        if (containsAny(text, "offer", "accept", "group")) {
            return "Food groups can offer on open listings in their city. The donor selects one offer; the other offers are declined when it is accepted.";
        }
        if (containsAny(text, "handoff", "pickup", "rescued", "receipt", "release")) {
            return "At pickup, the donor confirms release first. The food group then confirms receipt, completing the handoff and marking the listing Rescued.";
        }
        if (containsAny(text, "report", "rate", "expired", "cancelled", "canceled", "kilogram", " kg")) {
            return "Reports summarize listed, rescued, expired, and cancelled food, plus rescue, offer acceptance, and lapse rates. Rescue rate is rescued kilograms divided by listed kilograms for closed, non-cancelled listings.";
        }
        if (containsAny(text, "safety", "notice", "suspend")) {
            return "Admins review safety notices and may suspend an account with a recorded reason. This assistant cannot review notices or change account status.";
        }
        if (containsAny(text, "listing", "post", "donate", "donation", "surplus")) {
            return "To post surplus, open New Listing and enter the food details, quantity, use-by time, pickup window, and storage condition. Your account must be active and its documents current.";
        }
        if (containsAny(text, "verify", "verification", "document", "permit", "registration", "account")) {
            return "Registrations are checked by an admin. Documents need to be current and match the organization's name and city. For your account's specific status, check the relevant page or contact an admin.";
        }
        if (containsAny(text, "profile", "password", "history", "help")) {
            return "Use Profile to review your organization details and password options, and History to see closed listings or offers. I can explain the workflow, but I cannot access or change your account.";
        }
        return "I can answer general questions about listings, offers, handoffs, verification, safety notices, reports, and account pages. I can't access private account details or perform actions.";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private void startAssistantPulse() {
        assistantPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(assistantToggle.scaleXProperty(), 1),
                        new KeyValue(assistantToggle.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(850),
                        new KeyValue(assistantToggle.scaleXProperty(), 1.12),
                        new KeyValue(assistantToggle.scaleYProperty(), 1.12)),
                new KeyFrame(Duration.millis(1700),
                        new KeyValue(assistantToggle.scaleXProperty(), 1),
                        new KeyValue(assistantToggle.scaleYProperty(), 1))
        );
        assistantPulse.setCycleCount(Timeline.INDEFINITE);
        assistantPulse.play();
    }

    // FXML: logoutBtn onAction="#handleLogout"
    // PATTERN:ACTION
    @FXML
    private void handleLogout() {
        assistantPulse.stop();
        authenticationService.logout();
        Navigator.show(Screen.LOGIN);
    }
}