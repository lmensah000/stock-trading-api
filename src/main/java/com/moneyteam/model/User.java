package com.moneyteam.model;

public class User {
    private static String userName;
    private static String passWord;
    private static String login;
    private static String placeOrder;
    private static String buy;
    private static String sell;
    private static String email;

    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String userName) {
        User.userName = userName;
    }

    public static String getPassword() {
        return passWord;
    }

    public static void setPassWord(String passWord) {
        User.passWord = passWord;
    }

    public static String placeOrder() {
        return placeOrder;
    }

    public static void placeOrder(String placeOrder) {
        User.placeOrder = placeOrder;
    }

    public static String getBuy() {
        return buy;
    }

    public static void setBuy(String buy) {
        User.buy = buy;
    }

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String email) {
        User.email = email;
    }

    public String getLogin(String email, String passWord){
        return login;
    }

    public static void login(String email, String passWord) {
        User.email = email;
        User.passWord = passWord;
    }

    public static String getSell() {
        return sell;
    }

    public static void setSell(String sell) {
        User.sell = sell;
    }

    public User(String userName, String passWord, String login, String placeHolder, String buy, String sell, String email) {
        this.userName = userName;
        this.passWord = passWord;
        this.login = login;
        this.placeOrder =placeHolder;
        this.buy =buy;
        this.sell = sell;
        this.email= email;
    }

    @Override
    public String toString() {
        return "User{" +
                "userName" + userName +
                "passWord" + passWord +
                "buy" + buy +
                "sell" + sell +
                "email" + email +
                "}";
    }
}
