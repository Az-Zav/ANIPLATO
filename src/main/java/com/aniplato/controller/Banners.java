package com.aniplato.controller;

import com.aniplato.service.StatsService.Banner;
import com.aniplato.util.Navigator;
import com.aniplato.util.Selection;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** The warning banners above a dashboard's cards, each with a link to the screen that fixes it. */
public final class Banners {

    private Banners() { }

    public static void fill(VBox box, List<Banner> banners) {
        box.getChildren().clear();
        for (Banner item : banners) {
            Label text = new Label(item.text());
            text.getStyleClass().add("banner-text");
            text.setWrapText(true);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Hyperlink link = new Hyperlink(switch (item.kind()) {
                case REPORT_DUE -> "Submit report →";
                default -> "Go to Profile →";
            });
            link.getStyleClass().add("banner-link");
            link.setOnAction(e -> {
                if (item.offerId() != 0) {
                    Selection.selectOffer(item.offerId());
                    Navigator.show(Navigator.Screen.FOODGROUP_SUBMIT_REPORT);
                } else {
                    Navigator.show(Navigator.Screen.PROFILE);
                }
            });
            HBox row = new HBox(12, text, spacer, link);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("banner");
            box.getChildren().add(row);
        }
    }
}
