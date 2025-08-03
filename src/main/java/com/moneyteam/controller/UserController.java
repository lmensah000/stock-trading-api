package com.moneyteam.controller;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Retrieve the username and password from the loginRequest
        // Call the authenticateUser method in the userService to authenticate the user
        // Return the authentication result to the client
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest registrationRequest) {
        // Retrieve user details from the registrationRequest
        // Call the registerUser method in the userService to register the user
        // Return the registration result to the client
    }

    // Other controller methods for managing user-specific data
}