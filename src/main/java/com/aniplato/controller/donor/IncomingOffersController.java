package com.aniplato.controller.donor;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.board.OffersPage;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.service.AcceptanceService;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/** Offers for this listing: the offers waiting on the listing opened from My Listings, accept or decline. */
public class IncomingOffersController {

    private final AcceptanceService acceptanceService = new AcceptanceService();
    private OffersPage page;

    @FXML private VBox pageHost;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        page = new OffersPage(OffersPage.Kind.LISTING, dialogController, Navigator.Screen.MY_LISTINGS, this::refresh);
        pageHost.getChildren().setAll(page.view());
        refresh();
    }

    private void refresh() {
        page.show(FormSupport.load(() -> acceptanceService.getOffersOn(Selection.postId()), null));
    }
}
