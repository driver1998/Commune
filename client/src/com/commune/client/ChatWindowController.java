package com.commune.client;

import com.commune.model.User;
import com.commune.stream.FileMessage;
import com.commune.stream.Message;
import com.commune.utils.Util;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

public class ChatWindowController implements Initializable, WindowController{
    @FXML
    Label toUserLabel;

    @FXML
    TextArea messageTextArea;

    @FXML
    TextArea conversationTextArea;

    @FXML
    Button sendButton;

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

    private User from;
    private User to;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        from = App.CurrentUser;
        conversationTextArea.setEditable(false);
        sendButton.requestFocus();
    }

    void setTo(User to) {
        this.to = to;
        toUserLabel.setText(to.getName());
    }

    EventHandler<WindowEvent> ChatWindowCloseEventHandler = (event) -> {
        App.WindowControllers.remove("USER_" + to.getName());
    };

    @FXML
    void onSendButtonAction(ActionEvent event) {
        if (messageTextArea.getText().isEmpty()) return;
        Message message = new Message(from, to, messageTextArea.getText());
        System.out.println(message.getXML());

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(message);
            App.ElementSendQueue.notify();
        }

        appendMessage(message);
        messageTextArea.clear();
    }

    @FXML
    void onFileSendButtonAction(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION ,
                "准备发送文件:\n" +
                        file.getPath() +
                        "\n文件大小 " + Util.getHumanReadableFileLength(file.length()) +
                        "\n真的要继续吗？",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get().equals(ButtonType.YES)) {
            appendMessage(new Message(from,to, "正在传输文件 " + file.getName() + "..."));
            String ID = UUID.randomUUID().toString();
            FileMessage fileMessage = new FileMessage(from, to, file, file.getName(), file.length(), ID);
            synchronized (App.RequestIDs) {
                App.RequestIDs.add(ID);
                App.RequestIDs.notify();
            }
            synchronized (App.ElementSendQueue) {
                App.ElementSendQueue.add(fileMessage);
                App.ElementSendQueue.notify();
            }
        }
    }

    void appendMessage(Message message) {
        String msg = "  " + message.getBody().replace("\n", "\n  ");
        conversationTextArea.appendText(message.getFrom().getName() + ":\n" + msg + "\n");
    }

    static ChatWindowController newChatWindow(Class c, User to) {
        try {
            Stage stage = new Stage();

            FXMLLoader fxmlLoader = new FXMLLoader(c.getResource("ChatWindow.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);

            ChatWindowController controller = fxmlLoader.getController();
            controller.setTo(to);
            controller.setScene(scene);
            controller.setStage(stage);
            stage.setOnCloseRequest(controller.ChatWindowCloseEventHandler);
            App.WindowControllers.put("USER_" + to.getName(), controller);

            stage.show();
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            App.WindowControllers.remove("USER_" + to.getName());
            return null;
        }
    }
}
