package com.aniplato.controller;

import com.aniplato.service.GeminiAssistantService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Session;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The floating assistant: the open/close button, the chat panel, the suggested questions for the current screen, and the
 * call to the Gemini service. It holds no rules (the service does the asking); the shell owns the FXML nodes and hands
 * them in.
 */
final class AssistantPanel {

    private static final Map<Screen, List<String>> SUGGESTIONS = Map.ofEntries(
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

    private final VBox panel;
    private final VBox messages;
    private final ScrollPane scrollPane;
    private final FlowPane suggestions;
    private final TextField input;
    private final Button sendButton;
    private final Button toggle;
    private final GeminiAssistantService service = new GeminiAssistantService();

    private final List<GeminiAssistantService.Turn> history = new ArrayList<>();
    private Timeline pulse;
    private boolean requestPending;

    AssistantPanel(VBox panel, VBox messages, ScrollPane scrollPane, FlowPane suggestions,
                   TextField input, Button sendButton, Button toggle) {
        this.panel = panel;
        this.messages = messages;
        this.scrollPane = scrollPane;
        this.suggestions = suggestions;
        this.input = input;
        this.sendButton = sendButton;
        this.toggle = toggle;
        startPulse();
    }

    /** Opens or closes the panel. */
    void toggle() {
        boolean open = !panel.isVisible();
        panel.setVisible(open);
        panel.setManaged(open);
        toggle.setText(open ? "×" : "✦");
        toggle.setAccessibleText(open ? "Close ANIPLATO Assistant" : "Open ANIPLATO Assistant");

        if (open) {
            pulse.stop();
            panel.setOpacity(0);
            panel.setScaleX(0.88);
            panel.setScaleY(0.88);
            FadeTransition fade = new FadeTransition(Duration.millis(260), panel);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(320), panel);
            scale.setToX(1);
            scale.setToY(1);
            new ParallelTransition(fade, scale).play();
            input.requestFocus();
        } else {
            startPulse();
        }

        RotateTransition rotation = new RotateTransition(Duration.millis(320), toggle);
        rotation.setByAngle(open ? 180 : -180);
        rotation.play();
    }

    /** Sends what is typed in the input box. */
    void sendTyped() {
        send(input.getText());
    }

    /** Stops the button's pulse (on logout, so no animation outlives the shell). */
    void stop() {
        pulse.stop();
    }

    /** Shows the suggested questions for the screen now open. */
    void refreshSuggestions() {
        suggestions.getChildren().clear();
        List<String> prompts = SUGGESTIONS.getOrDefault(Navigator.currentScreen(),
                List.of("How do I use this screen?", "Where can I get help?"));
        for (String prompt : prompts) {
            Button suggestion = new Button(prompt);
            suggestion.getStyleClass().add("assistant-suggestion-button");
            suggestion.setWrapText(true);
            suggestion.setOnAction(event -> send(prompt));
            suggestions.getChildren().add(suggestion);
        }
    }

    private void send(String question) {
        String text = question == null ? "" : question.trim();
        if (text.isEmpty() || requestPending) return;

        Screen current = Navigator.currentScreen();
        if (current == null) {
            addMessage("The assistant is available after a portal screen is open.", false);
            return;
        }

        addMessage(text, true);
        input.clear();
        requestPending = true;
        setPending(true);
        Label response = addMessage("Thinking…", false);
        scrollToBottom();

        String role = Session.isLoggedIn() ? Session.require().role.key() : "";
        service.answer(current.key(), role, text, List.copyOf(history)).whenComplete((answer, failure) ->
                Platform.runLater(() -> {
                    requestPending = false;
                    setPending(false);
                    if (failure == null) {   // only a finished exchange joins the history, so the turns stay in order
                        history.add(new GeminiAssistantService.Turn(true, text));
                        history.add(new GeminiAssistantService.Turn(false, answer));
                    }
                    response.setText(failure == null
                            ? answer
                            : "I couldn't get an answer: " + GeminiAssistantService.explainFailure(failure));
                    scrollToBottom();
                }));
    }

    private Label addMessage(String text, boolean fromUser) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.setMaxWidth(270);
        message.getStyleClass().addAll("assistant-message", fromUser ? "assistant-question" : "assistant-reply");

        HBox row = new HBox(message);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.getStyleClass().add("assistant-message-row");
        messages.getChildren().add(row);
        return message;
    }

    private void setPending(boolean pending) {
        input.setDisable(pending);
        sendButton.setDisable(pending);
        suggestions.setDisable(pending);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1));
    }

    private void startPulse() {
        pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(toggle.scaleXProperty(), 1),
                        new KeyValue(toggle.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(850),
                        new KeyValue(toggle.scaleXProperty(), 1.12),
                        new KeyValue(toggle.scaleYProperty(), 1.12)),
                new KeyFrame(Duration.millis(1700),
                        new KeyValue(toggle.scaleXProperty(), 1),
                        new KeyValue(toggle.scaleYProperty(), 1))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }
}
