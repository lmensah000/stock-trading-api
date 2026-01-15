# MoneyTeam Stock Trading Platform

A modern, Robinhood-style stock trading platform built with React, FastAPI, and MongoDB.

## 🚀 Features

- **User Authentication**: JWT-based secure authentication
- **Real-time Stock Quotes**: Live market data with demo fallback
- **Interactive Charts**: Multiple time periods (1D, 5D, 1M, 3M, 1Y)
- **Trading**: Buy and sell stocks with real-time execution
- **Portfolio Management**: Track positions, P&L, and market value
- **Watchlist**: Monitor your favorite stocks
- **Fundamental Analysis**: P/E, EPS, market cap, and more
- **Trade History**: Complete transaction records

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Frontend | React 18 + TailwindCSS |
| Backend | FastAPI (Python) |
| Database | MongoDB |
| Market Data | yfinance + TD Schwab API |
| Authentication | JWT |
| Testing | pytest + Jest |

## 📁 Project Structure

```
/app
├── backend/
│   ├── server.py          # FastAPI application
│   ├── requirements.txt   # Python dependencies
│   └── .env              # Environment variables
├── frontend/
│   ├── src/
│   │   ├── App.js        # Main React component
│   │   ├── App.css       # Styles
│   │   ├── App.test.js   # Component tests
│   │   ├── utils.test.js # Utility tests
│   │   └── components.test.js # Component logic tests
│   ├── package.json
│   └── .env
├── tests/
│   ├── backend/          # Backend unit tests
│   │   ├── test_health.py
│   │   ├── test_auth.py
│   │   ├── test_stocks.py
│   │   ├── test_trading.py
│   │   ├── test_portfolio.py
│   │   └── test_watchlist.py
│   ├── integration/      # Integration tests
│   │   ├── test_user_flows.py
│   │   └── test_api_contracts.py
│   └── conftest.py       # Shared fixtures
├── scripts/
│   ├── run_backend_tests.sh
│   ├── run_frontend_tests.sh
│   ├── run_integration_tests.sh
│   └── run_all_tests.sh
├── memory/
│   └── PRD.md            # Product requirements
└── pytest.ini            # Pytest configuration
```

## 🧪 Testing

### Test Coverage

| Category | Tests | Coverage |
|----------|-------|----------|
| Backend Unit Tests | 59 | Health, Auth, Stocks, Trading, Portfolio, Watchlist |
| Integration Tests | 15 | User Flows, API Contracts, Edge Cases |
| Frontend Tests | 25+ | Components, Utilities, Validation |

### Running Tests

```bash
# Run all backend tests
./scripts/run_backend_tests.sh

# Run integration tests
./scripts/run_integration_tests.sh

# Run all tests
./scripts/run_all_tests.sh

# Run frontend tests
./scripts/run_frontend_tests.sh

# Run specific test file
python -m pytest tests/backend/test_auth.py -v

# Run with coverage
python -m pytest tests/ --cov=backend --cov-report=html
```

### Test Categories

#### Backend Unit Tests (`tests/backend/`)

- **test_health.py**: API health endpoints
- **test_auth.py**: User registration, login, JWT validation
- **test_stocks.py**: Stock quotes, fundamentals, charts, market data
- **test_trading.py**: Buy/sell execution, validation, history
- **test_portfolio.py**: Portfolio summary, positions, calculations
- **test_watchlist.py**: Add/remove stocks, validation

#### Integration Tests (`tests/integration/`)

- **test_user_flows.py**: Complete user journeys (register → trade → portfolio)
- **test_api_contracts.py**: API response schema validation

#### Frontend Tests (`frontend/src/*.test.js`)

- **App.test.js**: Component rendering, authentication UI
- **utils.test.js**: Formatting functions, data validation
- **components.test.js**: Component logic, calculations

## 🔧 Setup

### Prerequisites

- Python 3.11+
- Node.js 18+
- MongoDB

### Installation

```bash
# Backend
cd backend
pip install -r requirements.txt

# Frontend
cd frontend
yarn install
```

### Environment Variables

**Backend (.env)**:
```env
MONGO_URL=mongodb://localhost:27017/moneyteam
JWT_SECRET=your-secret-key
JWT_ALGORITHM=HS256
JWT_EXPIRATION_HOURS=24
TD_SCHWAB_CLIENT_ID=your-client-id
TD_SCHWAB_CLIENT_SECRET=your-client-secret
```

**Frontend (.env)**:
```env
REACT_APP_BACKEND_URL=http://localhost:8001
```

### Running the Application

```bash
# Start services
sudo supervisorctl restart all

# Or manually
# Backend
cd backend && uvicorn server:app --host 0.0.0.0 --port 8001 --reload

# Frontend
cd frontend && yarn start
```

## 📊 API Documentation

### Authentication

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Register new user |
| `/api/auth/login` | POST | Login user |
| `/api/auth/me` | GET | Get current user |

### Stocks

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/stocks/quote/{ticker}` | GET | Get stock quote |
| `/api/stocks/fundamentals/{ticker}` | GET | Get fundamentals |
| `/api/stocks/chart/{ticker}` | GET | Get price chart |
| `/api/market/movers` | GET | Get market movers |

### Trading

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/trades` | POST | Execute trade |
| `/api/trades` | GET | Get trade history |

### Portfolio

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/portfolio/summary` | GET | Get portfolio summary |
| `/api/portfolio/positions` | GET | Get all positions |

### Watchlist

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/watchlist` | GET | Get watchlist |
| `/api/watchlist` | POST | Add to watchlist |
| `/api/watchlist/{ticker}` | DELETE | Remove from watchlist |

## 🎨 UI Features

- Dark theme optimized for trading
- Green/red color coding for gains/losses
- Interactive charts with Recharts
- Responsive design
- Real-time notifications

## 📝 License

MIT License

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new features
4. Submit a pull request

---

Built with ❤️ by MoneyTeam
