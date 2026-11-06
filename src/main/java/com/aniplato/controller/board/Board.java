package com.aniplato.controller.board;

import com.aniplato.controller.TableRows;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The screen the list pages share (Figma "board"): a title with one action, summary tiles, and a card holding
 * tabs, an optional city filter, a search box, the table, and a footnote. The page gives it rows and says how to draw
 * one; tabs, city and search only narrow the rows already loaded, so no rule lives here (A1).
 */
public final class Board<T> {

    /** A tab: its name (the count is added) and which rows it keeps. */
    public record Tab<T>(String name, Predicate<T> keeps) { }

    private static final String ALL_CITIES = "All cities";

    private final VBox root = new VBox(16);
    private final HBox titleRow = new HBox(8);
    private final VBox card = new VBox(12);
    private final HBox toolbar = new HBox(12);
    private final HBox tabStrip = new HBox();
    private final VBox tableBox = new VBox(4);
    private final Label emptyLabel = new Label();
    private final Label footnote = new Label();
    private final TextField searchField = new TextField();
    private final ComboBox<String> cityBox = new ComboBox<>();
    private final List<Button> tabButtons = new ArrayList<>();

    private List<Tab<T>> tabs = List.of();
    private Tab<T> activeTab;
    private Function<T, String> searchText;
    private Function<T, String> cityOf;
    private double[] widths = {100};
    private String[] titles = {};
    private Function<T, String> bandKey;
    private BiFunction<String, Integer, String> bandText;
    private Function<T, GridPane> rowBuilder;
    private String emptyText = "Nothing here yet.";
    private List<T> items = List.of();
    private boolean refreshingCities;
    private Node tilesRow;

    public Board(String title) {
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().addAll(heading, spacer);

        tabStrip.getStyleClass().add("tab-strip");
        searchField.getStyleClass().add("board-search");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((o, was, now) -> render());
        cityBox.getStyleClass().add("board-city");
        cityBox.valueProperty().addListener((o, was, now) -> {
            if (!refreshingCities) render();
        });
        cityBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "City: " + item);
            }
        });

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getChildren().addAll(tabStrip, toolbarSpacer);

        emptyLabel.getStyleClass().add("dashboard-subtitle");
        emptyLabel.setWrapText(true);
        footnote.getStyleClass().add("table-footer-note");
        footnote.setWrapText(true);
        footnote.setVisible(false);
        footnote.setManaged(false);
        card.getStyleClass().add("table-card");
        card.setPadding(new Insets(20));
        card.getChildren().addAll(toolbar, tableBox, emptyLabel, footnote);
        root.getChildren().addAll(titleRow, card);
    }

    // ---- set up (called once by the page) ----

    /** The one button at the right of the title (New Request, Refresh, ...). */
    public Board<T> action(Button button) {
        titleRow.getChildren().add(button);
        return this;
    }

    /** Sets or replaces the summary tiles. */
    public Board<T> tiles(List<Tile> tiles) {
        Node row = Tiles.row(tiles);
        if (tilesRow == null) {
            root.getChildren().add(1, row);
        } else {
            root.getChildren().set(root.getChildren().indexOf(tilesRow), row);
        }
        tilesRow = row;
        return this;
    }

    /** Tabs in order; the first is the one shown first. */
    public Board<T> tabs(List<Tab<T>> definitions) {
        tabs = definitions;
        activeTab = definitions.isEmpty() ? null : definitions.get(0);
        tabButtons.clear();
        tabStrip.getChildren().clear();
        for (Tab<T> tab : definitions) {
            Button button = new Button();
            button.getStyleClass().add("tab-button");
            button.setOnAction(e -> {
                activeTab = tab;
                render();
            });
            tabButtons.add(button);
            tabStrip.getChildren().add(button);
        }
        return this;
    }

    public Board<T> search(String prompt, Function<T, String> text) {
        searchField.setPromptText(prompt);
        searchText = text;
        toolbar.getChildren().add(searchField);
        return this;
    }

    /** Adds the "City: All cities" drop-down; the choices are the cities of the loaded rows. */
    public Board<T> cities(Function<T, String> city) {
        cityOf = city;
        cityBox.setPrefWidth(190);
        toolbar.getChildren().add(toolbar.getChildren().size() - (searchText == null ? 0 : 1), cityBox);
        return this;
    }

    public Board<T> columns(double[] percentWidths, String... columnTitles) {
        widths = percentWidths;
        titles = columnTitles;
        return this;
    }

    /** Draws a band row before the first row of each group, in the order the groups first appear. */
    public Board<T> bands(Function<T, String> key, BiFunction<String, Integer, String> text) {
        bandKey = key;
        bandText = text;
        return this;
    }

    public Board<T> row(Function<T, GridPane> builder) {
        rowBuilder = builder;
        return this;
    }

    public Board<T> footnote(String text) {
        footnote.setText(text);
        footnote.setVisible(true);
        footnote.setManaged(true);
        return this;
    }

    /** What the table says when there are no rows at all. */
    public Board<T> empty(String text) {
        emptyText = text;
        return this;
    }

    public Node view() {
        return root;
    }

    // ---- data ----

    /** Replaces the rows and draws the table. */
    public void show(List<T> loaded) {
        items = loaded;
        if (cityOf != null) {
            TreeSet<String> cities = new TreeSet<>();
            for (T item : loaded) cities.add(cityOf.apply(item));
            String keep = cityBox.getValue();
            refreshingCities = true;
            cityBox.getItems().setAll(ALL_CITIES);
            cityBox.getItems().addAll(cities);
            cityBox.setValue(keep != null && cityBox.getItems().contains(keep) ? keep : ALL_CITIES);
            refreshingCities = false;
        }
        render();
    }

    /** The rows now on screen after the tab, city and search have narrowed them. Tests read this. */
    public List<T> visible() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String city = cityBox.getValue();
        List<T> kept = new ArrayList<>();
        for (T item : items) {
            if (activeTab != null && !activeTab.keeps().test(item)) continue;
            if (cityOf != null && city != null && !ALL_CITIES.equals(city) && !city.equals(cityOf.apply(item))) continue;
            if (searchText != null && !needle.isEmpty() && !searchText.apply(item).toLowerCase(Locale.ROOT).contains(needle)) continue;
            kept.add(item);
        }
        return kept;
    }

    private void render() {
        for (int i = 0; i < tabs.size(); i++) {
            Tab<T> tab = tabs.get(i);
            long count = items.stream().filter(tab.keeps()).count();
            Button button = tabButtons.get(i);
            button.setText(tab.name() + " (" + count + ")");
            button.getStyleClass().remove("tab-button-active");
            if (tab == activeTab) button.getStyleClass().add("tab-button-active");
        }
        List<T> rows = visible();
        List<Node> lines = new ArrayList<>();
        if (!rows.isEmpty()) {
            lines.add(TableRows.header(widths, titles));
            if (bandKey == null) {
                for (T item : rows) lines.add(rowBuilder.apply(item));
            } else {
                Map<String, List<T>> groups = new LinkedHashMap<>();
                for (T item : rows) groups.computeIfAbsent(bandKey.apply(item), k -> new ArrayList<>()).add(item);
                for (Map.Entry<String, List<T>> group : groups.entrySet()) {
                    Label band = new Label(bandText.apply(group.getKey(), group.getValue().size()));
                    band.getStyleClass().add("board-band");
                    band.setMaxWidth(Double.MAX_VALUE);
                    lines.add(band);
                    for (T item : group.getValue()) lines.add(rowBuilder.apply(item));
                }
            }
        }
        tableBox.getChildren().setAll(lines);
        emptyLabel.setText(items.isEmpty() ? emptyText : "No rows match.");
        emptyLabel.setVisible(rows.isEmpty());
        emptyLabel.setManaged(rows.isEmpty());
    }
}
