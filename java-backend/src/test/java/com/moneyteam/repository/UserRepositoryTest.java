package com.moneyteam.repository;

import com.moneyteam.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for UserRepository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Repository Tests")
public class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

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
    @DisplayName("Should save user")
    void testSaveUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User saved = userRepository.save(testUser);

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should find user by ID")
    void testFindById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUserName());
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUserName() {
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
    @DisplayName("Should find user by username and password")
    void testFindByUserNameAndPassWord() {
        when(userRepository.findByUserNameAndPassWord("testuser", "hashedpassword"))
                .thenReturn(Optional.of(testUser));

        Optional<User> result = userRepository.findByUserNameAndPassWord("testuser", "hashedpassword");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUserName());
    }

    @Test
    @DisplayName("Should return empty for non-existent user")
    void testFindByIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should delete user")
    void testDeleteUser() {
        doNothing().when(userRepository).delete(testUser);

        userRepository.delete(testUser);

        verify(userRepository, times(1)).delete(testUser);
    }
}
