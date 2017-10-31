package com.commune.client;

import com.commune.model.User;
import com.commune.stream.DataElement;
import com.commune.utils.Util;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class App extends Application {

    static HashMap<String, WindowController> WindowControllers;
    static final String CONTROLLER_LOGIN_WINDOW = "CONTROLLER_LOGIN_WINDOW";
    static final String CONTROLLER_USER_LIST_WINDOW = "CONTROLLER_USER_LIST_WINDOW";
    static final String CONTROLLER_ADD_FRIEND_WINDOW = "CONTROLLER_ADD_FRIEND_WINDOW";

    static User CurrentUser;
    static Socket Socket;

    static Thread SendThread;
    static SendTask SendTask;
    static Thread ConnectionThread;
    static ConnectionTask ConnectionTask;

    static ObservableList<User> UserList = FXCollections.observableArrayList();

    final static ArrayList<String> RequestIDs = new ArrayList<>();
    final static ArrayList<DataElement> ElementSendQueue = new ArrayList<>();

    //默认值
    static String server = "127.0.0.1";
    static int port = 4074;

    private static void getSettings() {
        try {
            BufferedReader reader =new BufferedReader(new FileReader("commune.config"));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Document document = Util.parseXmlString(sb.toString());
            Element root = document.getDocumentElement();
            Element clientElement = (Element) root.getElementsByTagName("client").item(0);

            server = clientElement.getElementsByTagName("server").item(0).getTextContent();
            port = Integer.valueOf(clientElement.getElementsByTagName("port").item(0).getTextContent());

        } catch (IOException | ParserConfigurationException | SAXException ex) {
            ex.printStackTrace();
        }

    }
    public static void main(String[] args) {
        getSettings();
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
