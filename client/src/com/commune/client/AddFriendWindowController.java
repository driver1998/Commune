package com.commune.client;

import com.commune.model.User;
import com.commune.stream.BuddyListOperations;
import com.commune.stream.Presence;
import com.sun.javafx.scene.control.skin.LabeledText;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.swing.*;

import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class AddFriendWindowController implements WindowController, Initializable{

    private Scene scene;
    private Stage stage;
    private ObservableList<User> userList = FXCollections.observableArrayList();

    public Stage getStage() {
        return stage;
    }
    public Scene getScene() {
        return scene;
    }
    public void setScene(Scene scene) {
        this.scene = scene;
    }
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public ObservableList<User> getUserList() {
        return userList;
    }

    @FXML
    ListView<User> userListView;

    @FXML
    Button searchButton;

    @FXML
    TextField keywordTextField;

    @FXML
    void handleSearchButtonAction(ActionEvent event) {
        userListView.setVisible(true);
        stage.setHeight(300);
        stage.setResizable(true);

        String id = UUID.randomUUID().toString();
        BuddyListOperations operations = new BuddyListOperations(
                App.CurrentUser, null, id, BuddyListOperations.OPERATION_NS_SEARCH,
                keywordTextField.getText(), null);

        synchronized (App.RequestIDs) {
            App.RequestIDs.add(id);
            App.RequestIDs.notify();
        }

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(operations);
            App.ElementSendQueue.notify();
        }

    }

    @FXML
    void handleUserListViewClick(MouseEvent event) {
        if (event.getClickCount() == 2)
        {
            User currentItemSelected = userListView.getSelectionModel().getSelectedItem();

            Object target = event.getTarget();
            if (currentItemSelected != null) {

                //ListCell里就这个组件
                //所以这么判断相当于判断双击的是否为ListCell
                if (target instanceof LabeledText) addFriend(currentItemSelected);
            }
        }
    }

    private void addFriend(User user) {
        //已经是好友了
        if (App.UserList.contains(user)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "你们已经是好友了！");
            alert.show();
            return;
        }

        //发送好友请求
        String id = UUID.randomUUID().toString();
        Presence presence = new Presence(App.CurrentUser, user, Presence.TYPE_SUBSCRIBE, id);

        /*
        synchronized (App.RequestIDs) {
            App.RequestIDs.add(id);
            App.RequestIDs.notify();
        }
        */

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(presence);
            App.ElementSendQueue.notify();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "好友请求已发送。");
        alert.show();
    }

    private EventHandler<WindowEvent> AddFriendWindowCloseEventHandler = (event) -> {
        App.WindowControllers.remove(App.CONTROLLER_ADD_FRIEND_WINDOW);
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userListView.setItems(userList);
    }

    static AddFriendWindowController newAddFriendWindow(Class c) {
        try {
            Stage stage = new Stage();

            FXMLLoader fxmlLoader = new FXMLLoader(c.getResource("AddFriendWindow.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 400, 40);
            stage.setScene(scene);
            stage.setResizable(false);

            AddFriendWindowController controller = fxmlLoader.getController();
            controller.setScene(scene);
            controller.setStage(stage);
            stage.setOnCloseRequest(controller.AddFriendWindowCloseEventHandler);
            App.WindowControllers.put(App.CONTROLLER_ADD_FRIEND_WINDOW, controller);

            stage.setMinWidth(400);
            stage.setMaxWidth(400);
            stage.show();
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            App.WindowControllers.remove(App.CONTROLLER_ADD_FRIEND_WINDOW);
            return null;
        }
    }
}
