package com.aniplato;

import com.aniplato.model.Account;
import com.aniplato.util.DevPreview;
import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import com.aniplato.util.Session;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Baseline 1280x720 scene; Navigator swaps its root
        Scene scene = new Scene(new Pane(), 1280, 720);
        Navigator.init(scene);
        Account preview = DevPreview.fromEnvironment();   // TEMPORARY: see DevPreview
        if (preview != null) {
            Session.start(preview);
            Navigator.show(Screen.SHELL);
        } else {
            Navigator.show(Screen.LOGIN);
        }

        primaryStage.setTitle("ANIPLATO - Surplus Food Rescue System");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}