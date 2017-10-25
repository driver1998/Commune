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
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

public class UserListWindowController implements Initializable, WindowController{

    @FXML
    Label usernameLabel;

    @FXML
    ListView<User> userListView;

    @FXML
    Button menuButton;

    private ContextMenu mainMenu;
    private ContextMenu contextMenu;

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

    private EventHandler<WindowEvent> UserListWindowCloseEventHandler = (event) -> {
        App.WindowControllers.remove(App.CONTROLLER_USER_LIST_WINDOW);

        Presence presence = new Presence(App.CurrentUser, null, Presence.TYPE_UNAVAILABLE);

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(presence);
            App.ElementSendQueue.notify();
        }
    };
    private EventHandler<ActionEvent> AddFriendMenuItemActionEventHandler = (event) -> {
        AddFriendWindowController.newAddFriendWindow(getClass());
    };
    private EventHandler<ActionEvent> AboutMenuItemActionEventHandler = (event) -> {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Project Commune\nVersion 1.0 (Build 20171025)");
        alert.showAndWait();
    };
    private EventHandler<ActionEvent> DeleteFriendMenuItemActionEventHandler = (event) -> {
        User currentItemSelected = userListView.getSelectionModel().getSelectedItem();
        if (currentItemSelected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "真的要删除 " + currentItemSelected.getName() + " 吗？", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.YES)) {
                Presence presence = new Presence(App.CurrentUser, currentItemSelected, Presence.TYPE_UNSUBSCRIBE);
                synchronized (App.ElementSendQueue) {
                    App.ElementSendQueue.add(presence);
                    App.ElementSendQueue.notify();
                }

                App.UserList.remove(currentItemSelected);
            }
        }
    };
    private EventHandler<ActionEvent> SendMessageMenuItemActionEventHandler = (event) -> {
        User currentItemSelected = userListView.getSelectionModel().getSelectedItem();
        if (currentItemSelected != null) {
            callSendMessage(currentItemSelected);
        }
    };

    private class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User item, boolean empty) {
            super.updateItem(item, empty);

            if (item != null) {
                VBox vbox = new VBox();
                Label usernameLabel = new Label(item.getName());
                Label statusLabel = new Label(item.isOnline()? "在线" : "离线");
                vbox.getChildren().addAll(usernameLabel, statusLabel);

                setGraphic(vbox);
            } else {
                setGraphic(null);
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        userListView.setCellFactory((ListView<User> user) -> new UserListCell());
        userListView.setItems(App.UserList);
        usernameLabel.setText(App.CurrentUser.getName());

        //主菜单
        mainMenu = new ContextMenu();
        MenuItem addFriendItem = new MenuItem("添加好友");
        addFriendItem.setOnAction(AddFriendMenuItemActionEventHandler);
        MenuItem aboutMenuItem = new MenuItem("关于");
        aboutMenuItem.setOnAction(AboutMenuItemActionEventHandler);
        SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
        mainMenu.getItems().addAll(addFriendItem, separatorMenuItem, aboutMenuItem);

        //右键菜单
        contextMenu = new ContextMenu();
        MenuItem deleteFriendItem = new MenuItem("删除好友");
        deleteFriendItem.setOnAction(DeleteFriendMenuItemActionEventHandler);
        MenuItem sendMessageItem = new MenuItem("发送消息");
        sendMessageItem.setOnAction(SendMessageMenuItemActionEventHandler);
        contextMenu.getItems().addAll(sendMessageItem, deleteFriendItem);

        //发送获取好友列表请求
        String id = UUID.randomUUID().toString();
        BuddyListOperations query =
                new BuddyListOperations(App.CurrentUser, null, id,
                        BuddyListOperations.OPERATION_NS_QUERY, null);

        synchronized (App.RequestIDs) {
            App.RequestIDs.add(id);
            App.RequestIDs.notify();
        }

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(query);
            App.ElementSendQueue.notify();
        }
    }

    @FXML
    void handleMenuButtonAction(ActionEvent event) {
        mainMenu.show(menuButton, Side.TOP, 0, 0);
    }

    @FXML
    void handleUserListViewClick(MouseEvent event) {
        User currentItemSelected = userListView.getSelectionModel().getSelectedItem();
        Object target = event.getTarget();

        if (currentItemSelected != null) {

            //ListCell里就这两个组件
            //所以这么判断相当于判断点击的是否为ListCell
            if (target instanceof VBox || target instanceof LabeledText) {
                //鼠标左键双击
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                    callSendMessage(currentItemSelected);

                } else if (event.getButton() == MouseButton.SECONDARY) {
                    //鼠标右键单击
                    contextMenu.show(userListView, event.getScreenX(), event.getScreenY());
                    return; //不然右键菜单又隐藏了
                }
            }
        }

        contextMenu.hide();
    }

    //打开消息窗口
    private void callSendMessage(User user) {
        if (App.WindowControllers.containsKey("USER_" + user.getName())) {
            WindowController controller = App.WindowControllers.get("USER_" + user.getName());
            controller.getStage().show();
            controller.getStage().requestFocus();
        } else {
            ChatWindowController.newChatWindow(getClass(), user);
        }
    }


    static UserListWindowController newUserListWindow(Class c) {
        try {
            Stage stage = new Stage();

            FXMLLoader fxmlLoader = new FXMLLoader(c.getResource("UserListWindow.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 300, 600);
            stage.setScene(scene);

            UserListWindowController controller = fxmlLoader.getController();
            controller.setScene(scene);
            controller.setStage(stage);
            App.WindowControllers.put(App.CONTROLLER_USER_LIST_WINDOW, controller);

            stage.setMaxWidth(300);
            stage.setMinWidth(300);
            stage.setOnCloseRequest(controller.UserListWindowCloseEventHandler);
            stage.show();

            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            App.WindowControllers.remove(App.CONTROLLER_ADD_FRIEND_WINDOW);
            return null;
        }
    }


}
