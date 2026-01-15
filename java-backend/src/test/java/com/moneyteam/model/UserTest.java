package com.moneyteam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

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
    void testSetAndGetUsername() {
        String username = "testuser";
        user.setUsername(username);
        assertEquals(username, user.getUsername());
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
    void testSetAndGetPassword() {
        String password = "securepassword123";
        user.setPassword(password);
        assertEquals(password, user.getPassword());
    }

    @Test
    @DisplayName("Should set and get created timestamp")
    void testSetAndGetCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle null username")
    void testNullUsername() {
        user.setUsername(null);
        assertNull(user.getUsername());
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
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        String result = user.toString();
        assertNotNull(result);
        assertTrue(result.contains("testuser") || result.contains("User"));
    }
}
