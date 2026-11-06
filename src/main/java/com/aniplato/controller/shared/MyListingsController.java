package com.aniplato.controller.shared;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.board.PostsBoard;
import com.aniplato.controller.shared.DialogController;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

/** My Listings. Shared screen: it asks for "my" posts and never checks the role (A5). */
public class MyListingsController {

    private final PostService postService = new PostService();
    private PostsBoard board;

    @FXML private VBox boardHost;
    @FXML private DialogController dialogController;

    @FXML
    private void initialize() {
        board = new PostsBoard(PostsBoard.Kind.LISTINGS, dialogController, Navigator.Screen.LISTING_OFFERS, this::refresh);
        boardHost.getChildren().setAll(board.view());
        refresh();
    }

    private void refresh() {
        board.show(FormSupport.load(postService::getMyPosts, List.of()));
    }
}
