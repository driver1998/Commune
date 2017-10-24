package com.commune.server;
import com.commune.stream.*;
import com.commune.model.User;

import java.io.*;
import java.net.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public class Program {
    volatile static HashMap<String, ClientConnection> Clients;

    //获取数据库连接
    public static java.sql.Connection connectDatabase() throws SQLException{
        String url = "jdbc:mysql://localhost:3306/COMMUNE?characterEncoding=utf8&useSSL=true";
        String user = "root";
        String password = "p@ssw0rd";

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return DriverManager.getConnection(url, user, password);
    }


    public static void main(String[] args) throws IOException {
        System.out.println("Project Commune Server");
        System.out.println("Build 20171017");
        System.out.println();


        listen();
    }

    private static void listen() throws IOException{
        int port = 4074;
        ServerSocket server = new ServerSocket(port);

        Clients = new HashMap<>();

        System.out.println("Listening on Port " + Integer.toString(port));

        while (true) {
            final Socket client = server.accept();

            new Thread(() -> {
                try {
                    processMessages(client);
                } catch (InvalidElementException im) {
                    System.out.println(im.getMessage());
                }
            }).start();
        }
    }

    private static void processMessages(Socket socket) throws InvalidElementException {
        try {

            System.out.println("Program.processMessages");
            DataElement element = DataElement.getElement(socket);

            if (element instanceof Auth) {
                callLogin(socket, (Auth)element);
            } else if (element instanceof Register) {
                callSignUp(socket, (Register)element);
            }

        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                ex.printStackTrace();
            }
        }
    }

    private static void callLogin(Socket socket, Auth element) throws SQLException{
        String username = element.getUsername();
        String hash1 = element.getHash1();
        String id = element.getId();

        try {
            System.out.println("Program.callLogin");
            User user = UserManagement.login(username, hash1);

            Result result = new Result(username, id, Result.TYPE_INFORMATION, Result.BODY_LOGIN_SUCCESS);
            result.send(socket);

            try (ClientConnection connection = ClientConnection.newInstance(socket, user);){
                connection.listen();
            } catch (ClientConnection.ClientDisconnectException ex) {
                System.out.println(ex.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } catch (User.WrongUsernameOrPasswordException ex) {
             Result result = new Result(username, id, Result.TYPE_ERROR, Result.BODY_LOGIN_WRONG_INFO);
             result.send(socket);
        }

    }


    private static void callSignUp(Socket socket, Register element)
            throws IOException{

    }


}
