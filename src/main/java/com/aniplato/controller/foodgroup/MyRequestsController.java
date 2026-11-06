package com.aniplato.controller.foodgroup;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.board.PostsBoard;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

/** My Requests: the food group's posts, with their offers, receipt and cancel. */
public class MyRequestsController {

    private final PostService postService = new PostService();
    private PostsBoard board;

    @FXML private VBox boardHost;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        board = new PostsBoard(PostsBoard.Kind.REQUESTS, dialogController, Navigator.Screen.FOODGROUP_OFFERS_RECEIVED, this::refresh);
        boardHost.getChildren().setAll(board.view());
        refresh();
    }

    private void refresh() {
        board.show(FormSupport.load(postService::getMyPosts, List.of()));
    }
}
