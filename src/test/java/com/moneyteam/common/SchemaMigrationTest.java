package com.moneyteam.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The authoritative check on Phase 3: that V1 applies to a real MySQL and that
 * Hibernate's validate can then reconcile every entity mapping against it.
 *
 * Reading a migration against the entities by eye is not the same thing. This
 * is what actually proves the schema and the entities agree - it would have
 * caught the stockTicker / stock_ticker drift that went unnoticed for months
 * because ddl-auto was switched off.
 *
 * Uses real MySQL rather than H2 deliberately: H2 does not reproduce MySQL's
 * ENUM columns, FK semantics or dialect behaviour, so it can pass while
 * production fails.
 *
 * Named ...Test rather than ...IT so Surefire actually picks it up: without
 * the Failsafe plugin configured, an *IT class is silently never run, which is
 * worse than a test that visibly skips. It can move to Failsafe when CI exists.
 *
 * disabledWithoutDocker means this skips - rather than fails - where Docker is
 * unavailable, which includes the environment this was written in. It runs
 * unchanged once CI provides Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "app.jwt.secret=test-only-secret-at-least-32-bytes-long!"
})
@ActiveProfiles("test")
@DisplayName("Flyway V1 applies and Hibernate validates every entity against it")
class SchemaMigrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("moneyteamdb");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private DataSource dataSource;

    /**
     * Reaching this method at all is the assertion: the context only starts if
     * Flyway applied V1 and ddl-auto=validate reconciled every @Column,
     * @JoinColumn and @Index against the resulting schema.
     */
    @Test
    @DisplayName("the context starts, so migration and validation both succeeded")
    void contextLoads() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("V1 created every table the entities map")
    void everyMappedTableExists() throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }

        assertThat(tables).contains(
                "users", "stocks", "stock_historical_data", "accounts", "positions",
                "trades", "ledger_entries", "orders", "order_executions",
                "options", "option_trade_details", "watchlists", "watchlist_stocks");
    }

    @Test
    @DisplayName("ticker columns are snake_case everywhere, with no camelCase survivor")
    void tickerColumnsAreSnakeCase() throws Exception {
        Set<String> tickerColumns = new HashSet<>();
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, "%", "%ticker%")) {
            while (rs.next()) {
                tickerColumns.add(rs.getString("COLUMN_NAME"));
            }
        }

        assertThat(tickerColumns).isNotEmpty();
        assertThat(tickerColumns)
                .as("the reconciliation removed every camelCase ticker column")
                .allSatisfy(name -> assertThat(name).isEqualTo("stock_ticker"));
    }
}
