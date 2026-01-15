# MoneyTeam Stock Trading Platform

## Product Overview
A modern, Robinhood-style stock trading platform that integrates with TD Schwab's ThinkOrSwim API for real-time market data and trading capabilities.

## Tech Stack
- **Frontend**: React 18 with TailwindCSS
- **Backend**: FastAPI (Python)
- **Database**: MongoDB
- **Market Data**: yfinance + TD Schwab API integration (ready)
- **Authentication**: JWT-based custom authentication

## Testing Status
- **Backend Unit Tests**: 59 tests passing (100%)
- **Integration Tests**: 15 tests passing (100%)  
- **Frontend Tests**: 49 tests passing (100%)
- **Total Tests**: 123 tests

## Features Implemented

### 1. User Authentication
- [x] User registration with email and password
- [x] JWT-based login system
- [x] Session persistence with localStorage
- [x] Secure password hashing with bcrypt

### 2. Dashboard
- [x] Portfolio summary with total value
- [x] Cash balance display
- [x] Total gain/loss (P&L) with percentage
- [x] Position count
- [x] Stock search functionality
- [x] Market movers display

### 3. Stock Data & Analysis
- [x] Real-time stock quotes (with demo fallback)
- [x] Interactive price charts (1D, 5D, 1M, 3M, 1Y)
- [x] Fundamental analysis data:
  - P/E Ratio, P/B Ratio
  - EPS, Market Cap
  - Dividend Yield, Beta
  - Profit Margin, ROE
- [x] Key statistics display

### 4. Trading
- [x] Buy stocks
- [x] Sell stocks
- [x] Real-time price quotes during trade
- [x] Trade confirmation
- [x] Position management (auto-update on trades)
- [x] Cash balance management

### 5. Portfolio Management
- [x] View all positions
- [x] Per-position P&L tracking
- [x] Current price updates
- [x] Market value calculation
- [x] Average cost basis tracking

### 6. Watchlist
- [x] Add stocks to watchlist
- [x] Remove stocks from watchlist
- [x] Real-time watchlist price updates
- [x] Quick access to stock details

### 7. Trade History
- [x] View all executed trades
- [x] Trade status tracking
- [x] Trade details (price, quantity, total)

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/me` - Get current user

### Stocks
- `GET /api/stocks/quote/{ticker}` - Get stock quote
- `GET /api/stocks/fundamentals/{ticker}` - Get fundamental data
- `GET /api/stocks/chart/{ticker}` - Get price chart data
- `GET /api/stocks/search?q={query}` - Search stocks

### Trading
- `POST /api/trades` - Execute trade
- `GET /api/trades` - Get trade history
- `DELETE /api/trades/{trade_id}` - Cancel trade

### Portfolio
- `GET /api/portfolio/positions` - Get all positions
- `GET /api/portfolio/summary` - Get portfolio summary

### Watchlist
- `GET /api/watchlist` - Get user's watchlist
- `POST /api/watchlist` - Add to watchlist
- `DELETE /api/watchlist/{ticker}` - Remove from watchlist

### Market
- `GET /api/market/movers` - Get top market movers
- `GET /api/market/indices` - Get market indices

### TD Schwab Integration
- `GET /api/schwab/auth-url` - Get OAuth URL
- `POST /api/schwab/authenticate` - Authenticate with TD Schwab

## Environment Variables

### Backend (.env)
```
MONGO_URL=mongodb://localhost:27017/moneyteam
JWT_SECRET=your-super-secret-jwt-key
JWT_ALGORITHM=HS256
JWT_EXPIRATION_HOURS=24
TD_SCHWAB_CLIENT_ID=your-client-id
TD_SCHWAB_CLIENT_SECRET=your-client-secret
TD_SCHWAB_REDIRECT_URI=http://localhost:3000/callback
```

### Frontend (.env)
```
REACT_APP_BACKEND_URL=https://demobackend.emergentagent.com
```

## Demo Mode
When the yfinance API is rate-limited, the application falls back to demo stock data for the following tickers:
- AAPL, MSFT, GOOGL, AMZN, NVDA, TSLA, META, JPM, V, WMT

## TD Schwab Integration (Ready for Configuration)
The application includes TD Schwab ThinkOrSwim API integration endpoints. To enable live trading:
1. Register for a TD Schwab developer account
2. Create an application and get API credentials
3. Add credentials to backend .env file
4. Users can link their TD Schwab accounts via OAuth flow

## Design Philosophy
- **Robinhood-inspired UI**: Clean, minimalist design with green/red indicators
- **Dark theme**: Easy on the eyes for extended trading sessions
- **Responsive**: Works on all screen sizes
- **Real-time**: Updates portfolio and prices automatically

## Starting Cash
New users start with $100,000 virtual cash balance for paper trading.
