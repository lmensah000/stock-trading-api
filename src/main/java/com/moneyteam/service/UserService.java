package com.moneyteam.service;

import com.moneyteam.model.User;

public interface UserService {
    User authenticateUser(String userName, String passWord);
    void registerUser(User user);
    void updateUser(User user, User newUser);
    void deleteUser(User user);// Other methods for managing user-specific data

    User getUserById(Long userId);
}