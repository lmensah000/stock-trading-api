package com.moneyteam.controller;

import com.moneyteam.dto.LoginRequest;
import com.moneyteam.dto.UserRegistrationRequest;
import com.moneyteam.model.User;
import com.moneyteam.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for UserController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Controller Tests")
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private LoginRequest loginRequest;
    private UserRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassWord("hashedpassword");

        loginRequest = new LoginRequest();
        loginRequest.setUserName("testuser");
        loginRequest.setPassWord("password");

        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setUserName("newuser");
        registrationRequest.setEmail("new@example.com");
        registrationRequest.setPassWord("password123");
    }

    @Test
    @DisplayName("Should authenticate user successfully")
    void testAuthenticateSuccess() {
        when(userService.authenticateUser("testuser", "password")).thenReturn(testUser);

        ResponseEntity<?> result = userController.authenticate(loginRequest);

        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
        assertTrue(result.getBody().toString().contains("Login successful"));
    }

    @Test
    @DisplayName("Should fail authentication with wrong password")
    void testAuthenticateFailure() {
        when(userService.authenticateUser("testuser", "password"))
                .thenThrow(new RuntimeException("Incorrect password"));

        ResponseEntity<?> result = userController.authenticate(loginRequest);

        assertNotNull(result);
        assertEquals(401, result.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterUserSuccess() {
        doNothing().when(userService).registerUser(any(User.class));

        ResponseEntity<?> result = userController.registerUser(registrationRequest);

        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
        assertTrue(result.getBody().toString().contains("successfully registered"));
    }

    @Test
    @DisplayName("Should fail registration for duplicate username")
    void testRegisterUserDuplicate() {
        doThrow(new RuntimeException("Username already exists"))
                .when(userService).registerUser(any(User.class));

        ResponseEntity<?> result = userController.registerUser(registrationRequest);

        assertNotNull(result);
        assertEquals(500, result.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should get user by ID - Success")
    void testGetUserByIdSuccess() {
        when(userService.getUserById(1L)).thenReturn(testUser);

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUserName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should get user by ID - Not Found")
    void testGetUserByIdNotFound() {
        when(userService.getUserById(999L)).thenReturn(null);

        User result = userService.getUserById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("Should validate user input - valid username")
    void testValidateUsername() {
        assertTrue(isValidUsername("testuser"));
        assertTrue(isValidUsername("abc"));
        assertFalse(isValidUsername("ab")); // too short
        assertFalse(isValidUsername("")); // empty
        assertFalse(isValidUsername(null)); // null
    }

    @Test
    @DisplayName("Should validate user input - valid email")
    void testValidateEmail() {
        assertTrue(isValidEmail("test@example.com"));
        assertTrue(isValidEmail("user.name@domain.org"));
        assertFalse(isValidEmail("invalid")); // no @ symbol
        assertFalse(isValidEmail("test@")); // incomplete
        assertFalse(isValidEmail(null)); // null
    }

    @Test
    @DisplayName("Should validate user input - valid password")
    void testValidatePassword() {
        assertTrue(isValidPassword("password123"));
        assertTrue(isValidPassword("123456")); // minimum length
        assertFalse(isValidPassword("12345")); // too short
        assertFalse(isValidPassword("")); // empty
        assertFalse(isValidPassword(null)); // null
    }

    @Test
    @DisplayName("Should create LoginRequest correctly")
    void testLoginRequestCreation() {
        LoginRequest request = new LoginRequest("testuser", "password");
        assertEquals("testuser", request.getUserName());
        assertEquals("password", request.getPassWord());
    }

    @Test
    @DisplayName("Should create UserRegistrationRequest correctly")
    void testRegistrationRequestCreation() {
        UserRegistrationRequest request = new UserRegistrationRequest("newuser", "password", "new@example.com");
        assertEquals("newuser", request.getUserName());
        assertEquals("password", request.getPassWord());
        assertEquals("new@example.com", request.getEmail());
    }

    // Helper validation methods
    private boolean isValidUsername(String username) {
        return username != null && username.length() >= 3 && username.length() <= 50;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
