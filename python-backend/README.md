# Python FastAPI Backend

Modern Python backend for the MoneyTeam Stock Trading Platform.

## 📁 Structure

```
python-backend/
├── server.py           # Main FastAPI application (700+ lines)
├── requirements.txt    # Python dependencies
└── .env               # Environment variables
```

## 🚀 Running

```bash
# Install dependencies
pip install -r requirements.txt

# Run development server
uvicorn server:app --host 0.0.0.0 --port 8001 --reload

# Or with Python directly
python server.py
```

## ⚙️ Configuration

Edit `.env`:

```env
MONGO_URL=mongodb://localhost:27017/moneyteam
MONGO_DB_NAME=moneyteam
JWT_SECRET=your-super-secret-key
JWT_ALGORITHM=HS256
JWT_EXPIRATION_HOURS=24
TD_SCHWAB_CLIENT_ID=your_client_id
TD_SCHWAB_CLIENT_SECRET=your_client_secret
TD_SCHWAB_REDIRECT_URI=http://localhost:3000/callback
```

## 📊 API Endpoints

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
| `/api/stocks/search` | GET | Search stocks |
| `/api/market/movers` | GET | Get market movers |

### Trading
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/trades` | POST | Execute trade |
| `/api/trades` | GET | Get trade history |
| `/api/trades/{id}` | DELETE | Cancel trade |

### Portfolio
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/portfolio/summary` | GET | Portfolio summary |
| `/api/portfolio/positions` | GET | All positions |

### Watchlist
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/watchlist` | GET | Get watchlist |
| `/api/watchlist` | POST | Add to watchlist |
| `/api/watchlist/{ticker}` | DELETE | Remove from watchlist |

### TD Schwab
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/schwab/auth-url` | GET | Get OAuth URL |
| `/api/schwab/authenticate` | POST | Complete OAuth |

## 🧪 Testing

```bash
# Run all backend tests
cd /app
python -m pytest tests/backend/ -v

# Run specific test file
python -m pytest tests/backend/test_auth.py -v

# Run with coverage
python -m pytest tests/backend/ --cov=python-backend
```

## 📦 Dependencies

- **fastapi** - Web framework
- **uvicorn** - ASGI server
- **motor** - Async MongoDB driver
- **python-jose** - JWT handling
- **passlib** - Password hashing
- **yfinance** - Stock market data
- **httpx** - Async HTTP client
- **pydantic** - Data validation

## 🔒 Security Features

- JWT-based authentication
- Password hashing with bcrypt
- CORS configured for frontend
- Environment variable configuration
- Input validation with Pydantic

## 📈 Market Data

Uses **yfinance** for real-time stock data with demo fallback:

```python
DEMO_STOCKS = ["AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", 
               "TSLA", "META", "JPM", "V", "WMT"]
```

When yfinance is rate-limited, the API returns demo data for these tickers.
