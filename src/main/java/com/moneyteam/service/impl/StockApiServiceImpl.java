package com.moneyteam.service.impl;

import com.moneyteam.model.Stock;
import com.moneyteam.service.StockApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class StockApiServiceImpl extends StockApiService {

    private static final Logger logger = LoggerFactory.getLogger(StockApiServiceImpl.class);
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL = "https://query1.finance.yahoo.com/v7/finance/quote?stockTickers=";

    @Override
    public Optional<Stock> fetchStockDetails(String stockTicker) {
        try {
            String url = API_URL + stockTicker;
            logger.info("Fetching stock data from: {}", url);

            // In production, map JSON to your Stock DTO
            Stock stock = restTemplate.getForObject(url, Stock.class);
            return Optional.ofNullable(stock);
        } catch (Exception e) {
            logger.error("Failed to fetch stock data for stockTicker {}: {}", stockTicker, e.getMessage());
            return Optional.empty();
        }
    }
}