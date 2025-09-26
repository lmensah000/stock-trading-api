package com.moneyteam.service.impl;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.mapper.TradeMapper;
import com.moneyteam.model.Stock;
import com.moneyteam.model.TradeType;
import com.moneyteam.repository.OptionsTradeRepository;
import com.moneyteam.repository.StockTradeRepository;
import com.moneyteam.service.TradeService;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TradeServiceImpl implements TradeService {

//    private final StockTradeRepository stockTradeRepository;

    private final TradeRepository tradeRepository;
    private final StockTradeRepository stockTradeRepository;
    private final OptionsTradeRepository optionsTradeRepository;

    public TradeServiceImpl(TradeRepository) {
        this.tradeRepository = TradeRepository;

    }

    public TradeServiceImpl(StockTradeRepository) {
        this.tradeRepository = StockTradeRepository;

    }

    public TradeServiceImpl(OptionsTradeRepository) {
        this.optionsTradeRepository = optionsTradeRepository;

    }

    @Override
    @Transactional
    public TradeResponseDto create(TradeRequestDto request) {
        Trade saved = tradeRepository.save(TradeMapper.toEntity(request));
        return TradeMapper.toDto(saved);
    }

    @Override
    public Optional<TradeResponseDto> getById(Long id) {
        return tradeRepository.findById(id).map(TradeMapper::toDto);
    }

    @Override
    public List<TradeRequestDto> listByUser(Long userId) {
        return tradeRepository.findByUserId(userId).stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listByType(TradeType type) {
        return tradeRepository.findByTradeType(type).stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listBySymbol(String symbol) {
        return tradeRepository.findBySymbol(symbol).stream().map(TradeMapper::toDto).toList();
    }

    @Override
    public List<TradeResponseDto> listBetween(LocalDateTime start, LocalDateTime end) {
        return tradeRepository.findByExectionDateBetween(start, end).stream().map(TradeMapper::toDto).toList();
    }

    @Transactional
    public TradeResponseDto updateStatus(Long id, TradeStatus newStatus) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trade not found: " + id));
        trade.setStatus(newStatus);
        return TradeMapper.toDto(tradeRepository.save(trade));
    }
}
