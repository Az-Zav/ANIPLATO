package com.aniplato.controller.shared.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Shows how a closed safety notice was resolved, with the admin's note. Only reads; Close is DialogCard's cancel. */
public class NoticeResolvedDialogController extends DialogCard {

    @FXML private Label noticeResolvedTitleLabel;
    @FXML private Label noticeResolvedNoteLabel;

    /** {@code how} is "Dismissed" or "Account suspended"; a missing note shows a dash. */
    public void show(String how, String adminNote) {
        open();
        noticeResolvedTitleLabel.setText(how);
        noticeResolvedNoteLabel.setText("Admin note: " + (adminNote == null ? "—" : adminNote));
    }
}
