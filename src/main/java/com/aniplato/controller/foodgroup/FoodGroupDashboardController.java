package com.aniplato.controller.foodgroup;

import com.aniplato.controller.Banners;
import com.aniplato.controller.Cards;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.TableRows;
import com.aniplato.model.Post;
import com.aniplato.service.DashboardService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** The food group's dashboard: banners, three cards and the nearest open listings. Reads one service call and shows it. */
public class FoodGroupDashboardController {

    private static final double[] WIDTHS = {30, 26, 10, 22, 12};

    private final DashboardService dashboardService = new DashboardService();

    @FXML private VBox alertBox;
    @FXML private Label welcomeLabel;
    @FXML private Label cityLabel;
    @FXML private Label openListingsValueLabel;
    @FXML private Label openListingsSubLabel;
    @FXML private Label acceptedValueLabel;
    @FXML private Label acceptedSubLabel;
    @FXML private Label rescuedValueLabel;
    @FXML private Label rescuedSubLabel;
    @FXML private VBox tableBox;
    @FXML private Label emptyLabel;

    @FXML
    private void initialize() {
        DashboardService.FoodGroupDashboard d = FormSupport.load(dashboardService::getFoodGroupDashboard, null);
        if (d == null) return;
        Banners.fill(alertBox, d.banners());
        welcomeLabel.setText("Welcome, " + d.name());
        cityLabel.setText(d.city());
        openListingsValueLabel.setText(String.valueOf(d.openListings()));
        openListingsSubLabel.setText(FormSupport.kg(d.kgWaiting()) + " waiting · " + d.inMyCity() + " in your city");
        acceptedValueLabel.setText(String.valueOf(d.acceptedOffers()));
        acceptedSubLabel.setText("Confirm receipt after the donor releases");
        rescuedValueLabel.setText(FormSupport.kg(d.kgRescued()));
        rescuedSubLabel.setText("Total rescued to date");

        tableBox.getChildren().clear();
        if (!d.nearListings().isEmpty()) tableBox.getChildren().add(TableRows.header(WIDTHS, "FOOD", "DONOR", "KG", "USE-BY", ""));
        for (Post post : d.nearListings()) tableBox.getChildren().add(row(post));
        emptyLabel.setVisible(d.nearListings().isEmpty());
        emptyLabel.setManaged(d.nearListings().isEmpty());
    }

    private static GridPane row(Post post) {
        GridPane row = TableRows.row(WIDTHS);
        TableRows.put(row, 0, TableRows.text(post.food.description, "form-label"));
        TableRows.put(row, 1, TableRows.text(post.posterName, "table-cell-text"));
        TableRows.put(row, 2, TableRows.text(FormSupport.kg(post.kg), "table-cell-text"));
        TableRows.put(row, 3, TableRows.text(FormSupport.when(post.food.useBy), "table-cell-text"));
        TableRows.put(row, 4, Cards.primary("Make offer", () -> {
            Selection.selectPost(post.id);
            Navigator.show(Navigator.Screen.MAKE_OFFER);
        }));
        return row;
    }
}
