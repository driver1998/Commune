package com.commune.server;

import com.commune.stream.*;
import com.commune.model.User;
import com.sun.org.apache.regexp.internal.RE;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

//与客户端交互的类
public class ClientConnection implements AutoCloseable{
    private Socket socket;
    private User user;

    //子线程 控制数据发送
    private ClientSendThread sendThread;

    //准备从服务器发出的element，sendThread将会将它们发出去
    private final ArrayList<DataElement> ElementSendQueue = new ArrayList<>();

    //从服务器发出的，当前未获得处理的request列表
    private final ArrayList<String> RequestIDs = new ArrayList<>();

    private ClientConnection(Socket socket, User user) {
        this.socket = socket;
        this.user = user;
    }

    static class ClientDisconnectException extends Exception {
        ClientDisconnectException(String msg) { super(msg); }
    }

    static ClientConnection newInstance(Socket socket, User user) throws IOException{
        ClientConnection connection = new ClientConnection(socket, user);

        Program.Clients.put(user.getName(), connection);
        return connection;
    }

    void listen() throws ClientDisconnectException{
        sendThread = new ClientSendThread();
        sendThread.start();

        while (true) {
            processElements();
        }
    }

    //接收element并按类型分别处理
    private void processElements() throws ClientDisconnectException{
        System.out.println(user.getName() + " ClientConnection.processMessage");
        try {
            DataElement element = DataElement.getElement(socket);

            if (element instanceof BuddyListOperations) {
                processBuddyListOperations((BuddyListOperations)element);
            } else if (element instanceof Message) {
                processMessage((Message)element);
            } else if (element instanceof FileMessage) {
                processFileMessage((FileMessage)element);
            } else if (element instanceof Result) {
                processResult((Result)element);
            }
        } catch (InvalidElementException e) {
            Result result = new Result("", "", Result.TYPE_ERROR, Result.BODY_INVALID_ELEMENT);

            synchronized (ElementSendQueue) {
                ElementSendQueue.add(result);
                ElementSendQueue.notify();
            }
        } catch (IOException iex) {
            throw new ClientDisconnectException(user.getName() + " disconnected");
        }

    }

    //接受文件发送请求，接收文件并暂存，然后把它发到目的地
    private void processFileMessage(FileMessage message) {
        //如果没有收件人，打回去
        if (message.getTo() == null) {
            Result result = new Result("", "", Result.TYPE_ERROR, Result.BODY_INVALID_ELEMENT);

            synchronized (ElementSendQueue) {
                ElementSendQueue.add(result);
                ElementSendQueue.notify();
            }
        }

        //如果收件人不在线，打回去
        String to = message.getTo().getName();
        if(!Program.Clients.containsKey(to)) return;

        //发一个ACCEPT给发件人，并接收文件
        Result result = new Result(message.getFrom().getName(), message.getId(), Result.TYPE_INFORMATION, Result.BODY_FILE_ACCEPT);
        synchronized (ElementSendQueue) {
            ElementSendQueue.add(result);
            ElementSendQueue.notify();
        }
        File file = receiveFile(message, socket);

        if (file.exists()) {
            //发文件给收件人
            FileMessage newMessage = new FileMessage(message.getFrom(), message.getTo(), file, message.getFilename(), message.getSize(), message.getId());

            if (Program.Clients.containsKey(to)) {
                ClientConnection toConnection = Program.Clients.get(to);

                synchronized (toConnection.RequestIDs) {
                    toConnection.RequestIDs.add(message.getId());
                    toConnection.RequestIDs.notify();
                }

                synchronized (toConnection.ElementSendQueue) {
                    toConnection.ElementSendQueue.add(newMessage);
                    toConnection.ElementSendQueue.notify();
                }
            }
        }

    }
    private File receiveFile(FileMessage message, Socket socket) {
        File stashFolder = new File("stash");
        if (!stashFolder.exists()) stashFolder.mkdir();

        File tmpFile = new File("stash/" + UUID.randomUUID().toString());

        try (FileOutputStream fileOutputStream = new FileOutputStream(tmpFile)){

            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
            //socket.setSoTimeout(4000);

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

            //ddrisocket.setSoTimeout(0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return tmpFile;
    }

    //暂时只用于接收文件传输相关的result
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

        if (result.getType().equals(Result.TYPE_INFORMATION)) {
            switch (result.getBody()) {
                case Result.BODY_FILE_ACCEPT:
                    System.out.println(this.user.getName() + " should be ready");
                    synchronized (sendThread.FileTransferStatus) {
                        if (sendThread.FileTransferStatus[0] == ClientSendThread.FILE_WAITING) {
                            sendThread.FileTransferStatus[0] = ClientSendThread.FILE_ACCEPTED;
                            sendThread.FileTransferStatus.notify();
                        }
                    }
                    break;
                case Result.BODY_FILE_REJECT:
                    synchronized (sendThread.FileTransferStatus) {
                        if (sendThread.FileTransferStatus[0] == ClientSendThread.FILE_WAITING) {
                            sendThread.FileTransferStatus[0] = ClientSendThread.FILE_REJECTED;
                            sendThread.FileTransferStatus.notify();
                        }
                    }
                    break;
            }
        }
    }

    //转发message
    private void processMessage(Message message) {
        if (message.getTo() == null) {
            Result result = new Result("", "", Result.TYPE_ERROR, Result.BODY_INVALID_ELEMENT);

            synchronized (ElementSendQueue) {
                ElementSendQueue.add(result);
                ElementSendQueue.notify();
            }
        }

        String to = message.getTo().getName();
        if (Program.Clients.containsKey(to)) {
            ClientConnection toConnection = Program.Clients.get(to);

            synchronized (toConnection.ElementSendQueue) {
                toConnection.ElementSendQueue.add(message);
                toConnection.ElementSendQueue.notify();
            }
        }
    }

    //处理好友列表操作指令
    private void processBuddyListOperations(BuddyListOperations operations) {

        if (operations.getFrom() == null) {
            Result result = new Result("", "", Result.TYPE_ERROR, Result.BODY_INVALID_ELEMENT);

            synchronized (ElementSendQueue) {
                ElementSendQueue.add(result);
                ElementSendQueue.notify();
            }
        }

        try {
            List<User> buddyList = UserManagement.getBuddyList(operations.getFrom());
            switch (operations.getOperation()) {
                case BuddyListOperations.OPERATION_NS_QUERY:
                    BuddyListOperations result =
                            new BuddyListOperations(null,
                                    operations.getFrom(),
                                    operations.getId(),
                                    operations.getOperation(),
                                    buddyList);

                    synchronized (ElementSendQueue) {
                        ElementSendQueue.add(result);
                        ElementSendQueue.notify();
                    }
                    break;
                case BuddyListOperations.OPERATION_NS_ADD:
                    break;
                case BuddyListOperations.OPERATION_NS_DELETE:
                    break;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (User.WrongUsernameOrPasswordException ex) {
            Result result = new Result("", "", Result.TYPE_ERROR, Result.BODY_INVALID_USER);

            synchronized (ElementSendQueue) {
                ElementSendQueue.add(result);
                ElementSendQueue.notify();
            }
        }

    }

    @Override
    public void close() throws IOException {
        sendThread.stop = true;
        sendThread.interrupt();

        socket.close();
        Program.Clients.remove(user.getName());
    }

    //ClientConnection的子线程，发送element
    class ClientSendThread extends Thread {
        volatile boolean stop = false;
        static final int FILE_ACCEPTED = 1; //对方接受了文件，马上开始传输
        static final int FILE_REJECTED = -1; //对方不接受文件
        static final int FILE_WAITING = 2; //文件准备发送，等待对方接受
        static final int FILE_DEFAULT = 0; //默认状态，无意义

        //指示文件传输进程
        final Integer[] FileTransferStatus = {FILE_DEFAULT};

        @Override
        public void run() {
            while (!stop) {
                sendElements();
            }
        }

        //将elementSendQueue中的element全部发出去
        private void sendElements() {
            synchronized (ElementSendQueue) {
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
                    e.send(socket);

                    //如果是文件发送请求，阻塞掉发送线程，改为发文件
                    //不是特别好的方法，但起码能用
                    if (e instanceof FileMessage) {
                        sendFile((FileMessage)e, socket);
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
                System.out.println("waiting...");
                try {
                    FileTransferStatus.wait();
                } catch (InterruptedException e) {
                    return;
                }

                System.out.println("started");
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
}
