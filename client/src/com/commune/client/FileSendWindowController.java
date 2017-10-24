package com.commune.client;

import com.commune.model.User;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FileSendWindowController implements WindowController, Initializable {
    private Stage stage;
    private Scene scene;

    private User from;
    private User to;

    public void setTo(User to) {
        this.to = to;
    }

    public Scene getScene() {
        return scene;
    }
    public Stage getStage() {
        return stage;
    }
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public void setScene(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.from = App.CurrentUser;
    }

    static FileSendWindowController newFileSendWindow(Class c, User to) {
        try {
            Stage stage = new Stage();

            FXMLLoader fxmlLoader = new FXMLLoader(c.getResource("FileSendWindow.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 600, 200);
            stage.setScene(scene);

            FileSendWindowController controller = fxmlLoader.getController();
            controller.setTo(to);
            controller.setScene(scene);
            controller.setStage(stage);
            App.WindowControllers.put("FILE_" + to.getName(), controller);

            stage.show();
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            App.WindowControllers.remove("FILE_" + to.getName());
            return null;
        }
    }
}
