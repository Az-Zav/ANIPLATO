package com.aniplato.controller.foodgroup;

import com.aniplato.controller.Cards;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.MyPostRows;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.model.Post;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/** My Requests: the food group's posts, with their offers, receipt and cancel. */
public class MyRequestsController {

    private final PostService postService = new PostService();
    private MyPostRows rows;

    @FXML private VBox rowsBox;
    @FXML private Label emptyLabel;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        rows = new MyPostRows(dialogController, Navigator.Screen.FOODGROUP_OFFERS_RECEIVED, this::refresh);
        refresh();
    }

    private void refresh() {
        List<Post> posts = FormSupport.load(postService::getMyPosts, List.of());
        Cards.fill(rowsBox, emptyLabel, rows.build(posts));
    }
}
