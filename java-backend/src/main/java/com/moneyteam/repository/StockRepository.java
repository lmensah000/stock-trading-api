package com.moneyteam.repository;

import com.moneyteam.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Stock entity operations.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, String> {

    Optional<Stock> findByStockTicker(String stockTicker);

    List<Stock> findBySector(String sector);

    List<Stock> findByStockNameContainingIgnoreCase(String name);

    @Query("SELECT s FROM Stock s WHERE s.volume > :minVolume ORDER BY s.volume DESC")
    List<Stock> findByMinimumVolume(Integer minVolume);

    @Query("SELECT s FROM Stock s ORDER BY s.markChange DESC")
    List<Stock> findTopGainers();

    @Query("SELECT s FROM Stock s ORDER BY s.markChange ASC")
    List<Stock> findTopLosers();

    @Query("SELECT s FROM Stock s ORDER BY s.volume DESC")
    List<Stock> findMostActive();

    boolean existsByStockTicker(String stockTicker);
}
