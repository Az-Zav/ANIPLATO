package com.aniplato.controller.shared;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * What one dialog says and does: a heading, a subheading, some content blocks, and buttons. DialogController draws it
 * into the one dialog card, so every popup is data, not its own FXML. Build one with {@link #of}, add blocks and
 * actions, hand it to {@code DialogController.open}.
 *
 * <pre>
 * DialogSpec.of(Tone.RED, "Reject this organization?", "We'll email them your reason.")
 *         .body(Block.reason("Reason *", "e.g., The permit is expired."))
 *         .action("✕ Reject", Style.DANGER, 100, form -> service.reject(id, form.text("reason")));
 * </pre>
 */
public final class DialogSpec {

    public enum Tone { GREEN, RED }

    public enum Style {
        PRIMARY("primary-button"), DANGER("danger-button"), SECONDARY("secondary-button");

        final String css;

        Style(String css) {
            this.css = css;
        }
    }

    /** What a button does. A broken rule it throws is shown in the dialog, which stays open (A8); otherwise the dialog closes. */
    public interface Action {
        void run(Form form);
    }

    /** One piece of the dialog's content. Built fresh each time the dialog opens; it registers what the user can type or pick. */
    public interface Block extends Function<Form, Node> {

        /** A multi-line box, read back as {@code form.text("reason")}. */
        static Block reason(String label, String prompt) {
            return form -> {
                TextArea area = new TextArea();
                area.setPrefRowCount(3);
                area.setPromptText(prompt);
                area.setWrapText(true);
                area.getStyleClass().add("text-area");
                form.inputs.put("reason", area);
                return labelled(label, area);
            };
        }

        /** A one-line box ({@code secret} hides what is typed), read back as {@code form.text(key)}. */
        static Block text(String key, String label, String prompt, String prefill, boolean secret) {
            return form -> {
                TextField field = secret ? new PasswordField() : new TextField();
                field.setPromptText(prompt);
                field.getStyleClass().add("text-field");
                if (prefill != null) field.setText(prefill);
                form.inputs.put(key, field);
                return labelled(label, field);
            };
        }

        /** A file picker (PDF, JPG or PNG), read back as {@code form.file(key)}. */
        static Block file(String key, String label, String chooserTitle) {
            return form -> {
                Button choose = new Button("⇪  Upload PDF, JPG or PNG (max 5 MB)");
                choose.getStyleClass().add("secondary-button");
                choose.setMaxWidth(Double.MAX_VALUE);
                Label name = new Label("No file chosen");
                name.getStyleClass().add("doc-subtitle");
                choose.setOnAction(e -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle(chooserTitle);
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF, JPG or PNG", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
                    File chosen = chooser.showOpenDialog(choose.getScene() == null ? null : choose.getScene().getWindow());
                    if (chosen == null) return;
                    form.files.put(key, chosen);
                    name.setText(chosen.getName());
                });
                return labelled(label, choose, name);
            };
        }

        /** A date picker, read back as {@code form.date(key)}. */
        static Block date(String key, String label) {
            return form -> {
                DatePicker picker = new DatePicker();
                picker.setMaxWidth(Double.MAX_VALUE);
                form.dates.put(key, picker);
                return labelled(label, picker);
            };
        }

        /** A date picker with a "no expiry" tick that only some documents may use, read back as {@code form.checked(tickKey)}. */
        static Block dateOrNoExpiry(String key, String label, String tickKey, boolean mayHaveNoExpiry) {
            return form -> {
                VBox box = (VBox) date(key, label).apply(form);
                CheckBox none = new CheckBox("This document has no expiry date");
                none.setVisible(mayHaveNoExpiry);
                none.setManaged(mayHaveNoExpiry);
                form.checks.put(tickKey, none);
                box.getChildren().add(none);
                return box;
            };
        }

        /** A tick box with a sentence, read back as {@code form.checked(key)}. */
        static Block consent(String key, String text) {
            return form -> {
                CheckBox box = new CheckBox(text);
                box.setWrapText(true);
                form.checks.put(key, box);
                return new VBox(8, box);
            };
        }

        /** Label and value rows in a green box, e.g. the handoff details. */
        static Block facts(String[][] rows) {
            return form -> {
                GridPane grid = new GridPane();
                grid.setHgap(12);
                grid.setVgap(8);
                grid.getStyleClass().add("notice-box");
                grid.setPadding(new Insets(12, 16, 12, 16));
                ColumnConstraints left = new ColumnConstraints();
                left.setPercentWidth(30);
                ColumnConstraints right = new ColumnConstraints();
                right.setPercentWidth(70);
                grid.getColumnConstraints().addAll(left, right);
                for (int i = 0; i < rows.length; i++) {
                    Label name = new Label(rows[i][0]);
                    name.getStyleClass().add("text-muted");
                    Label value = new Label(rows[i][1]);
                    value.getStyleClass().add("form-label");
                    grid.add(name, 0, i);
                    grid.add(value, 1, i);
                }
                return grid;
            };
        }

        /** A heading with ticked boxes the user can untick. */
        static Block checks(String heading, String... labels) {
            return form -> {
                VBox box = new VBox(8);
                Label title = new Label(heading);
                title.getStyleClass().add("form-label");
                box.getChildren().add(title);
                for (String text : labels) {
                    CheckBox check = new CheckBox(text);
                    check.setSelected(true);
                    check.getStyleClass().add("small-check");
                    box.getChildren().add(check);
                }
                return box;
            };
        }

        /** A shaded line with an ⓘ; the three style classes are the box, the icon and the text. */
        static Block notice(String text, String boxStyle, String iconStyle, String textStyle) {
            return form -> {
                Label icon = new Label("ⓘ");
                icon.getStyleClass().add(iconStyle);
                Label message = new Label(text);
                message.getStyleClass().add(textStyle);
                message.setWrapText(true);
                HBox.setHgrow(message, Priority.ALWAYS);
                HBox box = new HBox(10, icon, message);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(10, 12, 10, 12));
                box.getStyleClass().add(boxStyle);
                return box;
            };
        }

        /** Small muted text under the fields. */
        static Block hint(String text) {
            return form -> {
                Label label = new Label(text);
                label.getStyleClass().add("text-muted");
                label.setWrapText(true);
                return label;
            };
        }

        /** A red link that does something else, e.g. opens another dialog. */
        static Block link(String text, Runnable action) {
            return form -> {
                Hyperlink link = new Hyperlink(text);
                link.getStyleClass().add("required-star");
                link.setOnAction(e -> action.run());
                return link;
            };
        }

        private static VBox labelled(String label, Node... nodes) {
            Label title = new Label(label);
            title.getStyleClass().add("form-label");
            VBox box = new VBox(8, title);
            box.getChildren().addAll(nodes);
            return box;
        }
    }

    /** What the user typed, picked or ticked in the open dialog. */
    public static final class Form {
        private final Map<String, TextInputControl> inputs = new HashMap<>();
        private final Map<String, File> files = new HashMap<>();
        private final Map<String, DatePicker> dates = new HashMap<>();
        private final Map<String, CheckBox> checks = new HashMap<>();
        private boolean keepOpen;

        public String text(String key) {
            TextInputControl input = inputs.get(key);
            return input == null ? null : input.getText();
        }

        public File file(String key) {
            return files.get(key);
        }

        public LocalDate date(String key) {
            DatePicker picker = dates.get(key);
            return picker == null ? null : picker.getValue();
        }

        public boolean checked(String key) {
            CheckBox box = checks.get(key);
            return box != null && box.isSelected();
        }

        /** Leave the dialog open after this action worked, e.g. to show a confirmation in it. */
        public void keepOpen() {
            keepOpen = true;
        }

        boolean staysOpen() {
            return keepOpen;
        }
    }

    record ButtonSpec(String label, Style style, double minWidth, Action action) { }

    final Tone tone;
    final String title;
    final String subtitle;
    String emoji;                                  // a symbol instead of the round icon
    boolean wide;                                  // the handoff dialogs: wider card, tighter heading
    final List<List<Block>> body = new ArrayList<>();   // each entry is one child of the card; more than one block means a group
    final List<Double> gaps = new ArrayList<>();
    final List<ButtonSpec> actions = new ArrayList<>();
    Runnable onBack;                               // null: Back just closes
    boolean noBack;                                // an info dialog has only its one button

    private DialogSpec(Tone tone, String title, String subtitle) {
        this.tone = tone;
        this.title = title;
        this.subtitle = subtitle;
    }

    public static DialogSpec of(Tone tone, String title, String subtitle) {
        return new DialogSpec(tone, title, subtitle);
    }

    /** Draws this symbol instead of the round check or cross. */
    public DialogSpec emoji(String symbol) {
        this.emoji = symbol;
        return this;
    }

    /** The wider card with a tighter heading, used for the handoff confirmations. */
    public DialogSpec wide() {
        this.wide = true;
        return this;
    }

    /** One block directly in the card. */
    public DialogSpec body(Block block) {
        body.add(List.of(block));
        gaps.add(0.0);
        return this;
    }

    /** Several blocks stacked with {@code gap} between them, as one child of the card (a form). */
    public DialogSpec group(double gap, Block... blocks) {
        body.add(List.of(blocks));
        gaps.add(gap);
        return this;
    }

    /** A button on the right. */
    public DialogSpec action(String label, Style style, double minWidth, Action action) {
        actions.add(new ButtonSpec(label, style, minWidth, action));
        return this;
    }

    /** Something other than closing when Back is pressed. */
    public DialogSpec onBack(Runnable action) {
        this.onBack = action;
        return this;
    }

    /** No Back button; one {@code label} button that only closes. For a dialog that just tells the user something. */
    public DialogSpec infoOnly(String label) {
        this.noBack = true;
        actions.add(new ButtonSpec(label, Style.PRIMARY, 110, form -> { }));
        return this;
    }
}
