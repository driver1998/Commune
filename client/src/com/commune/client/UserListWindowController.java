package com.commune.client;

import com.commune.model.User;
import com.commune.stream.BuddyListOperations;
import com.commune.stream.Presence;
import com.sun.javafx.scene.control.skin.LabeledText;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

public class UserListWindowController implements Initializable, WindowController{

    @FXML
    Label usernameLabel;

    @FXML
    ListView<User> userListView;

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

    EventHandler<WindowEvent> UserListWindowCloseEventHandler = (event) -> {
        Presence presence = new Presence(App.CurrentUser, null, Presence.TYPE_UNAVAILABLE);

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(presence);
            App.ElementSendQueue.notify();
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        userListView.setCellFactory((ListView<User> user) -> new UserListCell());
        usernameLabel.setText(App.CurrentUser.getName());

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

        userListView.setItems(App.userList);
    }

    @FXML
    void handleUserListViewClick(MouseEvent event) {

        if (event.getClickCount() == 2)
        {
            User currentItemSelected = userListView.getSelectionModel().getSelectedItem();

            Object target = event.getTarget();
            if (currentItemSelected != null) {

                if (target instanceof VBox || target instanceof LabeledText) {
                    if (App.WindowControllers.containsKey("USER_" + currentItemSelected.getName())) {
                        WindowController controller = App.WindowControllers.get("USER_" + currentItemSelected.getName());
                        controller.getStage().show();
                        controller.getStage().requestFocus();
                    } else {
                        ChatWindowController.newChatWindow(getClass(), currentItemSelected);
                    }
                }
            }
        }

    }
    class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User item, boolean empty) {
            super.updateItem(item, empty);

            if (item != null) {
                VBox vbox = new VBox();
                Label usernameLabel = new Label(item.getName());
                Label statusLabel = new Label("在线");
                vbox.getChildren().addAll(usernameLabel, statusLabel);

                setGraphic(vbox);
            } else {
                setGraphic(null);
            }
        }
    }
}
