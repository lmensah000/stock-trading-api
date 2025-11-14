package com.moneyteam.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class UserRegistrationRequest {
    private String userName;
    private String passWord;
    private String email;

//    @Override
//    User user;

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassWord() { return passWord; }
    public void setPassWord(String password) { this.passWord = passWord; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
