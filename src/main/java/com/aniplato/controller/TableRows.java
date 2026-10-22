package com.aniplato.controller;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

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
        row.getChildren().add(node);
    }

    /** A wrapping cell label; a null text shows a dash. */
    public static Label text(String text, String styleClass) {
        Label label = new Label(text == null ? "—" : text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private static GridPane grid(double[] widths) {
        GridPane row = new GridPane();
        row.setPadding(new Insets(12, 16, 12, 16));
        for (double percent : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percent);
            row.getColumnConstraints().add(column);
        }
        return row;
    }
}
