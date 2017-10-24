package com.commune.client;

import javafx.scene.Scene;
import javafx.stage.Stage;

public interface WindowController {
    void setScene(Scene scene);
    void setStage(Stage stage);
    Stage getStage();
    Scene getScene();
}
