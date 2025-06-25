package com.lds.demo;

public class Main {

    public static void main(String[] args) {
        User user = new User();
        user.setName("name");
        user.setId(1234);
        System.out.println("user: id=" + user.getId() + ", name=" + user.getName());
    }
}
