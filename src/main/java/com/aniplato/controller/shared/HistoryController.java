package com.aniplato.controller.shared;

import com.aniplato.model.Account;
import com.aniplato.util.Navigator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

/** Shared history screen with role-specific copy and sample records. */
public class HistoryController {

    @FXML private Label pageSubtitle;
    @FXML private Label rescuedValue;
    @FXML private Label handoffsValue;
    @FXML private Label partnersValue;
    @FXML private Label otherPartyHeader;
    @FXML private Label recordHeader;
    @FXML private Label historyStatus;
    @FXML private ComboBox<String> periodFilter;

    @FXML
    private void initialize() {
        periodFilter.setItems(FXCollections.observableArrayList("Last 30 days", "Last 6 months", "This year"));
        periodFilter.getSelectionModel().select("This year");

        if (Navigator.currentRole() == Account.Role.FOOD_GROUP) {
            pageSubtitle.setText("Review rescued food, pickups, and partner activity.");
            rescuedValue.setText("96.0");
            handoffsValue.setText("18");
            partnersValue.setText("6");
            otherPartyHeader.setText("DONOR");
            recordHeader.setText("REPORT");
            historyStatus.setText("Showing representative pickup and report records.");
        } else {
            pageSubtitle.setText("Review your donated food and completed handoffs.");
            rescuedValue.setText("128.5");
            handoffsValue.setText("24");
            partnersValue.setText("8");
            otherPartyHeader.setText("OTHER PARTY");
            recordHeader.setText("RECORD");
        }
    }

    // PATTERN:ACTION handlePeriodChanged
    @FXML
    private void handlePeriodChanged() {
        historyStatus.setText("Showing history for " + periodFilter.getValue() + ".");
    }

    // PATTERN:ACTION handleStatusFilter
    @FXML
    private void handleStatusFilter(ActionEvent event) {
        if (event.getSource() instanceof Button button) {
            historyStatus.setText("Showing " + button.getText().toLowerCase() + " history records.");
        }
    }

    // PATTERN:ACTION handleViewRecord
    @FXML
    private void handleViewRecord() {
        Navigator.show(Navigator.Screen.HANDOFF_RECORD);
    }
}
