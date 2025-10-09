package com.moneyteam.repository;

import com.moneyteam.model.User;

import java.util.List;
import java.util.Optional;

// UserRepository.java
public interface UserRepository {
    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndPassword(String username, String password);


    Optional<User> findByEmail(String email);
    
    List<User> findAll();
    
    void save(User user);
    
    void update(User user);
    
    void delete(User user);
}

/*
In the above example, the User class defines the attributes commonly found in a UserRepository, such as id, username,
password, email, name, role, accountStatus, creationDate, and lastLogin. You can add additional attributes as per your requirements.


The UserRepository interface defines several methods for performing common operations on user data, including finding
 a user by ID, username, or email, retrieving all users, saving a new user, updating an existing user, and deleting a user.


Please note that this is a simplified example, and you may need to adapt it to fit your specific application framework, persistence technology (such as JDBC or an ORM like Hibernate), or any additional requirements you may have.
 */