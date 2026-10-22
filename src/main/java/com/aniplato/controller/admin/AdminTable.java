package com.aniplato.controller.admin;

import com.aniplato.model.Account;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds the table rows of the admin screens in code, with the same style classes as the FXML sample rows
 * (a row is a GridPane whose columns are percentages). Controllers fill a row box from service data.
 */
final class AdminTable {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private AdminTable() {}

    /** An empty data row with the given column widths (percent). */
    static GridPane row(double... percents) {
        GridPane row = new GridPane();
        row.getStyleClass().add("table-data-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        for (double percent : percents) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percent);
            row.getColumnConstraints().add(column);
        }
        return row;
    }

    /** Puts the node in a column of the row. */
    static void put(GridPane row, int column, Node node) {
        GridPane.setColumnIndex(node, column);
        row.getChildren().add(node);
    }

    static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** A wrapping label, for text that can be long (concerns, listing names). */
    static Label wrapped(String text, String styleClass) {
        Label label = label(text, styleClass);
        label.setWrapText(true);
        return label;
    }

    /** A pill in its own cell so it keeps its natural width. */
    static HBox pill(String text, String styleClass) {
        HBox cell = new HBox(label(text, styleClass));
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    /** A name with a small line of tags under it. */
    static VBox titled(String title, Node... under) {
        HBox tags = new HBox(6, under);
        tags.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(2, label(title, "form-label"), tags);
        return box;
    }

    /** A muted text on the left and a button on the right (the last cell of most rows). */
    static HBox textAndButton(String text, Button... buttons) {
        Label muted = label(text, "table-cell-muted");
        HBox.setHgrow(muted, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox cell = new HBox(8, muted, spacer);
        for (Button button : buttons) {
            if (button != null) cell.getChildren().add(button);
        }
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    static Button button(String text, String styleClass, EventHandler<ActionEvent> onClick) {
        Button button = new Button(text);
        button.setMinWidth(Region.USE_PREF_SIZE);   // never shorten the label to "Rev..."
        button.getStyleClass().add(styleClass);
        button.setOnAction(onClick);
        return button;
    }

    /** "Business donor", "Casual donor" or "Food group". */
    static String roleName(Account.Role role) {
        return switch (role) {
            case BUSINESS_DONOR -> "Business donor";
            case CASUAL_DONOR -> "Casual donor";
            case FOOD_GROUP -> "Food group";
            case ADMIN -> "Admin";
        };
    }

    /** Donors are green pills, food groups the other colour, as in the design. */
    static String roleStyle(Account.Role role) {
        return role == Account.Role.FOOD_GROUP ? "pill-food-org" : "pill-food-donor";
    }

    static String date(LocalDateTime when) {
        return when.format(DATE);
    }

    static String dateTime(LocalDateTime when) {
        return when.format(DATE_TIME);
    }

    /** 1,284 kg, or 12.5 kg when the total has a fraction. */
    static String kg(double value) {
        return (value == Math.rint(value) ? String.format("%,.0f", value) : String.format("%,.1f", value));
    }

    /** 94.6%, or a dash when the rate cannot be worked out yet. */
    static String percent(Double rate) {
        return rate == null ? "—" : String.format("%.1f%%", rate * 100);
    }
}
