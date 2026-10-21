package com.aniplato.controller.foodgroup;

import com.aniplato.util.Navigator;
import javafx.fxml.FXML;

/** Presentation-only report form until the report service is delivered. */
public class SubmitReportController {

    // PATTERN:ACTION placeholder: submits the selected handoff report
    @FXML
    private void handleSubmit() {
        // TODO(service): reportService.submit(...)
        Navigator.show(Navigator.Screen.HISTORY);
    }

    // PATTERN:NAV -> MY_REQUESTS
    @FXML
    private void handleCancel() {
        Navigator.show(Navigator.Screen.MY_REQUESTS);
    }
}
