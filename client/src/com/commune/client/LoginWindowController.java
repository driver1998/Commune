package com.commune.client;

import com.commune.stream.Auth;
import com.commune.utils.Util;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class LoginWindowController implements WindowController{

    @FXML
    TextField usernameField;

    @FXML
    PasswordField passwordField;

    private Stage stage;
    private Scene scene;
    public void setScene(Scene scene) {
        this.scene = scene;
    }
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public Stage getStage() {
        return stage;
    }
    public Scene getScene() {
        return scene;
    }

    @FXML
    protected void onLoginButtonAction(ActionEvent actionEvent) {

        try {
            App.Socket = new Socket("192.168.1.229", 4074);
            App.Socket.setKeepAlive(true);

            App.SendTask = new SendTask();
            App.SendThread = new Thread(App.SendTask);
            App.SendThread.setDaemon(true);
            App.SendThread.start();

            App.ConnectionTask = new ConnectionTask();
            App.ConnectionThread = new Thread(App.ConnectionTask);
            App.ConnectionThread.setDaemon(true);
            App.ConnectionThread.start();

            String id = UUID.randomUUID().toString();

            String username = usernameField.getText();
            String hash1 = Util.getSHA1("COMMUNE" + username + passwordField.getText());
            Auth auth = new Auth(username, hash1, id);

            synchronized (App.RequestIDs) {
                App.RequestIDs.add(id);
                App.RequestIDs.notify();
            }

            synchronized (App.ElementSendQueue) {
                App.ElementSendQueue.add(auth);
                App.ElementSendQueue.notify();
            }
        } catch (IOException e) {
            try {
                App.Socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
