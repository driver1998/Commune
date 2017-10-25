package com.commune.model;

import java.util.List;

public class User {

    private String name;
    private int uid;
    private boolean online;

    public String getName() {
        return name;
    }

    public int getUid() {
        return uid;
    }

    public User(String name) {
        this.name = name;
        this.uid = uid;
        this.online = false;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof User) && getName().equals(((User)obj).getName());
    }

    public static class UserNameConflictException extends Exception {
        public UserNameConflictException(String msg) { super(msg); }
    }

    public static class WrongUsernameOrPasswordException extends Exception {
        public WrongUsernameOrPasswordException(String msg) { super(msg); }
    }
}
