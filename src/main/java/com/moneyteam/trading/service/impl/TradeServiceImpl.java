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
    /**
     * Loads a trade only if it belongs to the acting user. Reporting a
     * not-found for someone else's trade avoids confirming that the id exists.
     */
    private Trade requireOwnedTrade(Long userId, Long tradeId) {
        return tradeRepository.findByIdAndUserTradeId(tradeId, userId)
                .orElseThrow(() -> new NoSuchElementException("Trade not found: " + tradeId));
    }

    @Override
    @Transactional
    public TradeResponseDto create(Long userId, TradeRequestDto request) {
        Trade trade = TradeMapper.toEntity(request);
        trade.setUserTradeId(userId);
        trade.setStatus(TradeStatus.PENDING);
        return TradeMapper.toDto(tradeRepository.save(trade));
    }

    @Override
    public Optional<TradeResponseDto> getById(Long userId, Long id) {
        return tradeRepository.findByIdAndUserTradeId(id, userId).map(TradeMapper::toDto);
    }

    @Override
    public List<TradeResponseDto> listByUser(Long userId) {
        return tradeRepository.findByUserTradeId(userId)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listBySide(Long userId, OrderSide side) {
        return tradeRepository.findByUserTradeIdAndSide(userId, side)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listByStatus(Long userId, TradeStatus status) {
        return tradeRepository.findByUserTradeIdAndStatus(userId, status)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listByStockTicker(Long userId, String stockTicker) {
        return tradeRepository.findByUserTradeIdAndStockTickerIgnoreCase(userId, stockTicker)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listBetween(Long userId, LocalDateTime start, LocalDateTime end) {
        return tradeRepository.findByUserTradeIdAndExecutionDateBetween(userId, start, end)
                .stream().map(TradeMapper::toDto).toList();
    }

    @Override
    @Transactional
    public TradeResponseDto updateStatus(Long userId, Long id, TradeStatus newStatus) {
        Trade trade = requireOwnedTrade(userId, id);
        trade.setStatus(newStatus);
        return TradeMapper.toDto(tradeRepository.save(trade));
    }

    @Override
    public TradeResponseDto placeTrade(Long userId, TradeRequestDto dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        //Fetch or create position
        Position position = positionRepository
                .findByUsersIdAndStockTicker(userId, dto.getStockTicker())
                .orElseGet(() -> createNewPosition(user, dto.getStockTicker()));

        Trade trade = new Trade();
        trade.setUsers(user);
        trade.setUserTradeId(userId);
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
            accountService.debitForBuy(userId, settlementAmount, saved.getId());
        } else if (dto.getSide() == OrderSide.SELL) {
            accountService.creditForSell(userId, settlementAmount, saved.getId());
        }

        // Update the position based on the trade
        updatePosition(position, saved);

        return TradeMapper.toDto(saved);
    }


    private Position createNewPosition(User user, String stockTicker) {
        Position position = new Position();
        position.setUsers(user);
        // users is a read-only mapping, so the scalar FK must be set explicitly
        // or the NOT NULL user_id column is written as null.
        position.setUserRefId(user.getId());
        position.setStockTicker(stockTicker);
        position.setAveragePrice(BigDecimal.ZERO);
        position.setTotalQuantity(0.0);
        return positionRepository.save(position);
    }

    @Override
    public List<TradeResponseDto> getTradeHistory(Long userId) {
        return listByUser(userId);
    }

    @Override
    public List<Position> getUserPositions(Long userId) {
        return positionRepository.findByUserRefId(userId);
    }

    @Override
    public void cancelTrade(Long userId, Long tradeId) {
        Trade trade = requireOwnedTrade(userId, tradeId);
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