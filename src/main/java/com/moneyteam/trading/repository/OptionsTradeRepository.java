package com.moneyteam.trading.repository;

import com.moneyteam.trading.model.OptionTradeDetails;
import com.moneyteam.trading.model.Options;
import com.moneyteam.trading.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionsTradeRepository extends JpaRepository <OptionTradeDetails, Long> {

    //List<OptionTradeDetails> findByStockTicker(String stockTicker);
    List<Options> findByOptionType(String optionType);
}
