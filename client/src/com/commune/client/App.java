package com.commune.client;

import com.commune.model.User;
import com.commune.stream.DataElement;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class App extends Application {

    static HashMap<String, WindowController> WindowControllers;
    static final String CONTROLLER_LOGIN_WINDOW = "CONTROLLER_LOGIN_WINDOW";
    static final String CONTROLLER_USER_LIST_WINDOW = "CONTROLLER_USER_LIST_WINDOW";


    static User CurrentUser;
    static Socket Socket;

    static Thread SendThread;
    static SendTask SendTask;
    static Thread ConnectionThread;
    static ConnectionTask ConnectionTask;

    static ObservableList<User> userList = FXCollections.observableArrayList();

    final static ArrayList<String> RequestIDs = new ArrayList<>();
    public final static ArrayList<DataElement> ElementSendQueue = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException{
        WindowControllers = new HashMap<>();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LoginWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 360, 600);
        primaryStage.setScene (scene);

        LoginWindowController controller = fxmlLoader.getController();
        controller.setScene(scene);
        controller.setStage(primaryStage);
        WindowControllers.put(CONTROLLER_LOGIN_WINDOW, controller);

        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
