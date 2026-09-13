package com.moneyteam.trading.service.impl;

import com.moneyteam.marketdata.repository.StockTradeRepository;
import com.moneyteam.trading.model.Position;
import com.moneyteam.trading.model.Trade;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.repository.OptionsTradeRepository;
import com.moneyteam.trading.repository.PositionRepository;
import com.moneyteam.trading.repository.TradeRepository;
import com.moneyteam.trading.service.AccountService;
import com.moneyteam.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Average-cost position maintenance.
 *
 * This method previously reduced quantity on a SELL while booking no realized
 * P&L and without guarding against selling more than the position held; both
 * cases are pinned here.
 */
@ExtendWith(MockitoExtension.class)
class TradeServiceImplUpdatePositionTest {

    @Mock private UserRepository userRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private StockTradeRepository stockTradeRepository;
    @Mock private OptionsTradeRepository optionsTradeRepository;
    @Mock private AccountService accountService;
    @InjectMocks private TradeServiceImpl tradeService;

    private Position position;

    @BeforeEach
    void setUp() {
        position = new Position();
        position.setStockTicker("AAPL");
        position.setTotalQuantity(0.0);
        position.setAveragePrice(BigDecimal.ZERO);
        position.setRealizedPnL(BigDecimal.ZERO);
    }

    private static Trade trade(OrderSide side, double qty, String price) {
        Trade t = new Trade();
        t.setSide(side);
        t.setQuantity(qty);
        t.setPrice(new BigDecimal(price));
        return t;
    }

    @Test
    @DisplayName("a first BUY sets quantity and average price to the fill")
    void firstBuy() {
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));

        assertThat(position.getTotalQuantity()).isEqualTo(10.0);
        assertThat(position.getAveragePrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("a second BUY averages cost across both fills")
    void averagesAcrossBuys() {
        // 10 @ 100 then 10 @ 120 -> 20 @ 110
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "120.00"));

        assertThat(position.getTotalQuantity()).isEqualTo(20.0);
        assertThat(position.getAveragePrice()).isEqualByComparingTo("110.00");
    }

    @Test
    @DisplayName("averaging weights by quantity, not by fill count")
    void averagingIsQuantityWeighted() {
        // 90 @ 10 then 10 @ 110 -> (900 + 1100) / 100 = 20
        position.setTotalQuantity(0.0);
        tradeService.updatePosition(position, trade(OrderSide.BUY, 90, "10.00"));
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "110.00"));

        assertThat(position.getAveragePrice()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("a SELL books realized P&L and reduces quantity")
    void sellBooksRealizedPnL() {
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));

        tradeService.updatePosition(position, trade(OrderSide.SELL, 4, "130.00"));

        assertThat(position.getTotalQuantity()).isEqualTo(6.0);
        // (130 - 100) * 4
        assertThat(position.getRealizedPnL()).isEqualByComparingTo("120.0000");
    }

    @Test
    @DisplayName("a SELL leaves average cost of the remaining shares unchanged")
    void sellDoesNotDisturbAverageCost() {
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));

        tradeService.updatePosition(position, trade(OrderSide.SELL, 4, "130.00"));

        assertThat(position.getAveragePrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("realized P&L accumulates across multiple sells")
    void realizedPnLAccumulates() {
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));

        tradeService.updatePosition(position, trade(OrderSide.SELL, 3, "110.00")); //  +30
        tradeService.updatePosition(position, trade(OrderSide.SELL, 2, "90.00"));  //  -20

        assertThat(position.getRealizedPnL()).isEqualByComparingTo("10.0000");
        assertThat(position.getTotalQuantity()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("a losing sell books negative realized P&L")
    void losingSell() {
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));

        tradeService.updatePosition(position, trade(OrderSide.SELL, 10, "80.00"));

        assertThat(position.getRealizedPnL()).isEqualByComparingTo("-200.0000");
        assertThat(position.getTotalQuantity()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("selling more than the position holds is rejected and changes nothing")
    void oversellIsRejected() {
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));

        assertThatThrownBy(() -> tradeService.updatePosition(position, trade(OrderSide.SELL, 11, "100.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot sell");

        assertThat(position.getTotalQuantity()).isEqualTo(10.0);
        assertThat(position.getRealizedPnL()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("selling exactly the full position is allowed")
    void sellEntirePositionIsAllowed() {
        tradeService.updatePosition(position, trade(OrderSide.BUY, 10, "100.00"));

        tradeService.updatePosition(position, trade(OrderSide.SELL, 10, "100.00"));

        assertThat(position.getTotalQuantity()).isEqualTo(0.0);
    }
}
