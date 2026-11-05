package com.aniplato.controller;

import com.aniplato.service.ProfileService;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Edits the contact person and number right where they are shown on the Profile (6.3), instead of in a popup.
 * Edit swaps the label for two fields; a broken rule (A8) is written under the fields and they stay open.
 */
final class ContactEditor {

    private final ProfileService profileService = new ProfileService();
    private final Label shown;
    private final Hyperlink editLink;
    private final VBox editor = new VBox(8);
    private final TextField personField = new TextField();
    private final TextField phoneField = new TextField();
    private final Label problem = new Label();
    private Runnable onSaved = () -> { };

    /** Adds the (hidden) editor to {@code host}, below the label and the Edit link. */
    ContactEditor(VBox host, Label shown, Hyperlink editLink) {
        this.shown = shown;
        this.editLink = editLink;
        phoneField.setPromptText("09171234567");
        problem.getStyleClass().add("inline-error-text");
        problem.setWrapText(true);
        Button save = new Button("Save");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(e -> close());
        Label note = new Label("Only you, the ANIPLATO admin and the other side of an accepted offer see the number.");
        note.getStyleClass().add("helper-note");
        note.setWrapText(true);
        editor.getChildren().addAll(personField, phoneField, note, problem, new HBox(8, save, cancel));
        host.getChildren().add(editor);
        hide(problem);
        hide(editor);
    }

    /** Opens the editor with the current details; {@code saved} runs after a successful save. */
    void open(String contactPerson, String contactPhone, Runnable saved) {
        onSaved = saved;
        personField.setText(contactPerson);
        phoneField.setText(contactPhone);
        hide(problem);
        show(editor);
        hide(shown);
        hide(editLink);
    }

    private void save() {
        String failure = FormSupport.failure(
                () -> profileService.updateContact(personField.getText(), phoneField.getText()),
                "Could not save this. Check the database connection and try again.");
        if (failure != null) {
            problem.setText(failure);
            show(problem);
            return;
        }
        close();
        onSaved.run();
    }

    private void close() {
        hide(editor);
        show(shown);
        show(editLink);
    }

    private static void show(javafx.scene.Node node) {
        node.setVisible(true);
        node.setManaged(true);
    }

    private static void hide(javafx.scene.Node node) {
        node.setVisible(false);
        node.setManaged(false);
    }
}
