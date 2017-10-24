package com.commune.client;

import com.commune.model.User;
import com.commune.stream.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;

import static com.commune.client.App.ElementSendQueue;
import static com.commune.client.App.RequestIDs;

class SendTask extends Task<Integer> {
    volatile boolean stop = false;

    @Override
    protected Integer call() throws Exception {
        while(!stop) {
            sendElements();
        }
        return 0;
    }

    private void sendElements() {
        synchronized (ElementSendQueue){
            if (ElementSendQueue.isEmpty()) {
                try {
                    ElementSendQueue.wait();
                } catch (InterruptedException ex) {
                    return;
                }
            }

            Iterator<DataElement> i = ElementSendQueue.iterator();
                    while(i.hasNext()) {
                DataElement e = i.next();
                e.send(App.Socket);
                if (e instanceof FileMessage) {
                    sendFile((FileMessage)e, App.Socket);
                }
                i.remove();
            }
        }
    }

    private void sendFile(FileMessage message, Socket socket) {
        if (message.getFile() == null) return;
        try (FileInputStream fileInputStream = new FileInputStream(message.getFile())) {
            byte[] bytes = new byte[1048576];
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            long sentLength = 0;
            int length;
            while((length = fileInputStream.read(bytes, 0, bytes.length))!=-1) {
                outputStream.write(bytes, 0, length);
                sentLength+=(long)length;
                outputStream.flush();
                System.out.println(String.valueOf(sentLength) + " " + String.valueOf(message.getSize()) + " " + String.valueOf(message.getFile().length()));
            }

            System.out.println("file send finished");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}

class ConnectionTask extends Task<Integer> {
    volatile boolean stop = false;

    @Override
    protected Integer call() throws Exception {
        try{
            while(!stop) {
                processElements();
            }
        } catch (InvalidElementException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private void processElements() throws InvalidElementException {
        try {
            System.out.println("ConnectionTask.processElements");
            DataElement element = DataElement.getElement(App.Socket);

            if (element instanceof Result) {
                processResult((Result)element);
            } else if (element instanceof Message) {
                processMessage((Message)element);
            } else if (element instanceof Presence) {

            } else if (element instanceof BuddyListOperations) {
                processBuddyListOperations((BuddyListOperations)element);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Message message) {

        if (message.getFrom() == null) return;
        String from = message.getFrom().getName();
        System.out.println("receive message from" + from);

        Platform.runLater( () -> {
            ChatWindowController controller;
            if (App.WindowControllers.containsKey("USER_" + from)) {
                controller = (ChatWindowController)App.WindowControllers.get("USER_" + from);
                controller.getStage().show();
                controller.getStage().requestFocus();
            } else {
                controller = ChatWindowController.newChatWindow(getClass(), message.getFrom());
                if (controller == null) return;
            }
            controller.appendMessage(message);
        });

    }

    private void processResult(Result result){

        synchronized (RequestIDs) {
            if (RequestIDs.isEmpty()) {
                try {
                    RequestIDs.wait();
                } catch (InterruptedException ex) {
                    return;
                }
            }
            if (!result.getId().isEmpty() && !RequestIDs.contains(result.getId())) {
                return;
            }

            RequestIDs.remove(result.getId());
        }

        if (result.getType().equals(Result.TYPE_ERROR)) {
            Platform.runLater(()-> {
                Alert alert = new Alert(Alert.AlertType.ERROR, result.getBody(), ButtonType.CLOSE);
                alert.showAndWait();
            });
        } else {
            switch (result.getBody()) {
                case Result.BODY_LOGIN_SUCCESS:
                    processLoginResult(result);
                    break;
            }
        }
    }
    private void processBuddyListOperations(BuddyListOperations operations) {
        synchronized (RequestIDs) {
            if (RequestIDs.isEmpty()) {
                try {
                    RequestIDs.wait();
                } catch (InterruptedException ex) {
                    return;
                }
            }
            if (!operations.getId().isEmpty() && !RequestIDs.contains(operations.getId())) {
                return;
            }
            RequestIDs.remove(operations.getId());
        }

        if (operations.getOperation().equals(BuddyListOperations.OPERATION_NS_QUERY)) {
            if (App.userList!=null) {
                System.out.println("this is observableList");
                System.out.println(operations.getItems());
                App.userList.setAll(operations.getItems());
            }
        }


    }

    private void processLoginResult(Result result) {
        User user = new User(result.getTo());

        Presence presence = new Presence(user, null, Presence.TYPE_AVAILABLE);

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(presence);
            App.ElementSendQueue.notify();
        }

        Platform.runLater( () -> loginCallback(user) );
    }
    private void loginCallback(User user){
        if (App.WindowControllers.containsKey(App.CONTROLLER_LOGIN_WINDOW)) {
            WindowController controller = App.WindowControllers.get(App.CONTROLLER_LOGIN_WINDOW);
            App.WindowControllers.remove(App.CONTROLLER_LOGIN_WINDOW);
            controller.getStage().close();
        }

        App.CurrentUser = user;

        try {
            Stage stage = new Stage();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("UserListWindow.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 300, 600);
            stage.setScene(scene);

            UserListWindowController controller= fxmlLoader.getController();
            controller.setScene(scene);
            controller.setStage(stage);
            App.WindowControllers.put(App.CONTROLLER_USER_LIST_WINDOW, controller);

            stage.setMaxWidth(300);
            stage.setMinWidth(300);
            stage.setOnCloseRequest(controller.UserListWindowCloseEventHandler);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            App.WindowControllers.remove(App.CONTROLLER_USER_LIST_WINDOW);
        }
    }
}
