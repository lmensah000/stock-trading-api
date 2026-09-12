package com.moneyteam.trading.repository;

import com.moneyteam.trading.model.OptionTradeDetails;
import com.moneyteam.trading.model.Options;
import com.moneyteam.trading.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionsTradeRepository extends JpaRepository <OptionTradeDetails, Long> {

    // Returns OptionTradeDetails, not Options: optionType is a field on
    // OptionTradeDetails (the repository root), and a derived query cannot
    // return a different entity than the one it queries.
    List<OptionTradeDetails> findByOptionType(String optionType);
}
