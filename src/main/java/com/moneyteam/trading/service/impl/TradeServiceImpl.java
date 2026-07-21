package com.moneyteam.trading.service.impl;

import com.moneyteam.trading.dto.TradeRequestDto;
import com.moneyteam.trading.dto.TradeResponseDto;
import com.moneyteam.trading.mapper.TradeMapper;
import com.moneyteam.trading.model.Trade;
import com.moneyteam.trading.model.Position;
import com.moneyteam.user.model.User;
import com.moneyteam.trading.model.enums.TradeStatus;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.user.repository.UserRepository;
import com.moneyteam.trading.repository.PositionRepository;
import com.moneyteam.trading.repository.TradeRepository;
import com.moneyteam.marketdata.repository.StockTradeRepository;
import com.moneyteam.trading.repository.OptionsTradeRepository;
import com.moneyteam.trading.service.AccountService;
import com.moneyteam.trading.service.PnLCalculator;
import com.moneyteam.trading.service.TradeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

//private static final Logger log = LoggerFactory.getLogger(TradeServiceImpl.class);

@Service
@Transactional
public class TradeServiceImpl implements TradeService {

    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final StockTradeRepository stockTradeRepository;
    private final OptionsTradeRepository optionsTradeRepository;
    private final AccountService accountService;

    public TradeServiceImpl(
            UserRepository userRepository,
            PositionRepository positionRepository,
            TradeRepository tradeRepository,
            StockTradeRepository stockTradeRepository,
            OptionsTradeRepository optionsTradeRepository,
            AccountService accountService
    ) {
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.stockTradeRepository = stockTradeRepository;
        this.optionsTradeRepository = optionsTradeRepository;
        this.accountService = accountService;
    }
//    public void executeTrade (Trade trade) {
//        log.info("Executing trade: ");
//
    //    private final StockTradeRepository stockTradeRepository;
    @Override
    @Transactional
    public TradeResponseDto create(TradeRequestDto request) {
        Trade trade = TradeMapper.toEntity(request);
        trade.setStatus(TradeStatus.PENDING);
        Trade saved = tradeRepository.save(TradeMapper.toEntity(request));
        return TradeMapper.toDto(saved);
    }

    @Override
    public Optional<TradeResponseDto> getById(Long id) {
        return tradeRepository.findById(id).map(TradeMapper::toDto);
    }

    @Override
    public List<TradeResponseDto> listByUser(Long userId) {
        return tradeRepository.findByUserTradeId(userId)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listBySide(OrderSide side) {
        return tradeRepository.findBySide(side)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listByStockTicker(String stockTicker) {
        return tradeRepository.findByStockTickerIgnoreCase(stockTicker)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listBetween(LocalDateTime start, LocalDateTime end) {
        return tradeRepository.findByExecutionDateBetween(start, end)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    @Transactional
    public TradeResponseDto updateStatus(Long id, TradeStatus newStatus) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trade not found: " + id));
        trade.setStatus(newStatus);
        return TradeMapper.toDto(tradeRepository.save(trade));
    }

    @Override
    public TradeResponseDto placeTrade(TradeRequestDto dto) {

    User user = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
    //Fetch or create position
    Position position = positionRepository
            .findByUsersIdAndStockTicker(dto.getUserId(), dto.getStockTicker())
            .orElseGet(() -> createNewPosition(user, dto.getStockTicker()));
    // Build Trade
    Trade trade = new Trade();
        trade.setUsers(user);
        trade.setPosition(position);
        trade.setStockTicker(dto.getStockTicker());
        trade.setQuantity(dto.getQuantity());
        trade.setPrice(dto.getPrice());
        trade.setExecutionDate(LocalDateTime.now());
        trade.setSide(dto.getSide());
        trade.setStatus(TradeStatus.PENDING);

        Trade saved = tradeRepository.save(trade);

        BigDecimal settlementAmount = dto.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
        if (dto.getSide() == OrderSide.BUY) {
            accountService.debitForBuy(dto.getUserId(), settlementAmount, saved.getId());
        } else if (dto.getSide() == OrderSide.SELL) {
            accountService.creditForSell(dto.getUserId(), settlementAmount, saved.getId());
        }

    // Update the position based on the trade
    updatePosition(position, saved);

    return TradeMapper.toDto(saved);
}


    private Position createNewPosition(User user, String stockTicker) {
    Position position = new Position();
    position.setUsers(user);
    position.setStockTicker(stockTicker);
    position.setAveragePrice(BigDecimal.ZERO);
    position.setTotalQuantity(0.0);
    return positionRepository.save(position);
}

    @Override
    public List<TradeResponseDto> getTradeHistory(Long userId) {
    return List.of();
}

    @Override
    public List<?> getUserPositions(Long userId) {
    return List.of();
}

    @Override
    public void cancelTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NoSuchElementException("Trade not found: " + tradeId));
        trade.setStatus(TradeStatus.CANCELLED);
        tradeRepository.save(trade);
    }

    //helper function
    @Override
    public void updatePosition(Position position, Trade trade) {
    double qty = trade.getQuantity();
    BigDecimal price = trade.getPrice();

    if (trade.getSide() == OrderSide.BUY) {
        double oldQty = position.getTotalQuantity();
        BigDecimal oldAvg = position.getAveragePrice();

        double newQty = oldQty + qty;

        BigDecimal newAvg = (oldAvg.multiply(BigDecimal.valueOf(oldQty))
                .add(price.multiply(BigDecimal.valueOf(qty))))
                .divide(BigDecimal.valueOf(newQty), 4, java.math.RoundingMode.HALF_UP);

        position.setTotalQuantity(newQty);
        position.setAveragePrice(newAvg);
    } else if (trade.getSide() == OrderSide.SELL) {
        double heldQty = position.getTotalQuantity();
        if (qty > heldQty) {
            throw new IllegalArgumentException(
                    "Cannot sell " + qty + " shares of " + position.getStockTicker() +
                            "; position only holds " + heldQty);
        }

        BigDecimal realized = PnLCalculator.realizedPnL(position.getAveragePrice(), price, qty);
        position.setRealizedPnL(position.getRealizedPnL().add(realized));
        position.setTotalQuantity(heldQty - qty);
        // averagePrice on the remaining shares is unchanged by a sell
    }

    positionRepository.save(position);
}

}