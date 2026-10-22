package com.aniplato.controller.foodgroup;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PhotoPicker;
import com.aniplato.service.ReportService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** The food group's Submit Report form (R5.3) for the handoff opened from the record. */
public class SubmitReportController {

    private final ReportService reportService = new ReportService();
    private int offerId;

    @FXML private Label dueLabel;
    @FXML private TextField eventNameField;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField peopleServedField;
    @FXML private TextArea descriptionArea;
    @FXML private PhotoPicker photoPicker;
    @FXML private CheckBox consentCheck;

    @FXML
    private void initialize() {
        offerId = Selection.offerId();
        ReportService.ReportDefaults defaults = FormSupport.load(() -> reportService.getDefaults(offerId), null);
        if (defaults != null) {
            eventNameField.setText(defaults.eventName());
            eventDatePicker.setValue(defaults.eventDate());   // empty when the offer did not state a date
        }
        var due = FormSupport.load(() -> reportService.getDueAt(offerId), null);
        dueLabel.setText(due == null ? "" : "Due " + FormSupport.when(due) + ". A late report is still accepted and shows as submitted late.");
    }

    // PATTERN:ACTION saves the report, then opens the handoff record
    @FXML
    private void handleSubmit() {
        boolean saved = FormSupport.attempt(() -> reportService.submitReport(new ReportService.ReportForm(offerId,
                eventNameField.getText(), eventDatePicker.getValue(),
                FormSupport.wholeNumber(peopleServedField.getText(), "people served"), descriptionArea.getText(),
                consentCheck.isSelected(), photoPicker.files())));
        if (saved) Navigator.show(Navigator.Screen.HANDOFF_RECORD);
    }

    // PATTERN:ACTION back to the handoff record
    @FXML
    private void handleBack() {
        Navigator.show(Navigator.Screen.HANDOFF_RECORD);
    }
}
