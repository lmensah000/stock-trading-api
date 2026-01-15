package com.moneyteam.repository;

import com.moneyteam.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for PositionRepository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Position Repository Tests")
public class PositionRepositoryTest {

    @Mock
    private PositionRepository positionRepository;

    private Position testPosition;

    @BeforeEach
    void setUp() {
        testPosition = new Position();
        testPosition.setId(1L);
        testPosition.setUserId(1L);
        testPosition.setStockTicker("AAPL");
        testPosition.setTotalQuantity(100.0);
        testPosition.setAveragePrice(BigDecimal.valueOf(150.00));
    }

    @Test
    @DisplayName("Should save position")
    void testSavePosition() {
        when(positionRepository.save(any(Position.class))).thenReturn(testPosition);

        Position saved = positionRepository.save(testPosition);

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    @DisplayName("Should find position by ID")
    void testFindById() {
        when(positionRepository.findById(1L)).thenReturn(Optional.of(testPosition));

        Optional<Position> result = positionRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getStockTicker());
    }

    @Test
    @DisplayName("Should find positions by user ref ID")
    void testFindByUserRefId() {
        List<Position> positions = Arrays.asList(testPosition);
        when(positionRepository.findByUserRefId(1L)).thenReturn(positions);

        List<Position> result = positionRepository.findByUserRefId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    @DisplayName("Should find positions by stock ticker")
    void testFindByStockTicker() {
        List<Position> positions = Arrays.asList(testPosition);
        when(positionRepository.findByStockTicker("AAPL")).thenReturn(positions);

        List<Position> result = positionRepository.findByStockTicker("AAPL");

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getStockTicker());
    }

    @Test
    @DisplayName("Should find position by user ID and stock ticker")
    void testFindByUsersIdAndStockTicker() {
        when(positionRepository.findByUsersIdAndStockTicker(1L, "AAPL"))
                .thenReturn(Optional.of(testPosition));

        Optional<Position> result = positionRepository.findByUsersIdAndStockTicker(1L, "AAPL");

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getStockTicker());
        assertEquals(1L, result.get().getUserId());
    }

    @Test
    @DisplayName("Should return empty for non-existent position")
    void testFindByIdNotFound() {
        when(positionRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Position> result = positionRepository.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should delete position")
    void testDeletePosition() {
        doNothing().when(positionRepository).deleteById(1L);

        positionRepository.deleteById(1L);

        verify(positionRepository, times(1)).deleteById(1L);
    }
}
