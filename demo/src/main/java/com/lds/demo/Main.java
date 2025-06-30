package com.lds.demo;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final User u1 = new User();
    private static final List<User> u2 = new ArrayList<>();
    private static final User[] u3 = new User[3];

    public static void main(String[] args) {
        User user = new User();
        user.setName("name");
        user.setId(1234);
        dumpUser(user);

        User[] users = new User[3];
        users[0] = new User(1, "user1");
        users[1] = new User(2, "user2");
        users[2] = new User(3, "user3");
        dumpUsers(users);

        int a = 1;
        int b = 2;
        int c = add(a, b);
    }

    private static void dumpUser(User user) {
        System.out.println("user: id=" + user.getId() + ", name=" + user.getName());
    }

    private static void dumpUsers(User[] users) {
        System.out.println("users: " + users.length);
        for (User u : users) {
            System.out.println("user: id=" + u.getId() + ", name=" + u.getName());
        }
    }

    public static int add(int a, int b) {
        int c = a + b;
        return c;
    }
}
