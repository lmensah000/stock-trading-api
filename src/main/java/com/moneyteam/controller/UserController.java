package com.moneyteam.controller;

import com.moneyteam.model.User;
import com.moneyteam.service.UserService;

import com.moneyteam.dto.LoginRequest;
import com.moneyteam.dto.UserRegistrationRequest;
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

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest loginRequest) {
        // Retrieve the username and password from the loginRequest
        try {
            User authenticatedUser = userService.authenticateUser(
                    loginRequest.getUserName(),
                    loginRequest.getPassWord()
                    );
            log.info("✅ Successful login for users: {}", authenticatedUser.getUserName());
            return ResponseEntity.ok("Login successful for users: " + authenticatedUser.getUserName());
        } catch (Exception e) {
            log.warn("❌ Login failed for username: {} - {}", loginRequest.getUserName(), e.getMessage());
            return ResponseEntity.status(401).body("Authentication failed: " + e.getMessage());
        }
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

        try {
                User newUser = new User();
                newUser.setUserName(registrationRequest.getUserName());
                newUser.setPassWord(registrationRequest.getPassWord());
                newUser.setEmail(registrationRequest.getEmail());
                //newUser.setCreatedAt(registrationRequest.getCreatedAt());

                userService.registerUser(newUser);
                log.info("✅ Successfully registered new users: {}", newUser.getUserName());
                return ResponseEntity.ok("User successfully registered.");// Retrieve users details from the registrationRequest
            } catch (Exception e) {
                log.error("❌ Registration failed for username: {} - {}", registrationRequest.getUserName(), e.getMessage());
                return ResponseEntity.status(500).body("Registration failed: " + e.getMessage());
            }
        // Call the registerUser method in the userService to register the users
        // Return the registration result to the client
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