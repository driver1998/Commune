package com.commune.server;

import com.commune.utils.Util;
import com.commune.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserManagement {

    //注册
    public static void signUp(String username, String hash1)
            throws User.UserNameConflictException {

        Connection databaseConnection = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;

        try {
            databaseConnection = Program.connectDatabase();

            //查询是否有重名用户
            boolean isConflict = false;
            String sql = "SELECT uid from Users WHERE username = ?";
            statement1 = databaseConnection.prepareStatement(sql);
            statement1.setString(1, username);
            ResultSet resultSet = statement1.executeQuery();

            //用户名有冲突
            if (resultSet.next()) throw new User.UserNameConflictException("用户名冲突。");

            //计算salt和hash2
            String salt = Util.generateRandomString(10);
            String hash2 = Util.getSHA1(hash1 + username + salt);

            //写到数据库中
            sql = "INSERT INTO Users(username, hash2, salt) VALUES(?,?,?)";
            statement2 = databaseConnection.prepareStatement(sql);
            statement2.setString(1, username);
            statement2.setString(2, hash2);
            statement2.setString(3, salt);
            statement2.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (statement1 != null) statement1.close();
                if (statement2 != null) statement2.close();
                if (databaseConnection != null) databaseConnection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    //登录
    public static User login(String username, String hash1)
            throws User.WrongUsernameOrPasswordException, SQLException{
        Connection databaseConnection = null;
        PreparedStatement statement = null;

        try {
            databaseConnection = Program.connectDatabase();
            String sql = "SELECT uid, salt, hash2 FROM Users WHERE username = ?";
            statement = databaseConnection.prepareStatement(sql);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            //用户不存在
            if (!resultSet.next()) {
                statement.close();
                databaseConnection.close();
                throw new User.WrongUsernameOrPasswordException("用户名或密码错误.");
            }

            //从数据库读取uid salt hash2
            String salt = resultSet.getString ("salt");
            String hash2 = resultSet.getString ("hash2");

            //计算新的hash2
            String generatedHash2 = Util.getSHA1(hash1 + username + salt);

            //hash2不匹配 登录失败
            if (!hash2.equals(generatedHash2)) throw new User.WrongUsernameOrPasswordException("用户名或密码错误.");

            return new User(username);

        } finally {
            try {
                if (statement != null) statement.close();
                if (databaseConnection != null) databaseConnection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static List<User> getBuddyList(User user) throws SQLException, User.WrongUsernameOrPasswordException{
        try (Connection connection = Program.connectDatabase()) {
            String sql = "SELECT buddyList FROM Users WHERE username=?";
            PreparedStatement statement1 = connection.prepareStatement(sql);
            statement1.setString(1, user.getName());
            ResultSet resultSet = statement1.executeQuery();

            if (!resultSet.next()) throw new User.WrongUsernameOrPasswordException("用户名无效");

            String buddyListString = resultSet.getString("buddyList");

            sql = "SELECT username FROM Users WHERE uid IN (@buddyList)"
                    .replace("@buddyList", buddyListString);
            PreparedStatement statement2 = connection.prepareStatement(sql);
            resultSet = statement2.executeQuery();

            List<User> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(new User(resultSet.getString("username")));
            }

            return list;
        }
    }
}
