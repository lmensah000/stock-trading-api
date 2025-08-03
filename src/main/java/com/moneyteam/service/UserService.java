package com.moneyteam.service;

import com.moneyteam.model.User;

public interface UserService {
    User authenticateUser(String username, String password);
    void registerUser(User user);
    // Other methods for managing user-specific data
}