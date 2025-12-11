package com.moneyteam.service;

import com.moneyteam.model.User;

public interface UserService {
    User authenticateUser(String userName, String passWord);
    void registerUser(User users);
    void updateUser(Long userId, User newUser);
    void deleteUser(User users);// Other methods for managing users-specific data

    User getUserById(Long userId);
}