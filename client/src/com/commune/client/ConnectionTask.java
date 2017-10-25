package com.commune.client;

import com.commune.model.User;
import com.commune.stream.*;
import com.commune.utils.Util;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.Optional;

import static com.commune.client.App.ElementSendQueue;
import static com.commune.client.App.RequestIDs;


//发送element到服务器的线程
class SendTask extends Task<Integer> {
    volatile boolean stop = false; //结束线程的标志

    //指示文件传输进程
    static final int FILE_ACCEPTED = 1;  //对方接受了文件，马上开始传输
    static final int FILE_REJECTED = -1; //对方不接受文件
    static final int FILE_WAITING = 2;   //文件准备发送，等待对方接受
    static final int FILE_DEFAULT = 0;   //默认状态，无意义
    final Integer[] FileTransferStatus = {FILE_DEFAULT};

    @Override
    protected Integer call() throws Exception {
        while(!stop) {
            sendElements();
        }
        return 0;
    }

    //将elementSendQueue中的element全部发出去
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

                //如果是文件发送请求，阻塞掉发送线程，改为发文件
                //不是特别好的方法，但起码能用
                if (e instanceof FileMessage) {
                    sendFile((FileMessage)e, App.Socket);
                }
                i.remove();
            }
        }
    }

    //发送文件
    private void sendFile(FileMessage message, Socket socket) {
        if (message.getFile() == null) return;

        synchronized (FileTransferStatus) {
            //等待对方发的ACCEPT result
            //发过来的时候，processResult会将这里的fileTransferStatus改为FILE_ACCEPTED
            FileTransferStatus[0] = FILE_WAITING;
            try {
                FileTransferStatus.wait();
            } catch (InterruptedException e) {
                return;
            }

            if (stop || FileTransferStatus[0] == FILE_REJECTED) return;
            FileTransferStatus[0] = FILE_DEFAULT;

            try (FileInputStream fileInputStream = new FileInputStream(message.getFile())) {
                byte[] bytes = new byte[1048576];
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
}

//接收服务器发的element，并处理
class ConnectionTask extends Task<Integer> {
    volatile boolean stop = false; //线程结束标志

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

    //接收element并按类型分别处理
    private void processElements() throws InvalidElementException {
        try {
            System.out.println("ConnectionTask.processElements");
            DataElement element = DataElement.getElement(App.Socket);

            if (element instanceof Result) {
                processResult((Result)element);
            } else if (element instanceof Message) {
                processMessage((Message)element);
            } else if (element instanceof Presence) {
                processPresence((Presence)element);
            } else if (element instanceof BuddyListOperations) {
                processBuddyListOperations((BuddyListOperations)element);
            } else if (element instanceof FileMessage) {
                processFileMessage((FileMessage)element);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processPresence(Presence presence) {
        switch (presence.getType()) {
            case Presence.TYPE_AVAILABLE:
            case Presence.TYPE_UNAVAILABLE:
                processPresenceAvailability(presence); //上下线
                break;
            case Presence.TYPE_ACCEPT:
            case Presence.TYPE_REJECT:
                processPresenceResponse(presence); //加好友，对方是否同意的反馈
                break;
            case Presence.TYPE_SUBSCRIBE:
                processPresenceSubscription(presence); //收到好友请求
                break;
            case Presence.TYPE_UNSUBSCRIBE:
                processPresenceUnSubscription(presence); //收到被删除好友的通知
                break;
        }
    }
    private void processPresenceSubscription(Presence presence) {
        Platform.runLater(()->{
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    presence.getFrom() + " 正请求与你成为好友，是否同意？",
                    ButtonType.YES, ButtonType.NO);

            Optional<ButtonType> responce = alert.showAndWait();

            String type;
            if (responce.isPresent() && responce.get().equals(ButtonType.YES)) {
                type = Presence.TYPE_ACCEPT;

                if (App.UserList != null) {
                    User user = presence.getFrom();
                    user.setOnline(true);
                    App.UserList.add(user);
                }
            } else {
                type = Presence.TYPE_REJECT;
            }
            Presence result = new Presence(App.CurrentUser, presence.getFrom(), type);
            synchronized (ElementSendQueue) {
                ElementSendQueue.add(result);
                ElementSendQueue.notify();
            }
        });
    }
    private void processPresenceUnSubscription(Presence presence) {
        Platform.runLater(()->{
            if (App.UserList != null) {
                int len = App.UserList.size();
                for (int i=0; i<len; i++) {
                    User user = App.UserList.get(i);
                    if (user.equals(presence.getFrom())) {
                        App.UserList.remove(i);
                    }
                }
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    presence.getFrom() + " 已将你从好友列表中删除。");
            alert.show();
        });
    }
    private void processPresenceAvailability(Presence presence) {
        Platform.runLater(()->{
            if (App.UserList != null) {
                int len = App.UserList.size();
                for (int i=0; i<len; i++) {
                    User user = App.UserList.get(i);
                    if (user.equals(presence.getFrom())) {
                        User newUser = new User(user.getName());
                        newUser.setOnline(presence.getType().equals(Presence.TYPE_AVAILABLE));
                        App.UserList.set(i, newUser);
                    }
                }
            }
        });
    }
    private void processPresenceResponse(Presence presence) {
        Platform.runLater(()->{
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    presence.getFrom() + " 已将你添加为好友。");
            alert.show();

            if (presence.getType().equals(Presence.TYPE_ACCEPT)) {
                if (App.UserList != null) {
                    User user = presence.getFrom();
                    user.setOnline(true);
                    App.UserList.add(user);
                }
            }
        });
    }

    //接收message，然后选取相应的聊天窗口，把它显示出来
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

    //处理result
    private void processResult(Result result){
        //查询当前request队列中是否有和本result对应的
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
            //错误统一弹窗
            Platform.runLater(()-> {
                Alert alert = new Alert(Alert.AlertType.ERROR, result.getBody(), ButtonType.CLOSE);
                alert.showAndWait();
            });
        } else {
            //其它消息分类处理
            switch (result.getBody()) {
                case Result.BODY_LOGIN_SUCCESS: //服务器发回登录成功的信息 进入主界面
                    processLoginResult(result);
                    break;
                case Result.BODY_FILE_ACCEPT: //服务器发回接受文件的信息，允许sendTask把文件发过去
                    System.out.println("server should be ready");
                    synchronized (App.SendTask.FileTransferStatus) {
                        if (App.SendTask.FileTransferStatus[0] == SendTask.FILE_WAITING) {
                            App.SendTask.FileTransferStatus[0] = SendTask.FILE_ACCEPTED;
                            App.SendTask.FileTransferStatus.notify();
                        }
                    }
                    break;
                case Result.BODY_FILE_REJECT: //服务器发回拒绝文件的信息，把传文件操作停掉
                    synchronized (App.SendTask.FileTransferStatus) {
                        if (App.SendTask.FileTransferStatus[0] == SendTask.FILE_WAITING) {
                            App.SendTask.FileTransferStatus[0] = SendTask.FILE_REJECTED;
                            App.SendTask.FileTransferStatus.notify();
                        }
                    }
                    break;
            }
        }
    }

    //处理好友列表操作的返回值 这里处理类似result
    private void processBuddyListOperations(BuddyListOperations operations) {
        //查询当前request队列中是否有和本result对应的
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

        switch (operations.getOperation()) {
            case BuddyListOperations.OPERATION_NS_QUERY:
                //QUERY 返回好友列表
                Platform.runLater(()->{
                    if (App.UserList!=null) {
                        App.UserList.clear();
                        App.UserList.setAll(operations.getItems());
                    }
                });
                break;
            case BuddyListOperations.OPERATION_NS_SEARCH:
                //SEARCH 返回符合条件的用户
                //要是添加好友窗口已经关掉了，那就没意义了
                if (App.WindowControllers.
                        containsKey(App.CONTROLLER_ADD_FRIEND_WINDOW)) {

                    AddFriendWindowController controller = (AddFriendWindowController)
                            App.WindowControllers.get(App.CONTROLLER_ADD_FRIEND_WINDOW);

                    Platform.runLater(()->{
                        controller.getUserList().clear();
                        controller.getUserList().setAll(operations.getItems());
                    });
                }
                break;
        }
    }

    //接收服务器转发的文件
    private void processFileMessage(FileMessage message) {

        final File[] file = new File[1];
        final ChatWindowController[] windowController = new ChatWindowController[1];

        if (message.getFrom() == null) return;
        String from = message.getFrom().getName();
        System.out.println("receive fileImage from" + from);

        Platform.runLater( () -> {
            //这个锁是为了让UI线程与ConnectionTask同步...要是java有await/async那该多好...
            synchronized (file) {
                //获取发送者对应的聊天窗口，并把它拉到前台
                ChatWindowController controller;
                if (App.WindowControllers.containsKey("USER_" + from)) {
                    controller = (ChatWindowController)App.WindowControllers.get("USER_" + from);
                    controller.getStage().show();
                    controller.getStage().requestFocus();
                } else {
                    controller = ChatWindowController.newChatWindow(getClass(), message.getFrom());
                    if (controller == null) return;
                }

                //询问用户是否接收文件
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION ,
                        "准备接收文件:\n" +
                                message.getFilename() +
                                "\n文件大小 " + Util.getHumanReadableFileLength(message.getSize()) +
                                "\n真的要继续吗？",
                        ButtonType.YES, ButtonType.NO);
                Optional<ButtonType> result = alert.showAndWait();

                //接受，则弹出保存文件对话框
                if (result.isPresent() && result.get().equals(ButtonType.YES)) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialFileName(message.getFilename());
                    file[0] = fileChooser.showSaveDialog(controller.getStage());
                }

                file.notify();

                windowController[0] = controller; //让runLater外面能获取到这个controller
            }
        });

        //这个锁是为了让UI线程与ConnectionTask同步...要是java有await/async那该多好...
        synchronized (file) {

            //file本来是空的，等UI线程选择好文件之后file就有东西了
            if (file[0] == null) {
                try {

                    file.wait();
                } catch (InterruptedException ex) {
                    return;
                }
            }

            System.out.println(file[0].getPath());

            //如果file对象有效 发送ACCEPT result给服务器，然后开始接收文件
            if (file[0] != null) {
                Result result = new Result("", message.getId(), Result.TYPE_INFORMATION, Result.BODY_FILE_ACCEPT);
                synchronized (ElementSendQueue) {
                    ElementSendQueue.add(result);
                    ElementSendQueue.notify();
                }
                receiveFile(file[0], message, App.Socket);

                //给对方发一个文件传输成功的消息
                Platform.runLater(()-> {
                    Message successMessage = new Message(App.CurrentUser, message.getFrom(), "文件传输成功");
                    windowController[0].appendMessage(successMessage);
                    synchronized (ElementSendQueue) {
                        ElementSendQueue.add(successMessage);
                        ElementSendQueue.notify();
                    }
                });
            }
        }

    }

    //接收文件
    private void receiveFile(File file, FileMessage message, Socket socket) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)){

            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
            socket.setSoTimeout(4000); //临时设置一个超时

            long fileSize = message.getSize();
            long receivedLength = 0;
            int length;
            byte[] bytes = new byte[1048576];

            while (receivedLength < fileSize) {
                System.out.println(String.valueOf(fileSize) + " " + String.valueOf(receivedLength));
                length = inputStream.read(bytes, 0, bytes.length);
                if (length == -1) break;
                fileOutputStream.write(bytes, 0, length);
                receivedLength += (long)length;
            }
            System.out.println("file received.");

            socket.setSoTimeout(0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //登陆成功之后执行的操作
    private void processLoginResult(Result result) {
        App.CurrentUser = new User(result.getTo());

        Presence presence = new Presence(App.CurrentUser, null, Presence.TYPE_AVAILABLE);

        synchronized (App.ElementSendQueue) {
            App.ElementSendQueue.add(presence);
            App.ElementSendQueue.notify();
        }

        Platform.runLater( () -> {
            if (App.WindowControllers.containsKey(App.CONTROLLER_LOGIN_WINDOW)) {
                WindowController controller = App.WindowControllers.get(App.CONTROLLER_LOGIN_WINDOW);
                App.WindowControllers.remove(App.CONTROLLER_LOGIN_WINDOW);
                controller.getStage().close();
            }

            UserListWindowController.newUserListWindow(getClass());
        } );
    }
}
