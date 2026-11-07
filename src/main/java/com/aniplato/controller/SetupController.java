package com.aniplato.controller;

import com.aniplato.exception.RuleViolationException;
import com.aniplato.service.SetupDetails;
import com.aniplato.service.SetupService;
import com.aniplato.service.SetupService.Extras;
import com.aniplato.service.SetupService.Finding;
import com.aniplato.service.SetupService.Kind;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * First-run setup screen (Spec 6.8). Reads the form, calls SetupService and shows what it says; it holds no rules.
 * Database calls run on a background thread so the window stays responsive on a slow connection.
 */
public class SetupController {

    @FXML private TextField urlField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField databaseField;
    @FXML private TextField userField;
    @FXML private PasswordField passwordField;
    @FXML private TextField caField;
    @FXML private Button testButton;
    @FXML private Label statusLabel;
    @FXML private VBox adminBox;
    @FXML private TextField adminEmailField;
    @FXML private PasswordField adminPasswordField;
    @FXML private PasswordField adminConfirmField;
    @FXML private TextField assistantKeyField;
    @FXML private TextField mailUserField;
    @FXML private PasswordField mailPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button continueButton;

    private final SetupService setupService = new SetupService();
    private Kind tested;   // what the last Test connection found; null once any connection field has changed since

    @FXML
    private void initialize() {
        portField.setText("3306");
        databaseField.setText("aniplato");
        for (TextField field : new TextField[]{hostField, portField, databaseField, userField, passwordField, caField}) {
            field.textProperty().addListener((o, was, now) -> forgetTest());
        }
    }

    // PATTERN:ACTION fills host, port and database from a pasted jdbc:mysql address
    @FXML
    private void handleUseAddress() {
        hideError();
        String problem = FormSupport.failure(() -> {
            SetupDetails d = SetupDetails.fromUrl(urlField.getText(), userField.getText(), passwordField.getText(), caField.getText());
            hostField.setText(d.host());
            portField.setText(d.port());
            databaseField.setText(d.database());
        }, "Could not read that address.");
        if (problem != null) showError(problem);
    }

    // PATTERN:ACTION
    @FXML
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the certificate file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Certificate (*.pem, *.crt)", "*.pem", "*.crt"));
        File file = chooser.showOpenDialog(caField.getScene().getWindow());
        if (file != null) caField.setText(file.getAbsolutePath());
    }

    // PATTERN:ACTION Test connection
    @FXML
    private void handleTest() {
        hideError();
        SetupDetails details = details();
        run(() -> setupService.inspect(details), this::showFinding);
    }

    // PATTERN:ACTION Create, or Connect when the database is already set up
    @FXML
    private void handleContinue() {
        hideError();
        SetupDetails details = details();
        Extras extras = new Extras(assistantKeyField.getText(), mailUserField.getText(), mailPasswordField.getText());
        Kind kind = tested;
        String email = adminEmailField.getText();
        String password = adminPasswordField.getText();
        String confirm = adminConfirmField.getText();
        run(() -> {
            if (kind == Kind.ALREADY_SET_UP) setupService.connect(details, extras);
            else setupService.create(details, email, password, confirm, extras);
            return Boolean.TRUE;
        }, done -> Navigator.show(Screen.LOGIN));
    }

    private SetupDetails details() {
        return new SetupDetails(hostField.getText(), portField.getText(), databaseField.getText(), userField.getText(),
                passwordField.getText(), caField.getText());
    }

    private void showFinding(Finding found) {
        tested = found.kind();
        boolean ok = found.kind() != Kind.CANNOT_CONTINUE;
        statusLabel.setText(found.message());
        statusLabel.getStyleClass().removeAll("error-text", "inline-success-text");
        statusLabel.getStyleClass().add(ok ? "inline-success-text" : "error-text");
        setShown(statusLabel, true);
        boolean needsAdmin = found.kind() == Kind.EMPTY || found.kind() == Kind.ADMIN_MISSING;
        setShown(adminBox, needsAdmin);
        continueButton.setText(needsAdmin ? "Create" : "Connect");
        continueButton.setDisable(!ok);
    }

    private void forgetTest() {
        tested = null;
        setShown(statusLabel, false);
        setShown(adminBox, false);
        continueButton.setDisable(true);
    }

    /** Runs the call off the screen thread; a broken rule or database problem is shown in place, a result goes to onDone. */
    private <T> void run(Supplier<T> call, Consumer<T> onDone) {
        testButton.setDisable(true);
        continueButton.setDisable(true);
        Task<T> task = new Task<>() {
            @Override protected T call() {
                return call.get();
            }
        };
        task.setOnSucceeded(e -> {
            testButton.setDisable(false);
            continueButton.setDisable(tested == null || tested == Kind.CANNOT_CONTINUE);
            onDone.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            testButton.setDisable(false);
            continueButton.setDisable(tested == null || tested == Kind.CANNOT_CONTINUE);
            Throwable why = task.getException();
            showError(why instanceof RuleViolationException ? why.getMessage()
                    : "Something went wrong: " + (why == null || why.getMessage() == null ? "unknown problem" : why.getMessage()));
        });
        Thread worker = new Thread(task, "aniplato-setup");
        worker.setDaemon(true);
        worker.start();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            setShown(errorLabel, true);
        });
    }

    private void hideError() {
        setShown(errorLabel, false);
    }

    private static void setShown(javafx.scene.Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
