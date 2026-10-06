package com.moneyteam.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Infrastructure shared across features.
 *
 * The {@link Clock} bean previously lived in {@code marketdata/config}. It
 * moved here because {@code common} may not depend on a feature package, and
 * {@code common/security} now needs it for refresh token expiry. Injecting a
 * clock rather than calling {@code Instant.now()} is what lets expiry and
 * cooldown behaviour be tested without sleeping.
 */
@Configuration
@EnableScheduling
public class CoreConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
