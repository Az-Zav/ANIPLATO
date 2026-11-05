package com.aniplato.controller.foodgroup;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.board.OffersPage;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.service.AcceptanceService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/** Offers for this request: the offers waiting on the request opened from My Requests, accept or decline. */
public class FoodGroupIncomingOffersController {

    private final AcceptanceService acceptanceService = new AcceptanceService();
    private OffersPage page;

    @FXML private VBox pageHost;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        page = new OffersPage(OffersPage.Kind.REQUEST, dialogController, Navigator.Screen.MY_REQUESTS, this::refresh);
        pageHost.getChildren().setAll(page.view());
        refresh();
    }

    private void refresh() {
        page.show(FormSupport.load(() -> acceptanceService.getOffersOn(Selection.postId()), null));
    }
}
