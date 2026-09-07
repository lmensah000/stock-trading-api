package com.moneyteam.common.security;

import com.moneyteam.user.model.User;
import com.moneyteam.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Resolves the acting user from the authenticated principal.
 *
 * This is the only sanctioned source of caller identity. Request bodies and path
 * variables must never supply a user id: doing so let any authenticated caller
 * act on another account simply by changing the value.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }

        String userName = authentication.getName();
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new NoSuchElementException("Authenticated user no longer exists: " + userName));
    }

    @Transactional(readOnly = true)
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
