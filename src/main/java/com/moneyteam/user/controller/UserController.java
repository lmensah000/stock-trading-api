package com.moneyteam.user.controller;

import com.moneyteam.user.model.User;
import com.moneyteam.user.service.UserService;

import com.moneyteam.user.dto.LoginRequest;
import com.moneyteam.user.dto.UserRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    // Authentication failures propagate to GlobalExceptionHandler, which maps them
    // to 401/423 without echoing internal exception text back to the caller.
    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@Valid @RequestBody LoginRequest loginRequest) {
        User authenticatedUser = userService.authenticateUser(
                loginRequest.getUserName(),
                loginRequest.getPassWord());
        log.info("✅ Successful login for users: {}", authenticatedUser.getUserName());
        return ResponseEntity.ok("Login successful for users: " + authenticatedUser.getUserName());
    }
// public String getLogin(String email, String passWord){
//        return login;
//    }
//
//    public void login(String email, String passWord) {
//        this.email = email;
//        this.passWord = passWord;
//    }
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        log.info("User registration attempt for username: {}", registrationRequest.getUserName());

        User newUser = new User();
        newUser.setUserName(registrationRequest.getUserName());
        newUser.setPassWord(registrationRequest.getPassWord());
        newUser.setEmail(registrationRequest.getEmail());

        userService.registerUser(newUser);
        log.info("✅ Successfully registered new users: {}", newUser.getUserName());
        return ResponseEntity.ok("User successfully registered.");
    }

//    public ResponseEntity<?> updateUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
//
//        try {
//        }
//
//        } catch (Exception e) {}
//    }

    // Other controller methods for managing users-specific data
}