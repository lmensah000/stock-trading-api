package com.moneyteam.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.sql.Time;
import java.sql.Timestamp;

public class UserRegistrationRequest {
    @NotBlank
    private String userName;
    @NotBlank
    private String passWord;
    @Email
    private String email;
    private Timestamp createdAt;


//    @Override
//    User users;

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassWord() { return passWord; }
    public void setPassWord(String passWord) { this.passWord = passWord; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = Timestamp.valueOf(createdAt);
    }
}
