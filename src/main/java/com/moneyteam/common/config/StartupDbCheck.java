package com.moneyteam.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class StartupDbCheck {

    private static final Logger log = LoggerFactory.getLogger(StartupDbCheck.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void logConnectionInfo() {
        try (Connection conn = dataSource.getConnection()) {
            log.info("Connected as: {}", conn.getMetaData().getUserName());
            log.info("DB URL: {}", conn.getMetaData().getURL());
        } catch (SQLException e) {
            log.error("Database connection check failed: {}", e.getMessage());
        }
    }
}
