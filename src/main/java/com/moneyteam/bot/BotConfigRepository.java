package com.moneyteam.bot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BotConfigRepository extends JpaRepository<BotConfig, Long> {

    /**
     * The single configuration row, if it has been created yet.
     *
     * Declared as a query rather than a {@code default} method delegating to a
     * derived finder. A default method on a Mockito mock is not executed - the
     * mock answers it directly - so a test stubbing the delegate would appear
     * to work and silently return empty. Keeping this a real repository method
     * means it mocks the way every other repository method here does.
     *
     * Looked up by the constant {@code singleton} column rather than by id,
     * because callers must not have to know the generated id of a row there is
     * only ever one of.
     */
    @Query("select c from BotConfig c where c.singleton = 1")
    Optional<BotConfig> findSingleton();
}
