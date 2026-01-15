package com.moneyteam.service;

import com.moneyteam.model.User;
import com.moneyteam.repository.UserRepository;
import com.moneyteam.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassWord("hashedpassword");
    }

    @Test
    @DisplayName("Should get user by ID")
    void testGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUserName());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return null for non-existent user")
    void testGetUserByIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        User result = userService.getUserById(999L);

        assertNull(result);
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userRepository.findByUserName("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUserName());
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("Should register new user")
    void testRegisterUser() {
        User newUser = new User();
        newUser.setUserName("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassWord("password");

        when(userRepository.findByUserName("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedpassword");

        userService.registerUser(newUser);

        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password");
    }

    @Test
    @DisplayName("Should throw exception for duplicate username")
    void testRegisterDuplicateUsername() {
        User newUser = new User();
        newUser.setUserName("testuser"); // Duplicate
        newUser.setEmail("new@example.com");
        newUser.setPassWord("password");

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> userService.registerUser(newUser));
    }

    @Test
    @DisplayName("Should authenticate user with correct password")
    void testAuthenticateUserSuccess() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "hashedpassword")).thenReturn(true);

        User result = userService.authenticateUser("testuser", "password");

        assertNotNull(result);
        assertEquals("testuser", result.getUserName());
    }

    @Test
    @DisplayName("Should throw exception for incorrect password")
    void testAuthenticateUserWrongPassword() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.authenticateUser("testuser", "wrongpassword"));
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void testAuthenticateUserNotFound() {
        when(userRepository.findByUserName("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.authenticateUser("nonexistent", "password"));
    }

    @Test
    @DisplayName("Should update user")
    void testUpdateUser() {
        User updatedUser = new User();
        updatedUser.setUserName("updatedname");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPassWord("newpassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedupdatedpassword");

        userService.updateUser(1L, updatedUser);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user")
    void testDeleteUser() {
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(testUser);

        verify(userRepository, times(1)).delete(testUser);
    }
}
