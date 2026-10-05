package com.moneyteam.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyteam.alert.model.Alert;
import com.moneyteam.alert.repository.AlertRepository;
import com.moneyteam.strategy.model.Confidence;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.model.SignalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Alert storage, and the suppression that keeps it usable.
 *
 * Without the cooldown a five-minute scan re-raises an unchanged condition
 * twelve times an hour, which trains its reader to ignore the feed. These tests
 * treat suppression as core behaviour rather than an optimisation.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final long USER = 11L;
    private static final Instant NOW = Instant.parse("2024-03-04T19:00:00Z");

    @Mock
    private AlertRepository alertRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, new ObjectMapper(), 60);
    }

    private static Signal signal(String dedupeKey) {
        return new Signal(
                SignalType.PERCENT_MOVE, "AAPL", Direction.BULLISH, Confidence.MEDIUM,
                NOW, "AAPL moved 5%", dedupeKey,
                Map.of("changePercent", "5.0000", "thresholdPercent", "4"));
    }

    @Test
    @DisplayName("a new condition is stored")
    void storesNewSignal() {
        when(alertRepository.existsByUserIdAndDedupeKeyAndDetectedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Alert> stored = alertService.raise(USER, signal("PERCENT_MOVE:AAPL:BULLISH"));

        assertThat(stored).isPresent();
        assertThat(stored.get().getStockTicker()).isEqualTo("AAPL");
        assertThat(stored.get().getUserId()).isEqualTo(USER);
    }

    @Test
    @DisplayName("the same condition inside the cooldown is suppressed, not stored again")
    void suppressesDuplicateWithinCooldown() {
        when(alertRepository.existsByUserIdAndDedupeKeyAndDetectedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(true);

        Optional<Alert> stored = alertService.raise(USER, signal("PERCENT_MOVE:AAPL:BULLISH"));

        assertThat(stored).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("suppression is scoped per user and per condition")
    void suppressionIsScoped() {
        when(alertRepository.existsByUserIdAndDedupeKeyAndDetectedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        alertService.raise(USER, signal("PERCENT_MOVE:AAPL:BULLISH"));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertRepository).existsByUserIdAndDedupeKeyAndDetectedAtAfter(
                org.mockito.ArgumentMatchers.eq(USER), keyCaptor.capture(), any());

        assertThat(keyCaptor.getValue()).isEqualTo("PERCENT_MOVE:AAPL:BULLISH");
    }

    @Test
    @DisplayName("the cooldown window is measured back from the signal's own time")
    void cooldownWindowUsesSignalTime() {
        when(alertRepository.existsByUserIdAndDedupeKeyAndDetectedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        alertService.raise(USER, signal("K"));

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(alertRepository).existsByUserIdAndDedupeKeyAndDetectedAtAfter(
                anyLong(), anyString(), sinceCaptor.capture());

        assertThat(sinceCaptor.getValue()).isEqualTo(NOW.minusSeconds(3600));
    }

    @Test
    @DisplayName("evidence is stored as JSON so the signal can be audited later")
    void evidenceIsPersisted() {
        when(alertRepository.existsByUserIdAndDedupeKeyAndDetectedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(false);
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        Alert stored = alertService.raise(USER, signal("K")).orElseThrow();

        assertThat(stored.getEvidence())
                .contains("changePercent")
                .contains("5.0000");
    }

    @Test
    @DisplayName("acknowledging stamps the time")
    void acknowledgeStampsTime() {
        Alert alert = new Alert();
        alert.setUserId(USER);
        when(alertRepository.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        Alert acknowledged = alertService.acknowledge(USER, 5L);

        assertThat(acknowledged.isAcknowledged()).isTrue();
    }

    @Test
    @DisplayName("acknowledging twice keeps the original timestamp")
    void acknowledgeIsIdempotent() {
        Instant first = NOW.minusSeconds(600);
        Alert alert = new Alert();
        alert.setUserId(USER);
        alert.setAcknowledgedAt(first);
        when(alertRepository.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(alertService.acknowledge(USER, 5L).getAcknowledgedAt()).isEqualTo(first);
    }

    @Test
    @DisplayName("another user's alert reports not-found, so ids cannot be probed")
    void otherUsersAlertIsNotFound() {
        when(alertRepository.findByIdAndUserId(5L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.acknowledge(999L, 5L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Alert not found");
    }
}
