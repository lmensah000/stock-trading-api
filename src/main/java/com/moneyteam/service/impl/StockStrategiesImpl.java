package com.moneyteam.service.impl;

import com.moneyteam.model.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//MarketDataService: This service class encapsulates the logic for retrieving and processing market data.
// It interacts with the MarketData model class to fetch data and provides methods for the Controller to access and utilize the data.

//PortfolioService: This service class encapsulates the logic for managing the bot's portfolio.
//It interacts with the Portfolio model class to perform operations such as buying or selling assets,
//calculating performance metrics, and managing available funds.

//TradingService: This service class encapsulates the logic for executing trading strategies.
//It interacts with the TradingStrategy model class to make trading decisions based on market data and portfolio information.


//trading strategy logic based on stock, crypto
public class StockStrategiesImpl {
    private User user;
    private TechnicalAnalysis technicalAnalysis;
    private FundamentalAnalysis fundamentalAnalysis;
    private Stock stock;
    private RiskManagement riskManagement;
    private Crypto crypto;

    public Stock getStockData(String stockTicker) {

        //Map<String a ,List<Stock> stock;

        return stock;
    }
    public static Object[] getStockProperties(Object stock, String[] propertyNames) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object[] rowData = new Object[propertyNames.length];
        Class<?> Stock = stock.getClass();

        for (int i = 0; i < propertyNames.length; i++) {
            String propertyName = propertyNames[i];
            String getterMethodName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            Method getterMethod = Stock.getMethod(getterMethodName);

            Object propertyValue = getterMethod.invoke(stock);
            rowData[i] = propertyValue;
        }

        return rowData;
    }

//    private void getStockReport(Map<String, Object>){
//
//        String[] propertyName;
//        System.out.println(getStockProperties(stock, propertyName));
//    }

    public static boolean shouldBuy(String stockTicker) {
        // Replace this with your actual trading strategy logic
        double stockPrice = Stock.getLast(stockTicker);

        // Simple moving average strategy: Buy if the price is above a threshold
        return stockPrice > 50;
    }

    public static boolean shouldSell(String stockTicker) {
        // Replace this with your actual trading strategy logic
        double stockPrice = Stock.getLast(stockTicker);

        // Simple moving average strategy: Sell if the price is below a threshold
        return stockPrice < 40;
    }
}
