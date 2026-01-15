package com.moneyteam.service.impl;

import com.moneyteam.model.User;
import com.moneyteam.repository.UserRepository;
import com.moneyteam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of UserService for user management operations.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticateUser(String userName, String rawPassword) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassWord())) {
            throw new RuntimeException("Incorrect password");
        }

        return user;
    }

    @Override
    public void registerUser(User user) {
        // Check if username already exists
        if (userRepository.findByUserName(user.getUserName()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        
        // Encode password before saving
        user.setPassWord(passwordEncoder.encode(user.getPassWord()));
        userRepository.save(user);
    }

    @Override
    public void updateUser(Long userId, User newUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (newUser.getUserName() != null && !newUser.getUserName().isEmpty()) {
            existingUser.setUserName(newUser.getUserName());
        }
        
        if (newUser.getPassWord() != null && !newUser.getPassWord().isEmpty()) {
            existingUser.setPassWord(passwordEncoder.encode(newUser.getPassWord()));
        }
        
        if (newUser.getEmail() != null && !newUser.getEmail().isEmpty()) {
            existingUser.setEmail(newUser.getEmail());
        }

        userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deleteUser(User user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Invalid user");
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
