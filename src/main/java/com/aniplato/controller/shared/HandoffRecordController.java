package com.aniplato.controller.shared;

import com.aniplato.controller.Cards;
import com.aniplato.controller.FormSupport;
import com.aniplato.model.HandoffRecord;
import com.aniplato.model.Report;
import com.aniplato.service.OfferService;
import com.aniplato.service.ReportService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** The handoff record and, once submitted, its report, read-only. Shared screen: it never checks the role (A5). */
public class HandoffRecordController {

    private final OfferService offerService = new OfferService();
    private final ReportService reportService = new ReportService();
    private int offerId;

    @FXML private Button submitReportButton;
    @FXML private VBox recordBox;
    @FXML private Label disclaimerLabel;

    @FXML
    private void initialize() {
        offerId = Selection.offerId();
        HandoffRecord record = FormSupport.load(() -> offerService.getHandoffRecord(offerId), null);
        boolean canSubmit = record != null && FormSupport.load(() -> reportService.canSubmit(offerId), false);
        submitReportButton.setVisible(canSubmit);
        submitReportButton.setManaged(canSubmit);
        if (record == null) return;
        recordBox.getChildren().add(recordCard(record));
        Report report = FormSupport.load(() -> reportService.getReport(offerId), null);
        recordBox.getChildren().add(report == null ? reportDueCard(record) : reportCard(report, record));
        disclaimerLabel.setText(HandoffRecord.DISCLAIMER);
    }

    private static VBox recordCard(HandoffRecord r) {
        return Cards.card(Cards.title(r.number()),
                Cards.line("Donor: " + r.donorName() + "  |  Food group: " + r.groupName()),
                Cards.line(r.foodDescription() + " (" + r.category().label() + ")  |  " + FormSupport.kg(r.agreedKg())),
                Cards.muted("Released " + FormSupport.when(r.releasedAt()) + "  |  Received " + FormSupport.when(r.receivedAt())),
                Cards.muted("Stated use: " + r.useDescription() + "  |  " + r.headcount() + " people: " + r.beneficiaries() + "  |  " + r.place()));
    }

    private static VBox reportDueCard(HandoffRecord r) {
        return Cards.card(Cards.title("Report"), Cards.muted("Not submitted yet. Due " + FormSupport.when(r.reportDueAt()) + "."));
    }

    private static VBox reportCard(Report report, HandoffRecord r) {
        VBox card = Cards.card(Cards.title("Report" + (report.isLate(r.reportDueAt()) ? " (submitted late)" : "")),
                Cards.line(report.eventName + "  |  " + report.eventDate + "  |  " + report.peopleServed + " people served"),
                Cards.line(report.description),
                Cards.muted(report.photos.size() + " photos  |  submitted " + FormSupport.when(report.submittedAt)));
        return card;
    }

    // PATTERN:ACTION opens the Submit Report form for this handoff
    @FXML
    private void handleSubmitReport() {
        Selection.selectOffer(offerId);
        Navigator.show(Navigator.Screen.FOODGROUP_SUBMIT_REPORT);
    }

    // PATTERN:ACTION back to History
    @FXML
    private void handleBack() {
        Navigator.show(Navigator.Screen.HISTORY);
    }
}
