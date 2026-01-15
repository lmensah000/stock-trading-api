package com.moneyteam.service.impl;

import com.moneyteam.model.User;
import com.moneyteam.repository.UserRepository;
import com.moneyteam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
//find by users then fill in users and password?
        User users = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(rawPassword, users.getPassWord())) {
            throw new RuntimeException("Incorrect password");
        }

        return users;
    }

    @Override
    public void registerUser(User users) {
        users.setPassWord(passwordEncoder.encode(users.getPassWord()));
        userRepository.save(users);
    }

    //update users
    @Override
    public void updateUser(Long userId, User newUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        existingUser.setUserName(newUser.getUserName());;
        existingUser.setPassWord(newUser.getPassWord());//
        existingUser.setEmail(newUser.getEmail());

//        existingUser.setBuy(newUser.getBuy());
//        existingUser.setSell(newUser.getSell());
//        existingUser.setPlaceOrder(newUser.getPlaceOrder());
//        existingUser.setLogin(newUser.getLogin());
        userRepository.save(existingUser);// Logic for registering a new users
    }

    //delete users
    @Override
    @Transactional
    public void deleteUser(User users) {
        userRepository.delete(users);
        // Logic for registering a new users
        // Save the users details to the userRepository
    }

    @Override
    public User getUserById(Long userId) {
        return null;
    }
    // Implement other methods for managing users-specific data
}

/*

The UserService handles users-related functionalities such as authentication, registration, and managing users-specific data.
Here are some examples of users-related functionalities:
Authentication: The authenticateUser method receives a username and password as input, verifies the credentials
against the stored users data, and returns the authenticated users object. Example: User authenticatedUser = userService.authenticateUser(username, password);
Registration: The registerUser method takes a User object as input, saves the users details to the database or
repository, and completes the users registration process. Example: userService.registerUser(newUser);
Managing User Data: The UserService can have methods to update users information, retrieve users-specific data,
or perform any other operations related to managing users data.

User Profile: Allow users to create and update their profiles, including personal information, contact details,
and preferences. This data can be stored in a users-specific database table or document.

Account Balances: Keep track of users' account balances, including available funds for trading, holdings,
and transaction history. This information can be stored in a users-specific account table or document.

Watchlists: Enable users to create and manage watchlists.sql of stocks or options they are interested in.
Store the watchlist data associated with each users, allowing them to easily track their preferred securities.

Trade History: Maintain a record of users' past trades, including details such as trade type, date, time,
quantity, price, and any associated fees. This data can be stored in a users-specific trade history table or document.

Notifications: Implement a notification system to keep users informed about important events, such as trade
executions, account updates, or market alerts. Store the notification preferences and history for each users.

Preferences and Settings: Allow users to customize their app experience by providing options to set preferences
 and configure settings, such as language, theme, notification preferences, and default trading strategies.

Security and Authentication: Implement users authentication and authorization mechanisms to ensure secure
access to users-specific data. Store users credentials securely and manage users sessions to maintain a secure environment.


These are just a few examples of managing users-specific data in your stock trading app. The specific functionalities
and data management requirements may vary based on your app's design and users needs.


Remember to handle users data responsibly, following applicable privacy regulations and best practices to protect
 users privacy and ensure data security.
 */