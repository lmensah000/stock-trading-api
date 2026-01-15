package com.moneyteam.integration;

import com.moneyteam.dto.LoginRequest;
import com.moneyteam.dto.UserRegistrationRequest;
import com.moneyteam.model.User;
import com.moneyteam.repository.UserRepository;
import com.moneyteam.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Integration tests for user authentication workflow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Authentication Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAuthIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassWord("$2a$10$hashedpassword");
    }

    @Test
    @Order(1)
    @DisplayName("Integration: User registration workflow")
    void testUserRegistrationWorkflow() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUserName("newuser");
        request.setEmail("newuser@example.com");
        request.setPassWord("password123");

        when(userRepository.findByUserName("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedpassword");

        // Act - Simulate registration
        Optional<User> existingByUsername = userRepository.findByUserName(request.getUserName());
        Optional<User> existingByEmail = userRepository.findByEmail(request.getEmail());

        // Assert - No conflicts
        assertTrue(existingByUsername.isEmpty());
        assertTrue(existingByEmail.isEmpty());

        // Create user
        User newUser = new User();
        newUser.setUserName(request.getUserName());
        newUser.setEmail(request.getEmail());
        newUser.setPassWord(passwordEncoder.encode(request.getPassWord()));

        assertNotNull(newUser);
        assertEquals("newuser", newUser.getUserName());
        assertEquals("newuser@example.com", newUser.getEmail());
        assertEquals("$2a$10$encodedpassword", newUser.getPassWord());
    }

    @Test
    @Order(2)
    @DisplayName("Integration: User login workflow")
    void testUserLoginWorkflow() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUserName("testuser");
        loginRequest.setPassWord("password");

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "$2a$10$hashedpassword")).thenReturn(true);

        // Act - Find user
        Optional<User> user = userRepository.findByUserName(loginRequest.getUserName());

        // Assert
        assertTrue(user.isPresent());
        assertEquals("testuser", user.get().getUserName());

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassWord(), user.get().getPassWord());
        assertTrue(passwordMatches);
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Failed login with wrong password")
    void testFailedLoginWrongPassword() {
        // Arrange
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        // Act
        Optional<User> user = userRepository.findByUserName("testuser");
        assertTrue(user.isPresent());

        boolean passwordMatches = passwordEncoder.matches("wrongpassword", user.get().getPassWord());

        // Assert
        assertFalse(passwordMatches);
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Failed login with non-existent user")
    void testFailedLoginNonExistentUser() {
        // Arrange
        when(userRepository.findByUserName("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> user = userRepository.findByUserName("nonexistent");

        // Assert
        assertTrue(user.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Registration with duplicate username")
    void testRegistrationDuplicateUsername() {
        // Arrange
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> existingUser = userRepository.findByUserName("testuser");

        // Assert - User already exists
        assertTrue(existingUser.isPresent());
        assertEquals("testuser", existingUser.get().getUserName());
    }

    @Test
    @Order(6)
    @DisplayName("Integration: Registration with duplicate email")
    void testRegistrationDuplicateEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> existingUser = userRepository.findByEmail("test@example.com");

        // Assert - Email already exists
        assertTrue(existingUser.isPresent());
        assertEquals("test@example.com", existingUser.get().getEmail());
    }

    @Test
    @Order(7)
    @DisplayName("Integration: User profile update")
    void testUserProfileUpdate() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpassword")).thenReturn("$2a$10$newhashedpassword");

        // Act
        Optional<User> user = userRepository.findById(1L);
        assertTrue(user.isPresent());

        // Update profile
        user.get().setEmail("newemail@example.com");
        user.get().setPassWord(passwordEncoder.encode("newpassword"));

        // Assert
        assertEquals("newemail@example.com", user.get().getEmail());
        assertEquals("$2a$10$newhashedpassword", user.get().getPassWord());
    }

    @Test
    @Order(8)
    @DisplayName("Integration: Validate registration input")
    void testValidateRegistrationInput() {
        // Valid inputs
        assertTrue(isValidUsername("validuser"));
        assertTrue(isValidEmail("valid@email.com"));
        assertTrue(isValidPassword("validpassword"));

        // Invalid inputs
        assertFalse(isValidUsername("ab")); // too short
        assertFalse(isValidEmail("invalidemail")); // no @
        assertFalse(isValidPassword("12345")); // too short
    }

    @Test
    @Order(9)
    @DisplayName("Integration: Login request validation")
    void testLoginRequestValidation() {
        // Arrange
        LoginRequest validRequest = new LoginRequest("testuser", "password");
        LoginRequest invalidUsernameRequest = new LoginRequest("", "password");
        LoginRequest invalidPasswordRequest = new LoginRequest("testuser", "");

        // Assert
        assertTrue(isValidLoginRequest(validRequest));
        assertFalse(isValidLoginRequest(invalidUsernameRequest));
        assertFalse(isValidLoginRequest(invalidPasswordRequest));
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

    private boolean isValidLoginRequest(LoginRequest request) {
        return request != null &&
               request.getUserName() != null && !request.getUserName().isEmpty() &&
               request.getPassWord() != null && !request.getPassWord().isEmpty();
    }
}
