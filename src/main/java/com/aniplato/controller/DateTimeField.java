package com.aniplato.controller;

import com.aniplato.exception.RuleViolationException;
import javafx.geometry.Insets;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** A date picker and a 24-hour time box side by side, for the use-by, pickup and needed-by times. Used in FXML as one control. */
public class DateTimeField extends HBox {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

    private final DatePicker date = new DatePicker();
    private final TextField time = new TextField();

    public DateTimeField() {
        super(8);
        time.setPromptText("HH:mm");
        time.setPrefWidth(84);
        date.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(date, Priority.ALWAYS);
        setPadding(Insets.EMPTY);
        getChildren().addAll(date, time);
    }

    /** The chosen moment. A missing date or a time that is not like 17:30 is refused with a message naming the field. */
    public LocalDateTime value(String fieldName) {
        if (date.getValue() == null || time.getText() == null || time.getText().isBlank()) {
            throw new RuleViolationException("Choose a date and a time for " + fieldName + ".");
        }
        try {
            return LocalDateTime.of(date.getValue(), LocalTime.parse(time.getText().trim(), TIME));
        } catch (DateTimeParseException e) {
            throw new RuleViolationException("Enter the time for " + fieldName + " like 17:30.");
        }
    }

    public void setValue(LocalDateTime value) {
        date.setValue(value == null ? null : value.toLocalDate());
        time.setText(value == null ? "" : value.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
    }
}
