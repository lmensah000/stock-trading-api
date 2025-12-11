package com.moneyteam.repository;

import com.moneyteam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);

    Optional<User> findByUserNameAndPassWord(String userName, String passWord);

    Optional<User> findByEmail(String email);

}

/*
In the above example, the User class defines the attributes commonly found in a UserRepository, such as id, username,
password, email, name, role, accountStatus, creationDate, and lastLogin. You can add additional attributes as per your requirements.


The UserRepository interface defines several methods for performing common operations on users data, including finding
 a users by ID, username, or email, retrieving all users, saving a new users, updating an existing users, and deleting a users.


Please note that this is a simplified example, and you may need to adapt it to fit your specific application framework, persistence technology (such as JDBC or an ORM like Hibernate), or any additional requirements you may have.
 */