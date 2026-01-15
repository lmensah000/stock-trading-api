# MoneyTeam Stock Trading Platform

A comprehensive stock trading platform with **dual backend support** (Java Spring Boot & Python FastAPI) and a modern React frontend.

---

## 📁 Complete Project Structure

```
stock-trading-api/
│
├── 📂 java-backend/                    # ☕ ORIGINAL JAVA SPRING BOOT BACKEND
│   ├── pom.xml                         # Maven dependencies
│   └── src/
│       └── main/
│           ├── java/
│           │   ├── com/moneyteam/
│           │   │   ├── controller/
│           │   │   │   ├── StockTradingController.java
│           │   │   │   ├── StockController.java
│           │   │   │   ├── TradeController.java
│           │   │   │   └── UserController.java
│           │   │   │
│           │   │   ├── model/
│           │   │   │   ├── User.java
│           │   │   │   ├── Trade.java
│           │   │   │   ├── Stock.java
│           │   │   │   ├── Position.java
│           │   │   │   ├── Watchlist.java
│           │   │   │   ├── Options.java
│           │   │   │   ├── FundamentalAnalysis.java
│           │   │   │   └── TechnicalAnalysis.java
│           │   │   │
│           │   │   ├── service/
│           │   │   │   ├── ThinkOrSwimClient.java    # TD Schwab API
│           │   │   │   ├── UserService.java
│           │   │   │   ├── TradeService.java
│           │   │   │   ├── StockTradingService.java
│           │   │   │   ├── StockApiService.java
│           │   │   │   ├── stock_data_api.py         # Python helper
│           │   │   │   └── impl/
│           │   │   │       ├── TradeServiceImpl.java
│           │   │   │       └── UserServiceImpl.java
│           │   │   │
│           │   │   ├── repository/
│           │   │   │   ├── UserRepository.java
│           │   │   │   ├── TradeRepository.java
│           │   │   │   ├── StockRepository.java
│           │   │   │   ├── PositionRepository.java
│           │   │   │   └── WatchlistRepository.java
│           │   │   │
│           │   │   ├── dto/
│           │   │   │   ├── TradeRequestDto.java
│           │   │   │   ├── TradeResponseDto.java
│           │   │   │   ├── UserDto.java
│           │   │   │   └── StockDto.java
│           │   │   │
│           │   │   └── config/
│           │   │       ├── SecurityConfig.java
│           │   │       └── AppConfig.java
│           │   │
│           │   └── practice/
│           │       └── StockTradingApplication.java  # Main entry point
│           │
│           └── resources/
│               └── application.properties
│
├── 📂 python-backend/                  # 🐍 NEW PYTHON FASTAPI BACKEND
│   ├── server.py                       # Main FastAPI application (700+ lines)
│   ├── requirements.txt                # Python dependencies
│   └── .env                            # Environment variables
│
├── 📂 frontend/                        # ⚛️ NEW REACT FRONTEND (Robinhood-style)
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── App.js                      # Main React component (1200+ lines)
│   │   ├── App.css                     # Custom styles
│   │   ├── index.js                    # Entry point
│   │   ├── index.css                   # Tailwind imports
│   │   ├── setupTests.js               # Test configuration
│   │   ├── App.test.js                 # Component tests
│   │   ├── utils.test.js               # Utility tests
│   │   └── components.test.js          # Component logic tests
│   ├── package.json
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   └── .env                            # Frontend environment
│
├── 📂 tests/                           # 🧪 TEST SUITES (123 tests)
│   ├── conftest.py                     # Shared pytest fixtures
│   ├── backend/                        # Backend unit tests (59 tests)
│   │   ├── test_health.py
│   │   ├── test_auth.py
│   │   ├── test_stocks.py
│   │   ├── test_trading.py
│   │   ├── test_portfolio.py
│   │   └── test_watchlist.py
│   ├── integration/                    # Integration tests (15 tests)
│   │   ├── test_user_flows.py
│   │   └── test_api_contracts.py
│   └── frontend/                       # Frontend test placeholder
│
├── 📂 scripts/                         # 🔧 UTILITY SCRIPTS
│   ├── run_backend_tests.sh
│   ├── run_frontend_tests.sh
│   ├── run_integration_tests.sh
│   └── run_all_tests.sh
│
├── 📂 memory/
│   └── PRD.md                          # Product Requirements Document
│
├── pytest.ini                          # Pytest configuration
└── README.md                           # This file
```

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React)                         │
│                   Robinhood-style Trading UI                    │
│              localhost:3000 / preview URL                       │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          │ REST API Calls
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BACKEND OPTIONS                              │
│                                                                 │
│  ┌─────────────────────┐       ┌─────────────────────┐         │
│  │   JAVA BACKEND      │       │   PYTHON BACKEND    │         │
│  │   (Spring Boot)     │  OR   │   (FastAPI)         │         │
│  │   Port: 8080        │       │   Port: 8001        │         │
│  │   Database: MySQL   │       │   Database: MongoDB │         │
│  └─────────────────────┘       └─────────────────────┘         │
│                                                                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EXTERNAL SERVICES                            │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  TD Schwab API  │  │    yfinance     │  │   Database      │ │
│  │  (Live Trading) │  │  (Market Data)  │  │ (MySQL/MongoDB) │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Backend Comparison

| Feature | Java Backend | Python Backend |
|---------|--------------|----------------|
| **Framework** | Spring Boot | FastAPI |
| **Language** | Java 17+ | Python 3.11+ |
| **Database** | MySQL | MongoDB |
| **API Style** | REST | REST |
| **Auth** | Spring Security | JWT (python-jose) |
| **TD Schwab** | ThinkOrSwimClient.java | Built-in endpoints |
| **Market Data** | stock_data_api.py | yfinance |
| **Status** | Original | New (Active) |

---

## 🚀 Quick Start

### Option 1: Python Backend (Recommended for this platform)

```bash
# Backend
cd python-backend
pip install -r requirements.txt
uvicorn server:app --host 0.0.0.0 --port 8001 --reload

# Frontend
cd frontend
yarn install
yarn start
```

### Option 2: Java Backend

```bash
# Backend
cd java-backend
mvn spring-boot:run

# Frontend (update .env to point to Java backend)
cd frontend
# Edit .env: REACT_APP_BACKEND_URL=http://localhost:8080
yarn start
```

---

## 🧪 Testing

```bash
# Run all Python backend tests (59 tests)
python -m pytest tests/backend/ -v

# Run integration tests (15 tests)
python -m pytest tests/integration/ -v

# Run frontend tests (49 tests)
cd frontend && npm test

# Run everything
./scripts/run_all_tests.sh
```

**Test Results: 123 tests, 100% passing ✅**

---

## 📊 Features

### Frontend (React)
- ✅ Robinhood-style dark theme UI
- ✅ Real-time stock quotes
- ✅ Interactive price charts
- ✅ Buy/Sell trading interface
- ✅ Portfolio management
- ✅ Watchlist tracking
- ✅ Trade history
- ✅ Fundamental analysis display

### Python Backend (FastAPI)
- ✅ JWT Authentication
- ✅ Stock quotes & charts (yfinance)
- ✅ Trade execution
- ✅ Portfolio tracking
- ✅ Watchlist management
- ✅ TD Schwab OAuth integration ready

### Java Backend (Spring Boot)
- ✅ User management
- ✅ Trade execution
- ✅ Position tracking
- ✅ ThinkOrSwim API client
- ✅ MySQL database support

---

## 🔑 Environment Variables

### Python Backend (`python-backend/.env`)
```env
MONGO_URL=mongodb://localhost:27017/moneyteam
MONGO_DB_NAME=moneyteam
JWT_SECRET=your-secret-key
JWT_ALGORITHM=HS256
JWT_EXPIRATION_HOURS=24
TD_SCHWAB_CLIENT_ID=your-client-id
TD_SCHWAB_CLIENT_SECRET=your-client-secret
```

### Frontend (`frontend/.env`)
```env
REACT_APP_BACKEND_URL=http://localhost:8001
```

### Java Backend (`java-backend/src/main/resources/application.properties`)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/moneyteam
spring.datasource.username=root
spring.datasource.password=password
```

---

## 📝 API Endpoints (Python Backend)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Register user |
| `/api/auth/login` | POST | Login user |
| `/api/stocks/quote/{ticker}` | GET | Get stock quote |
| `/api/stocks/chart/{ticker}` | GET | Get price chart |
| `/api/trades` | POST | Execute trade |
| `/api/trades` | GET | Get trade history |
| `/api/portfolio/summary` | GET | Portfolio summary |
| `/api/portfolio/positions` | GET | All positions |
| `/api/watchlist` | GET/POST/DELETE | Manage watchlist |

---

## 👥 Contributors

- Original Java Backend: MoneyTeam
- Python Backend & Frontend: Built with Emergent AI

---

## 📄 License

MIT License
