package com.aniplato.controller;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Account;
import com.aniplato.service.AuthenticationService;
import com.aniplato.service.GeminiAssistantService;
import com.aniplato.service.StatsService;
import com.aniplato.service.MaintenanceService;
import com.aniplato.util.AppConfig;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Session;
import com.aniplato.util.SvgIcon;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** Header, sidebar and content area shared by every logged-in screen. */
public class ShellController {

    private static final String ACTIVE = "sidebar-button-active";
    private static final Map<Screen, List<String>> ASSISTANT_SUGGESTIONS = Map.ofEntries(
            Map.entry(Screen.PROFILE, List.of("How do I update my profile?", "How do I change my password?")),
            Map.entry(Screen.MY_REQUESTS, List.of("How do I review offers on my request?", "How do I cancel a request?")),
            Map.entry(Screen.NEW_REQUEST, List.of("What details should I include in a food request?", "How do pickup windows work?")),
            Map.entry(Screen.FOODGROUP_OFFERS_RECEIVED, List.of("How can I respond to an offer?", "What happens after an offer is accepted?")),
            Map.entry(Screen.FOODGROUP_SUBMIT_REPORT, List.of("What should I include in the post-event report?", "When is the report due?")),
            Map.entry(Screen.ADMIN_DASHBOARD, List.of("What do the dashboard counts mean?", "Where do I review registrations?")),
            Map.entry(Screen.PENDING_REGISTRATIONS, List.of("What should I check before approving a registration?", "What happens when I reject one?")),
            Map.entry(Screen.REVIEW_REGISTRATION, List.of("Which documents need verification?", "How does approval affect the account?")),
            Map.entry(Screen.ACCOUNTS, List.of("How do I review an account's status?", "What does suspended status mean?")),
            Map.entry(Screen.SAFETY_NOTICES, List.of("How should I handle a safety notice?", "When can an account be suspended?")),
            Map.entry(Screen.REPORTS, List.of("How are rescue and acceptance rates calculated?", "What do expired and cancelled totals include?")),
            Map.entry(Screen.DONOR_DASHBOARD, List.of("How do I post surplus food?", "What should I do after an offer arrives?")),
            Map.entry(Screen.DONOR_BROWSE_REQUESTS, List.of("How do I respond to a food request?", "What makes a useful offer?")),
            Map.entry(Screen.DONOR_MAKE_OFFER, List.of("What should I include in an offer?", "What happens after I send one?")),
            Map.entry(Screen.FOODGROUP_DASHBOARD, List.of("How do I find available listings?", "How do I record a completed handoff?")),
            Map.entry(Screen.BROWSE_LISTINGS, List.of("How do I find listings near me?", "What should I check before offering?")),
            Map.entry(Screen.MAKE_OFFER, List.of("What pickup details should I include?", "What happens after the donor accepts?")),
            Map.entry(Screen.MY_OFFERS, List.of("How can I tell whether my offer was accepted?", "What happens when I accept a listing?")),
            Map.entry(Screen.DONOR_MY_OFFERS, List.of("How do I manage offers I made?", "What happens after my offer is accepted?")),
            Map.entry(Screen.LISTING_FORM, List.of("What information is required to post food?", "How should I describe storage and pickup?")),
            Map.entry(Screen.MY_LISTINGS, List.of("How do I update or cancel a listing?", "What do the listing statuses mean?")),
            Map.entry(Screen.LISTING_OFFERS, List.of("How do I choose an offer?", "What happens to other offers after acceptance?")),
            Map.entry(Screen.HISTORY, List.of("How do I understand completed or expired posts?", "How can I review a past handoff?")),
            Map.entry(Screen.HANDOFF_RECORD, List.of("How does handoff confirmation work?", "Who confirms release and receipt?"))
    );

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

    private Timeline assistantPulse;
    private boolean assistantRequestPending;

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final StatsService statsService = new StatsService();
    private final GeminiAssistantService geminiAssistantService = new GeminiAssistantService();
    private final List<Label> badges = new ArrayList<>();
    /** When the automatic checks last ran, in this sign-in; 0 until the first screen after signing in. */
    private static long lastMaintenanceMillis;

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final MaintenanceService maintenanceService = new MaintenanceService();
    private final SidebarBadges badges = new SidebarBadges();

    @FXML
    private void initialize() {
        Navigator.setContent(contentArea);
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
        refreshBadges();
        refreshAssistantSuggestions();
        Navigator.setScreenListener(() -> {
            refreshActive();
            refreshBadges();   // an approval or resolved notice changes the counts
            refreshAssistantSuggestions();
        badges.refresh();
        Navigator.setBeforeScreenListener(this::runMaintenance);
        Navigator.setScreenListener(() -> {
            refreshActive();
            badges.refresh();   // an approval or resolved notice changes the counts
        });
        startAssistantPulse();

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
        sendAssistantQuestion(assistantInput.getText());
    }

    private void sendAssistantQuestion(String question) {
        String text = question == null ? "" : question.trim();
        if (text.isEmpty() || assistantRequestPending) return;

        Screen current = Navigator.currentScreen();
        if (current == null) {
            addAssistantMessage("The assistant is available after a portal screen is open.", false);
            return;
        }

        addAssistantMessage(text, true);
        assistantInput.clear();
        assistantRequestPending = true;
        setAssistantRequestPending(true);
        Label response = addAssistantMessage("Thinking…", false);
        scrollAssistantToBottom();

        geminiAssistantService.answer(current.key(), text).whenComplete((answer, failure) ->
                Platform.runLater(() -> {
                    assistantRequestPending = false;
                    setAssistantRequestPending(false);
                    response.setText(failure == null
                            ? answer
                            : "I couldn't get an answer: " + GeminiAssistantService.explainFailure(failure));
                    scrollAssistantToBottom();
                }));
    }

    private Label addAssistantMessage(String text, boolean fromUser) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.setMaxWidth(270);
        message.getStyleClass().addAll("assistant-message", fromUser ? "assistant-question" : "assistant-reply");

        HBox row = new HBox(message);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.getStyleClass().add("assistant-message-row");
        assistantMessages.getChildren().add(row);
        return message;
    }

    private void refreshAssistantSuggestions() {
        assistantSuggestions.getChildren().clear();
        Screen current = Navigator.currentScreen();
        List<String> prompts = ASSISTANT_SUGGESTIONS.getOrDefault(current,
                List.of("How do I use this screen?", "Where can I get help?"));
        for (String prompt : prompts) {
            Button suggestion = new Button(prompt);
            suggestion.getStyleClass().add("assistant-suggestion-button");
            suggestion.setWrapText(true);
            suggestion.setOnAction(event -> sendAssistantQuestion(prompt));
            assistantSuggestions.getChildren().add(suggestion);
        }
    }

    private void setAssistantRequestPending(boolean pending) {
        assistantInput.setDisable(pending);
        assistantSendButton.setDisable(pending);
        assistantSuggestions.setDisable(pending);
    }

    private void scrollAssistantToBottom() {
        Platform.runLater(() -> assistantScrollPane.setVvalue(1));
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
        dialogController.showConfirmActionMode("Log out?", "You will need to sign in again to use ANIPLATO.", "Log out", false, () -> {
            assistantPulse.stop();
            authenticationService.logout();
            Navigator.show(Screen.LOGIN);
        });
    }
}