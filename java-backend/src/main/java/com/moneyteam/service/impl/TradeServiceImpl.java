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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of TradeService for trade management operations.
 */
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

    @Override
    @Transactional
    public TradeResponseDto create(TradeRequestDto request) {
        Trade trade = TradeMapper.toEntity(request);
        trade.setStatus(TradeStatus.PENDING);
        Trade saved = tradeRepository.save(trade);
        return TradeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TradeResponseDto> getById(Long id) {
        return tradeRepository.findById(id).map(TradeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeResponseDto> listByUser(Long userId) {
        return tradeRepository.findByUserTradeId(userId)
                .stream().map(TradeMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeResponseDto> listByType(TradeType tradeType) {
        return tradeRepository.findByTradeType(tradeType)
                .stream().map(TradeMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeResponseDto> listByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status)
                .stream().map(TradeMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeResponseDto> listByStockTicker(String stockTicker) {
        return tradeRepository.findByStockTickerIgnoreCase(stockTicker)
                .stream().map(TradeMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeResponseDto> listBetween(LocalDateTime start, LocalDateTime end) {
        return tradeRepository.findByExecutionDateBetween(start, end)
                .stream().map(TradeMapper::toDto).collect(Collectors.toList());
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
    @Transactional
    public TradeResponseDto placeTrade(TradeRequestDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch or create position
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

        // Validate trade
        validateTrade(trade, position);

        // Save trade
        Trade savedTrade = tradeRepository.save(trade);

        // Update the position based on the trade
        updatePosition(position, savedTrade);

        // Update status to executed
        savedTrade.setStatus(TradeStatus.EXECUTED);
        tradeRepository.save(savedTrade);

        return TradeMapper.toDto(savedTrade);
    }

    private void validateTrade(Trade trade, Position position) {
        if (trade.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (trade.getPrice() == null || trade.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        // For SELL trades, check if user has enough shares
        if (trade.getTradeType() == TradeType.SELL) {
            if (position.getTotalQuantity() < trade.getQuantity()) {
                throw new IllegalArgumentException("Insufficient shares to sell");
            }
        }
    }

    private Position createNewPosition(User user, String stockTicker) {
        Position position = new Position();
        position.setUsers(user);
        position.setUserId(user.getId());
        position.setStockTicker(stockTicker);
        position.setAveragePrice(BigDecimal.ZERO);
        position.setTotalQuantity(0.0);
        position.setUnrealizedPnL(0.0);
        return positionRepository.save(position);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeResponseDto> getTradeHistory(Long userId) {
        return tradeRepository.findByUserTradeId(userId)
                .stream()
                .sorted((t1, t2) -> t2.getExecutionDate().compareTo(t1.getExecutionDate()))
                .map(TradeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<?> getUserPositions(Long userId) {
        return positionRepository.findByUserRefId(userId)
                .stream()
                .filter(p -> p.getTotalQuantity() > 0)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NoSuchElementException("Trade not found: " + tradeId));

        if (trade.getStatus() == TradeStatus.EXECUTED) {
            throw new IllegalStateException("Cannot cancel an executed trade");
        }

        trade.setStatus(TradeStatus.CANCELLED);
        tradeRepository.save(trade);
    }

    @Override
    @Transactional
    public void updatePosition(Position position, Trade trade) {
        double qty = trade.getQuantity();
        BigDecimal price = trade.getPrice();

        if (trade.getTradeType() == TradeType.BUY) {
            double oldQty = position.getTotalQuantity() != null ? position.getTotalQuantity() : 0.0;
            BigDecimal oldAvg = position.getAveragePrice() != null ? position.getAveragePrice() : BigDecimal.ZERO;

            double newQty = oldQty + qty;

            // Calculate new average price
            BigDecimal newAvg;
            if (newQty > 0) {
                BigDecimal totalOldValue = oldAvg.multiply(BigDecimal.valueOf(oldQty));
                BigDecimal totalNewValue = price.multiply(BigDecimal.valueOf(qty));
                newAvg = totalOldValue.add(totalNewValue)
                        .divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            } else {
                newAvg = BigDecimal.ZERO;
            }

            position.setTotalQuantity(newQty);
            position.setAveragePrice(newAvg);

        } else if (trade.getTradeType() == TradeType.SELL) {
            double newQty = position.getTotalQuantity() - qty;
            position.setTotalQuantity(Math.max(newQty, 0.0));
            // Average price remains unchanged on SELL
        }

        positionRepository.save(position);
    }
}
