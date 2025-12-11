package com.moneyteam.service.impl;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.mapper.TradeMapper;
import com.moneyteam.model.*;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.repository.*;
import com.moneyteam.service.TradeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.CascadeType;
import javax.persistence.OneToOne;
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

    public TradeServiceImpl(
            UserRepository userRepository,
            PositionRepository positionRepository,
            TradeRepository tradeRepository,
            StockTradeRepository stockTradeRepository,
            OptionsTradeRepository optionsTradeRepository
    ) {
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.stockTradeRepository = stockTradeRepository;
        this.optionsTradeRepository = optionsTradeRepository;
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
    public List<TradeResponseDto> listByType(TradeType tradeType) {
        return tradeRepository.findByTradeType(tradeType)
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
        trade.setTradeType(dto.getTradeType());
        trade.setStatus(TradeStatus.PENDING);

        tradeRepository.save(trade);

    // Update the position based on the trade
    updatePosition(position, trade);

    return TradeMapper.toDto(trade);
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

    @OneToOne(mappedBy = "trade", cascade = CascadeType.ALL)
    private OptionTradeDetails optionDetails;

    //helper function
    @Override
    public void updatePosition(Position position, Trade trade) {
//        final StockTradeRequest tradeRequest;
//        TradeType tradeType = tradeRequest.getTradeType();
    double qty = trade.getQuantity();
    BigDecimal price = trade.getPrice();

    if (trade.getTradeType() == TradeType.BUY) {
        double oldQty = position.getTotalQuantity();
        BigDecimal oldAvg = position.getAveragePrice();

        double newQty = oldQty + qty;

        BigDecimal newAvg = (oldAvg.multiply(BigDecimal.valueOf(oldQty))
                .add(price.multiply(BigDecimal.valueOf(qty))))
                .divide(BigDecimal.valueOf(newQty), BigDecimal.ROUND_HALF_UP);

        position.setTotalQuantity(newQty);
        position.setAveragePrice(newAvg);
    } else if (trade.getTradeType() == TradeType.SELL) {
        double newQty = position.getTotalQuantity() - qty;
        position.setTotalQuantity(newQty);
        // You might choose not to change averagePrice on SELL
    }

    positionRepository.save(position);
}

}