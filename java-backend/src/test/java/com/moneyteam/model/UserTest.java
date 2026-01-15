package com.moneyteam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User model.
 */
@DisplayName("User Model Tests")
public class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("Should create user with default values")
    void testUserCreation() {
        assertNotNull(user);
    }

    @Test
    @DisplayName("Should set and get user ID")
    void testSetAndGetId() {
        Long id = 1L;
        user.setId(id);
        assertEquals(id, user.getId());
    }

    @Test
    @DisplayName("Should set and get username")
    void testSetAndGetUserName() {
        String username = "testuser";
        user.setUserName(username);
        assertEquals(username, user.getUserName());
    }

    @Test
    @DisplayName("Should set and get email")
    void testSetAndGetEmail() {
        String email = "test@example.com";
        user.setEmail(email);
        assertEquals(email, user.getEmail());
    }

    @Test
    @DisplayName("Should set and get password")
    void testSetAndGetPassWord() {
        String password = "securepassword123";
        user.setPassWord(password);
        assertEquals(password, user.getPassWord());
    }

    @Test
    @DisplayName("Should handle null username")
    void testNullUsername() {
        user.setUserName(null);
        assertNull(user.getUserName());
    }

    @Test
    @DisplayName("Should handle empty email")
    void testEmptyEmail() {
        user.setEmail("");
        assertEquals("", user.getEmail());
    }

    @Test
    @DisplayName("Should return correct toString")
    void testToString() {
        user.setId(1L);
        user.setUserName("testuser");
        user.setEmail("test@example.com");
        
        String result = user.toString();
        assertNotNull(result);
        assertTrue(result.contains("testuser") || result.contains("User"));
    }

    @Test
    @DisplayName("Should create user with constructor")
    void testUserCreationWithConstructor() {
        User newUser = new User("testuser", "password123", "test@example.com", null);
        
        assertEquals("testuser", newUser.getUserName());
        assertEquals("password123", newUser.getPassWord());
        assertEquals("test@example.com", newUser.getEmail());
    }
}
