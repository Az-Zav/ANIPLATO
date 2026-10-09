package com.aniplato;

import javafx.application.Application;
import javafx.stage.Stage;

/** Entry point. Opens the database, then shows the login screen inside the shell. */
public class Main extends Application {
    @Override
    public void start(Stage stage) {
        // TODO Phase 4: Navigator.init(stage); Navigator.show("login");
        stage.setTitle("ANIPLATO");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
