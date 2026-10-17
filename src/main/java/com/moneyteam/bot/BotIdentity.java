package com.moneyteam.bot;

import com.moneyteam.user.model.User;
import com.moneyteam.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * The single account the bot acts as.
 *
 * This exists so the bot has an identity without weakening the rule that no
 * caller supplies one. {@code CurrentUserService} answers "who is making this
 * request" from the authenticated principal; there is no principal on a
 * scheduled scan, so this answers "who does the bot act as" from configuration
 * instead. Both are sources the client cannot influence, which is the property
 * that matters.
 *
 * The owner is named by {@code app.bot.owner-username} and resolved against the
 * database. Note what is deliberately absent: there is no method taking a user
 * id, and nothing here reads a request. Services downstream continue to take
 * the user id as an explicit parameter exactly as before - the bot simply
 * supplies it in place of a logged-in caller, so the compiler still catches a
 * path that forgets.
 *
 * Resolution is not cached across calls. Caching the id at startup would be
 * faster and was the original intention, but it would also mean the bot kept
 * acting as a user that had since been deleted; a lookup per scan is far
 * cheaper than that class of bug.
 */
@Component
public class BotIdentity {

    private final UserRepository userRepository;
    private final String ownerUserName;

    public BotIdentity(UserRepository userRepository,
                       @Value("${app.bot.owner-username:}") String ownerUserName) {
        this.userRepository = userRepository;
        this.ownerUserName = ownerUserName == null ? "" : ownerUserName.trim();
    }

    /** The configured owner's username, or empty when none is configured. */
    public String ownerUserName() {
        return ownerUserName;
    }

    public boolean isConfigured() {
        return !ownerUserName.isEmpty();
    }

    /**
     * The owner's user id.
     *
     * @throws IllegalStateException  when no owner is configured
     * @throws NoSuchElementException when the configured owner has no user row
     */
    @Transactional(readOnly = true)
    public Long ownerUserId() {
        return owner().getId();
    }

    @Transactional(readOnly = true)
    public User owner() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "No bot owner is configured. Set app.bot.owner-username (BOT_OWNER_USERNAME).");
        }
        return userRepository.findByUserName(ownerUserName)
                .orElseThrow(() -> new NoSuchElementException(
                        "The configured bot owner does not exist: " + ownerUserName));
    }
}
