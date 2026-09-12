package com.moneyteam.user.service.impl;

import com.moneyteam.common.security.LoginAttemptService;
import com.moneyteam.user.model.User;
import com.moneyteam.user.model.enums.Role;
import com.moneyteam.user.repository.UserRepository;
import com.moneyteam.user.service.UserService;
import com.moneyteam.trading.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final LoginAttemptService loginAttemptService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AccountService accountService,
                           LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticateUser(String userName, String rawPassword) {
        if (loginAttemptService.isLocked(userName)) {
            throw new LockedException("Account temporarily locked after too many failed login attempts. Try again in "
                    + loginAttemptService.getLockDuration().toMinutes() + " minutes.");
        }

        Optional<User> found = userRepository.findByUserName(userName);

        // Deliberately identical failure for "no such user" and "wrong password":
        // distinguishing them lets an attacker enumerate valid usernames.
        if (found.isEmpty() || !passwordEncoder.matches(rawPassword, found.get().getPassWord())) {
            loginAttemptService.recordFailure(userName);
            throw new BadCredentialsException("Invalid username or password");
        }

        loginAttemptService.recordSuccess(userName);
        return found.get();
    }

    @Override
    public void registerUser(User users) {
        users.setPassWord(passwordEncoder.encode(users.getPassWord()));
        if (users.getRole() == null) {
            users.setRole(Role.USER);
        }
        User saved = userRepository.save(users);
        accountService.createAccountForUser(saved.getId());
    }

    //update users
    @Override
    public void updateUser(Long userId, User newUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

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
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + userId));
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