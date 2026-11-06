package com.aniplato.controller;

import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Header and data rows for the table cards of the dashboards and History: a GridPane whose columns are percentages. */
public final class TableRows {

    private TableRows() { }

    /** The green header row with one title per column. */
    public static GridPane header(double[] widths, String... titles) {
        GridPane row = grid(widths);
        row.getStyleClass().add("table-header-row");
        for (int i = 0; i < titles.length; i++) put(row, i, text(titles[i], "table-header-text"));
        return row;
    }

    /** An empty data row; fill it with {@link #put}. */
    public static GridPane row(double[] widths) {
        GridPane row = grid(widths);
        row.getStyleClass().add("table-data-row");
        return row;
    }

    public static void put(GridPane row, int column, Node node) {
        GridPane.setColumnIndex(node, column);
        GridPane.setValignment(node, VPos.CENTER);
        row.getChildren().add(node);
    }

    /** A wrapping cell label; a null text shows a dash. */
    public static Label text(String text, String styleClass) {
        Label label = new Label(text == null ? "—" : text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    /** A rounded status pill in its own cell so it keeps its natural width. {@code tone}: open, accepted, done, neutral or danger. */
    public static HBox pill(String text, String tone) {
        Label label = new Label(text);
        label.getStyleClass().addAll("board-pill", "board-pill-" + tone);
        HBox cell = new HBox(label);
        cell.setAlignment(javafx.geometry.Pos.CENTER_LEFT);   // the cell fills the row; without this the pill sits at its top
        return cell;
    }

    /** The small green button at the end of a board row. */
    public static Button rowButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("row-button");
        button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);   // never cut the label to "Review off..."
        button.setOnAction(e -> action.run());
        return button;
    }

    /** The small outlined button (Withdraw, Suspend, ...). */
    public static Button rowOutline(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("row-button-outline");
        button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        button.setOnAction(e -> action.run());
        return button;
    }

    /** A small underlined link under a row button ("Cancel request"). */
    public static Hyperlink rowLink(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("board-link");
        link.setOnAction(e -> action.run());
        return link;
    }

    /** A cell that stacks its nodes with a small gap (a name over its detail lines). */
    public static VBox stack(Node... nodes) {
        VBox stack = new VBox(2, nodes);
        stack.setAlignment(javafx.geometry.Pos.CENTER_LEFT);   // centred in the row like the single-line cells
        return stack;
    }

    private static GridPane grid(double[] widths) {
        GridPane row = new GridPane();
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setHgap(8);   // a long cell never touches the next column
        for (double percent : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percent);
            row.getColumnConstraints().add(column);
        }
        return row;
    }
}
