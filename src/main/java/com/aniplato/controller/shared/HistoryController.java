package com.aniplato.controller.shared;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.TableRows;
import com.aniplato.service.HistoryService;
import com.aniplato.service.HistoryService.Outcome;
import com.aniplato.service.HistoryService.Row;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * History. Shared screen: it asks for "my" history and never checks the role (A5). The cards are all-time, the period
 * filters the table, and the tabs and search only narrow the lines already loaded.
 */
public class HistoryController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final String ALL_TIME = "All time";

    private final HistoryService historyService = new HistoryService();
    private List<YearMonth> months = List.of();
    private HistoryService.View view;
    private Outcome tab;            // null = all
    private boolean loading;        // refilling the period list must not reload the table

    @FXML private ComboBox<String> periodCombo;
    @FXML private Label kgLabel;
    @FXML private Label handoffsLabel;
    @FXML private Label partnersLabel;
    @FXML private Button allTab;
    @FXML private Button rescuedTab;
    @FXML private Button expiredTab;
    @FXML private Button cancelledTab;
    @FXML private TextField searchField;
    @FXML private VBox tableBox;
    @FXML private Label emptyLabel;

    @FXML
    private void initialize() {
        periodCombo.valueProperty().addListener((obs, was, now) -> {
            if (!loading) load(selectedPeriod());
        });
        searchField.textProperty().addListener((obs, was, now) -> showTable());
        load(null);
    }

    private YearMonth selectedPeriod() {
        int index = periodCombo.getItems().indexOf(periodCombo.getValue());
        return index <= 0 ? null : months.get(index - 1);
    }

    private void load(YearMonth period) {
        HistoryService.View loaded = FormSupport.load(() -> historyService.getHistory(period), null);
        if (loaded == null) return;
        view = loaded;
        if (period == null) {   // the period choices come from the data, so they are filled on the all-time load
            months = loaded.months();
            loading = true;
            periodCombo.getItems().setAll(ALL_TIME);
            months.forEach(m -> periodCombo.getItems().add(m.format(MONTH)));
            periodCombo.setValue(ALL_TIME);
            loading = false;
        }
        kgLabel.setText(FormSupport.kg(view.kgRescued()));
        handoffsLabel.setText(String.valueOf(view.handoffs()));
        partnersLabel.setText(String.valueOf(view.partners()));
        allTab.setText("All (" + view.rows().size() + ")");
        rescuedTab.setText("Rescued (" + view.count(Outcome.RESCUED) + ")");
        expiredTab.setText("Expired (" + view.count(Outcome.EXPIRED) + ")");
        cancelledTab.setText("Cancelled (" + view.count(Outcome.CANCELLED) + ")");
        showTable();
    }

    private void showTable() {
        if (view == null) return;
        for (Button button : List.of(allTab, rescuedTab, expiredTab, cancelledTab)) button.getStyleClass().remove("tab-button-active");
        (tab == null ? allTab : tab == Outcome.RESCUED ? rescuedTab : tab == Outcome.EXPIRED ? expiredTab : cancelledTab)
                .getStyleClass().add("tab-button-active");
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Row> rows = view.rows().stream().filter(r -> tab == null || r.outcome() == tab)
                .filter(r -> needle.isEmpty() || r.food().toLowerCase(Locale.ROOT).contains(needle)
                        || (r.otherParty() != null && r.otherParty().toLowerCase(Locale.ROOT).contains(needle))).toList();
        boolean reports = rows.stream().anyMatch(r -> r.report() != null);   // the column exists only where the data has reports
        double[] widths = reports ? new double[] {13, 24, 8, 21, 16, 10, 8} : new double[] {14, 28, 8, 24, 16, 10};
        List<Node> lines = new ArrayList<>();
        if (!rows.isEmpty()) {
            lines.add(reports ? TableRows.header(widths, "DATE", "FOOD", "KG", "OTHER PARTY", "OUTCOME", "REPORT", "RECORD")
                    : TableRows.header(widths, "DATE", "FOOD", "KG", "OTHER PARTY", "OUTCOME", "RECORD"));
        }
        for (Row row : rows) lines.add(line(row, widths, reports));
        tableBox.getChildren().setAll(lines);
        emptyLabel.setVisible(rows.isEmpty());
        emptyLabel.setManaged(rows.isEmpty());
    }

    private GridPane line(Row r, double[] widths, boolean reports) {
        GridPane line = TableRows.row(widths);
        TableRows.put(line, 0, TableRows.text(r.when().format(DAY), "table-cell-muted"));
        TableRows.put(line, 1, TableRows.text(r.food(), "form-label"));
        TableRows.put(line, 2, TableRows.text(FormSupport.kg(r.kg()), "table-cell-text"));
        TableRows.put(line, 3, TableRows.text(r.otherParty(), "table-cell-text"));
        TableRows.put(line, 4, TableRows.text(r.outcomeText(), "table-cell-text"));
        if (reports) TableRows.put(line, 5, TableRows.text(r.report(), "table-cell-text"));
        if (r.recordOfferId() != null) {
            Hyperlink link = new Hyperlink("View record");
            link.getStyleClass().add("terms-link");
            link.setOnAction(e -> {
                Selection.selectOffer(r.recordOfferId());
                Navigator.show(Navigator.Screen.HANDOFF_RECORD);
            });
            TableRows.put(line, reports ? 6 : 5, link);
        } else {
            TableRows.put(line, reports ? 6 : 5, TableRows.text(null, "table-cell-muted"));
        }
        return line;
    }

    // PATTERN:ACTION tab filters of the loaded lines
    @FXML private void handleAll() { tab = null; showTable(); }
    @FXML private void handleRescued() { tab = Outcome.RESCUED; showTable(); }
    @FXML private void handleExpired() { tab = Outcome.EXPIRED; showTable(); }
    @FXML private void handleCancelled() { tab = Outcome.CANCELLED; showTable(); }
}
