package com.moneyteam.repository;

import com.moneyteam.model.OptionTradeDetails;
import com.moneyteam.model.Options;
import com.moneyteam.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionsTradeRepository extends JpaRepository <OptionTradeDetails, Long> {

    //List<OptionTradeDetails> findByStockTicker(String stockTicker);
    List<Options> findByOptionType(String optionType);
}
