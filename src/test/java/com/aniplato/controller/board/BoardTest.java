package com.aniplato.controller.board;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.TableRows;
import com.aniplato.support.Fx;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shared board: tabs, city, search and bands only narrow what is loaded; the dates read "Today" and "Tomorrow". No database. */
class BoardTest {

    record Item(String name, String city, boolean today) { }

    private static final List<Item> ITEMS = List.of(
            new Item("Bread for school meals", "Mandaluyong City", true),
            new Item("Meals for 120 kids", "Mandaluyong City", true),
            new Item("Vegetables for pantry", "Mandaluyong City", false),
            new Item("Hot meals for shelter", "Pasig City", false));

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private static Board<Item> board() {
        return Fx.call(() -> {
            Board<Item> board = new Board<Item>("Browse")
                    .tabs(List.of(new Board.Tab<>("All", i -> true), new Board.Tab<>("Today", Item::today)))
                    .search("Search", Item::name)
                    .cities(Item::city)
                    .columns(new double[] {60, 40}, "REQUEST", "CITY")
                    .bands(i -> i.city().startsWith("Mandaluyong") ? "mine" : "other",
                            (key, n) -> (key.equals("mine") ? "IN MANDALUYONG CITY" : "OTHER CITIES") + " · " + n)
                    .row(i -> {
                        GridPane row = TableRows.row(new double[] {60, 40});
                        TableRows.put(row, 0, TableRows.text(i.name(), "form-label"));
                        TableRows.put(row, 1, TableRows.text(i.city(), "table-cell-text"));
                        return row;
                    });
            board.show(ITEMS);
            return board;
        });
    }

    private static List<String> texts(Node node) {
        List<String> found = new ArrayList<>();
        collect(node, found);
        return found;
    }

    private static void collect(Node node, List<String> found) {
        if (node instanceof Label label && label.getText() != null) found.add(label.getText());
        if (node instanceof Button button && button.getText() != null) found.add(button.getText());
        if (node instanceof javafx.scene.Parent parent) parent.getChildrenUnmodifiable().forEach(c -> collect(c, found));
    }

    @Test
    void tabsShowTheirCountAndKeepOnlyTheirRows() {
        Board<Item> board = board();
        assertTrue(Fx.call(() -> texts(board.view())).containsAll(List.of("All (4)", "Today (2)")));
        Fx.run(() -> {
            Button today = (Button) buttons(board.view()).stream().filter(b -> b.getText().startsWith("Today")).findFirst().orElseThrow();
            today.fire();
        });
        assertEquals(2, Fx.call(() -> board.visible().size()));
    }

    @Test
    void searchNarrowsTheLoadedRows() {
        Board<Item> board = board();
        Fx.run(() -> ((TextField) find(board.view(), TextField.class)).setText("kids"));
        assertEquals(List.of("Meals for 120 kids"), Fx.call(() -> board.visible().stream().map(Item::name).toList()));
    }

    @Test
    void theCityDropDownKeepsOnlyThatCity() {
        Board<Item> board = board();
        @SuppressWarnings("unchecked")
        javafx.scene.control.ComboBox<String> city = (javafx.scene.control.ComboBox<String>) find(board.view(), javafx.scene.control.ComboBox.class);
        assertEquals(List.of("All cities", "Mandaluyong City", "Pasig City"), Fx.call(() -> List.copyOf(city.getItems())));
        Fx.run(() -> city.setValue("Pasig City"));
        assertEquals(List.of("Hot meals for shelter"), Fx.call(() -> board.visible().stream().map(Item::name).toList()));
    }

    @Test
    void bandsGroupTheRowsInTheOrderTheyFirstAppear() {
        Board<Item> board = board();
        List<String> shown = Fx.call(() -> texts(board.view()));
        assertTrue(shown.contains("IN MANDALUYONG CITY · 3"));
        assertTrue(shown.contains("OTHER CITIES · 1"));
        assertTrue(shown.indexOf("IN MANDALUYONG CITY · 3") < shown.indexOf("OTHER CITIES · 1"));
    }

    @Test
    void anEmptyBoardSaysSoAndASearchWithNoMatchSaysNoRowsMatch() {
        Board<Item> board = Fx.call(() -> new Board<Item>("Browse").empty("Nothing is open right now.")
                .columns(new double[] {100}, "A").row(i -> TableRows.row(new double[] {100})));
        Fx.run(() -> board.show(List.of()));
        assertTrue(Fx.call(() -> texts(board.view())).contains("Nothing is open right now."));
        Board<Item> searched = board();
        Fx.run(() -> ((TextField) find(searched.view(), TextField.class)).setText("zzz"));
        assertTrue(Fx.call(() -> texts(searched.view())).contains("No rows match."));
    }

    @Test
    void soonReadsTodayTomorrowOrTheDate() {
        LocalDate day = LocalDate.of(2026, 11, 6);
        assertEquals("Today, 6:00 PM", FormSupport.soon(LocalDateTime.of(2026, 11, 6, 18, 0), day));
        assertEquals("Tomorrow, 9:00 AM", FormSupport.soon(LocalDateTime.of(2026, 11, 7, 9, 0), day));
        assertEquals("Nov 12, 9:00 AM", FormSupport.soon(LocalDateTime.of(2026, 11, 12, 9, 0), day));
        assertEquals("", FormSupport.soon(null, day));
    }

    private static List<Button> buttons(Node node) {
        List<Button> found = new ArrayList<>();
        if (node instanceof Button b) found.add(b);
        if (node instanceof Pane p) p.getChildren().forEach(c -> found.addAll(buttons(c)));
        return found;
    }

    private static Node find(Node node, Class<? extends Node> type) {
        if (type.isInstance(node)) return node;
        if (node instanceof Pane p) {
            for (Node child : p.getChildren()) {
                Node found = find(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
