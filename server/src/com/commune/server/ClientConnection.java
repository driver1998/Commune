package com.commune.server;

import com.commune.stream.*;
import com.commune.model.User;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ClientConnection implements AutoCloseable{
    private Socket socket;
    private User user;

    private ClientSendThread sendThread;
    final ArrayList<DataElement> ElementSendQueue = new ArrayList<>();

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

    private void processElements() throws ClientDisconnectException{
        System.out.println(user.getName() + " ClientConnection.processMessage");
        try {
            DataElement element = DataElement.getElement(socket);

            if (element instanceof BuddyListOperations) {
                processBuddyListOperations((BuddyListOperations)element);
            } else if (element instanceof Message) {
                processMessage((Message)element);
            } else if (element instanceof FileMessage) {
                receiveFile((FileMessage)element, socket);
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


    private void receiveFile(FileMessage message, Socket socket) {
        File stashFolder = new File("stash");
        if (!stashFolder.exists()) stashFolder.mkdir();

        File tmpFile = new File("stash/" + UUID.randomUUID().toString());

        try (FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);){

            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());

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

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

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

    class ClientSendThread extends Thread {
        volatile boolean stop = false;

        @Override
        public void run() {
            while (!stop) {
                sendElements();
            }
        }

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
                    i.remove();
                }
            }
        }
    }
}
