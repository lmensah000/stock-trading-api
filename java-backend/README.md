# Java Spring Boot Backend

Original Java backend for the MoneyTeam Stock Trading Platform.

## 📁 Structure

```
java-backend/
├── pom.xml                              # Maven configuration
└── src/main/
    ├── java/com/moneyteam/
    │   ├── controller/                  # REST Controllers
    │   │   ├── StockTradingController.java
    │   │   ├── StockController.java
    │   │   ├── TradeController.java
    │   │   └── UserController.java
    │   │
    │   ├── model/                       # Entity Models
    │   │   ├── User.java
    │   │   ├── Trade.java
    │   │   ├── Stock.java
    │   │   ├── Position.java
    │   │   ├── Watchlist.java
    │   │   ├── Options.java
    │   │   ├── FundamentalAnalysis.java
    │   │   └── TechnicalAnalysis.java
    │   │
    │   ├── service/                     # Business Logic
    │   │   ├── ThinkOrSwimClient.java   # TD Schwab API Integration
    │   │   ├── UserService.java
    │   │   ├── TradeService.java
    │   │   ├── StockTradingService.java
    │   │   ├── StockApiService.java
    │   │   ├── stock_data_api.py        # Python helper for yfinance
    │   │   └── impl/
    │   │       ├── TradeServiceImpl.java
    │   │       └── UserServiceImpl.java
    │   │
    │   ├── repository/                  # Data Access Layer
    │   │   ├── UserRepository.java
    │   │   ├── TradeRepository.java
    │   │   ├── StockRepository.java
    │   │   ├── PositionRepository.java
    │   │   └── WatchlistRepository.java
    │   │
    │   ├── dto/                         # Data Transfer Objects
    │   │   ├── TradeRequestDto.java
    │   │   ├── TradeResponseDto.java
    │   │   ├── UserDto.java
    │   │   └── StockDto.java
    │   │
    │   └── config/                      # Configuration
    │       ├── SecurityConfig.java
    │       └── AppConfig.java
    │
    └── resources/
        └── application.properties       # Application config

```

## 🚀 Running

```bash
# Prerequisites
# - Java 17+
# - Maven 3.8+
# - MySQL 8.0+

# Build
mvn clean install

# Run
mvn spring-boot:run
```

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/moneyteam
spring.datasource.username=root
spring.datasource.password=your_password

# TD Schwab API
td.schwab.client-id=your_client_id
td.schwab.client-secret=your_client_secret
td.schwab.redirect-uri=http://localhost:8080/callback
```

## 🔗 TD Schwab Integration

The `ThinkOrSwimClient.java` handles OAuth2 authentication with TD Schwab:

```java
// Get authorization URL
String authUrl = thinkOrSwimClient.getAuthorizationUrl();

// Exchange code for tokens
TokenResponse tokens = thinkOrSwimClient.exchangeCodeForTokens(authCode);

// Get account data
AccountInfo account = thinkOrSwimClient.getAccountInfo(accessToken);
```

## 📊 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/users` | GET/POST | User management |
| `/api/trades` | GET/POST | Trade operations |
| `/api/stocks` | GET | Stock data |
| `/api/positions` | GET | User positions |
| `/api/watchlist` | GET/POST/DELETE | Watchlist |

## 🗄️ Database Schema

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    password VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE trades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    stock_ticker VARCHAR(10),
    quantity DECIMAL(10,2),
    price DECIMAL(10,2),
    trade_type ENUM('BUY', 'SELL'),
    status VARCHAR(20),
    execution_date TIMESTAMP
);

CREATE TABLE positions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    stock_ticker VARCHAR(10),
    total_quantity DECIMAL(10,2),
    average_price DECIMAL(10,2)
);
```

## 🔄 Switching from Python Backend

To use this Java backend with the React frontend:

1. Start the Java backend on port 8080
2. Update `frontend/.env`:
   ```env
   REACT_APP_BACKEND_URL=http://localhost:8080
   ```
3. Restart the frontend

Note: API endpoint paths may need adjustment to match Java controller mappings.
