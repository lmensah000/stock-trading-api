package com.moneyteam.repository;

import com.moneyteam.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionsTradeRepository extends JpaRepository <Trade, Long> {

    List<Trade> findAll();
}
