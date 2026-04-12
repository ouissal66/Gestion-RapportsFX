package com.audit.auditaifx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        // Ajouter le CSS (doit être fait avant de montrer le stage pour éviter le
        // flash)
        String css = MainApp.class.getResource("audit.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("AuditAI - Gestion des Rapports d'Audit");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(550);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}