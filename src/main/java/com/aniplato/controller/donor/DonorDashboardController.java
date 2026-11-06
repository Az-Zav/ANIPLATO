package com.aniplato.controller.donor;

import com.aniplato.controller.Banners;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.TableRows;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.DashboardService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** The donor's dashboard: banners, four cards and the active listings. Reads one service call and shows it. */
public class DonorDashboardController {

    private static final double[] WIDTHS = {28, 10, 16, 12, 20, 14};

    private final DashboardService dashboardService = new DashboardService();

    @FXML private VBox alertBox;
    @FXML private Label welcomeLabel;
    @FXML private Label cityLabel;
    @FXML private Label openListingsValueLabel;
    @FXML private Label openListingsSubLabel;
    @FXML private Label awaitingValueLabel;
    @FXML private Label awaitingSubLabel;
    @FXML private Label rescuedValueLabel;
    @FXML private Label rescuedSubLabel;
    @FXML private Label neededValueLabel;
    @FXML private Label neededSubLabel;
    @FXML private VBox tableBox;
    @FXML private Label emptyLabel;

    @FXML
    private void initialize() {
        DashboardService.DonorDashboard d = FormSupport.load(dashboardService::getDonorDashboard, null);
        if (d == null) return;
        Banners.fill(alertBox, d.banners());
        welcomeLabel.setText("Welcome, " + d.name());
        cityLabel.setText(d.city());
        openListingsValueLabel.setText(String.valueOf(d.openListings()));
        openListingsSubLabel.setText(d.offersToReview() + (d.offersToReview() == 1 ? " offer to review" : " offers to review"));
        awaitingValueLabel.setText(String.valueOf(d.awaitingRelease()));
        awaitingSubLabel.setText("Confirm release at pickup");
        rescuedValueLabel.setText(FormSupport.kg(d.kgRescued()));
        rescuedSubLabel.setText("Total rescued to date");
        neededValueLabel.setText(FormSupport.kg(d.kgNeeded()));
        neededSubLabel.setText("Across " + d.openRequests() + (d.openRequests() == 1 ? " open request" : " open requests"));

        tableBox.getChildren().clear();
        if (!d.activeListings().isEmpty()) {
            tableBox.getChildren().add(TableRows.header(WIDTHS, "FOOD", "KG", "USE-BY", "STATUS", "ACCEPTED OFFER", "PICKUP"));
        }
        for (Post post : d.activeListings()) tableBox.getChildren().add(row(post));
        emptyLabel.setVisible(d.activeListings().isEmpty());
        emptyLabel.setManaged(d.activeListings().isEmpty());
    }

    private static GridPane row(Post post) {
        Offer accepted = post.acceptedOffer;
        GridPane row = TableRows.row(WIDTHS);
        TableRows.put(row, 0, TableRows.text(post.food.description, "form-label"));
        TableRows.put(row, 1, TableRows.text(FormSupport.kg(post.kg), "table-cell-text"));
        TableRows.put(row, 2, TableRows.text(FormSupport.when(post.food.useBy), "table-cell-text"));
        TableRows.put(row, 3, TableRows.text(post.status.label(), "table-cell-text"));
        TableRows.put(row, 4, TableRows.text(accepted == null ? null : accepted.offererName, "table-cell-text"));
        TableRows.put(row, 5, TableRows.text(accepted == null ? null : FormSupport.when(accepted.pickupDeadline()), "table-cell-text"));
        return row;
    }
}
