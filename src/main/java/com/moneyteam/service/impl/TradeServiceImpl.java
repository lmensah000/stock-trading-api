package com.moneyteam.service.impl;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.mapper.TradeMapper;
import com.moneyteam.model.Trade;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.repository.OptionsTradeRepository;
import com.moneyteam.repository.StockTradeRepository;
import com.moneyteam.repository.TradeRepository;
import com.moneyteam.service.TradeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

//private static final Logger log = LoggerFactory.getLogger(TradeServiceImpl.class);

@Service
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final StockTradeRepository stockTradeRepository;
    private final OptionsTradeRepository optionsTradeRepository;

    public TradeServiceImpl(
            TradeRepository tradeRepository,
            StockTradeRepository stockTradeRepository,
            OptionsTradeRepository optionsTradeRepository
    ) {
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
            Trade saved = tradeRepository.save(TradeMapper.toEntity(request));
            return TradeMapper.toDto(saved);
        }

        @Override
        public Optional<TradeResponseDto> getById(Long id) {
            return tradeRepository.findById(id).map(TradeMapper::toDto);
        }

        @Override
        public List<TradeResponseDto> listByUser(Long userId) {
            return tradeRepository.findByUserId(userId)
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
        public List<TradeResponseDto> listBySymbol(String symbol) {
            return tradeRepository.findBySymbolIgnoreCase(symbol)
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
}