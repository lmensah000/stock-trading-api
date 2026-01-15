from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field, EmailStr
from typing import Optional, List
from datetime import datetime, timedelta
from jose import JWTError, jwt
from passlib.context import CryptContext
from motor.motor_asyncio import AsyncIOMotorClient
from bson import ObjectId
import os
import uuid
import yfinance as yf
import httpx
from dotenv import load_dotenv

load_dotenv()

# App initialization
app = FastAPI(title="MoneyTeam Stock Trading API", version="2.0")

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Database connection
MONGO_URL = os.environ.get("MONGO_URL", "mongodb://localhost:27017")
client = AsyncIOMotorClient(MONGO_URL)
db = client.moneyteam

# JWT Configuration
JWT_SECRET = os.environ.get("JWT_SECRET", "your-super-secret-jwt-key")
JWT_ALGORITHM = os.environ.get("JWT_ALGORITHM", "HS256")
JWT_EXPIRATION_HOURS = int(os.environ.get("JWT_EXPIRATION_HOURS", "24"))

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
security = HTTPBearer()

# ==================== MODELS ====================

class UserRegistration(BaseModel):
    username: str = Field(..., min_length=3, max_length=50)
    password: str = Field(..., min_length=6)
    email: EmailStr

class UserLogin(BaseModel):
    username: str
    password: str

class UserResponse(BaseModel):
    id: str
    username: str
    email: str
    created_at: datetime

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

class TradeRequest(BaseModel):
    stock_ticker: str = Field(..., min_length=1, max_length=10)
    quantity: float = Field(..., gt=0)
    price: float = Field(..., gt=0)
    trade_type: str = Field(..., pattern="^(BUY|SELL)$")

class TradeResponse(BaseModel):
    id: str
    user_id: str
    stock_ticker: str
    quantity: float
    price: float
    trade_type: str
    status: str
    execution_date: datetime
    total_value: float

class PositionResponse(BaseModel):
    id: str
    user_id: str
    stock_ticker: str
    total_quantity: float
    average_price: float
    current_price: Optional[float] = None
    market_value: Optional[float] = None
    unrealized_pnl: Optional[float] = None
    unrealized_pnl_percent: Optional[float] = None

class WatchlistItem(BaseModel):
    stock_ticker: str

class WatchlistResponse(BaseModel):
    id: str
    user_id: str
    stocks: List[str]

class StockQuote(BaseModel):
    ticker: str
    name: str
    price: float
    change: float
    change_percent: float
    volume: int
    market_cap: Optional[float] = None
    pe_ratio: Optional[float] = None
    dividend_yield: Optional[float] = None
    high_52_week: Optional[float] = None
    low_52_week: Optional[float] = None
    open_price: Optional[float] = None
    previous_close: Optional[float] = None
    day_high: Optional[float] = None
    day_low: Optional[float] = None

class FundamentalData(BaseModel):
    ticker: str
    name: str
    sector: Optional[str] = None
    industry: Optional[str] = None
    market_cap: Optional[float] = None
    pe_ratio: Optional[float] = None
    pb_ratio: Optional[float] = None
    eps: Optional[float] = None
    dividend_yield: Optional[float] = None
    beta: Optional[float] = None
    revenue: Optional[float] = None
    net_income: Optional[float] = None
    total_assets: Optional[float] = None
    total_liabilities: Optional[float] = None
    profit_margin: Optional[float] = None
    roe: Optional[float] = None

class PortfolioSummary(BaseModel):
    total_value: float
    total_cost: float
    total_gain_loss: float
    total_gain_loss_percent: float
    positions_count: int
    cash_balance: float

class TDSchwabAuthRequest(BaseModel):
    auth_code: str

# ==================== HELPER FUNCTIONS ====================

def create_access_token(user_id: str, username: str) -> str:
    expire = datetime.utcnow() + timedelta(hours=JWT_EXPIRATION_HOURS)
    to_encode = {
        "sub": user_id,
        "username": username,
        "exp": expire
    }
    return jwt.encode(to_encode, JWT_SECRET, algorithm=JWT_ALGORITHM)

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        user_id = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
        user = await db.users.find_one({"_id": user_id})
        if user is None:
            raise HTTPException(status_code=401, detail="User not found")
        return user
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

def format_dollar(value):
    if value is None:
        return None
    return round(value, 2)

# ==================== AUTH ENDPOINTS ====================

@app.post("/api/auth/register", response_model=TokenResponse)
async def register(user_data: UserRegistration):
    # Check if user already exists
    existing_user = await db.users.find_one({
        "$or": [
            {"username": user_data.username},
            {"email": user_data.email}
        ]
    })
    if existing_user:
        raise HTTPException(status_code=400, detail="Username or email already registered")
    
    # Create new user
    user_id = str(uuid.uuid4())
    hashed_password = pwd_context.hash(user_data.password)
    
    new_user = {
        "_id": user_id,
        "username": user_data.username,
        "email": user_data.email,
        "password": hashed_password,
        "created_at": datetime.utcnow(),
        "cash_balance": 100000.0  # Starting balance
    }
    
    await db.users.insert_one(new_user)
    
    # Create empty watchlist
    await db.watchlists.insert_one({
        "_id": str(uuid.uuid4()),
        "user_id": user_id,
        "stocks": []
    })
    
    access_token = create_access_token(user_id, user_data.username)
    
    return TokenResponse(
        access_token=access_token,
        user=UserResponse(
            id=user_id,
            username=user_data.username,
            email=user_data.email,
            created_at=new_user["created_at"]
        )
    )

@app.post("/api/auth/login", response_model=TokenResponse)
async def login(credentials: UserLogin):
    user = await db.users.find_one({"username": credentials.username})
    if not user or not pwd_context.verify(credentials.password, user["password"]):
        raise HTTPException(status_code=401, detail="Invalid username or password")
    
    access_token = create_access_token(user["_id"], user["username"])
    
    return TokenResponse(
        access_token=access_token,
        user=UserResponse(
            id=user["_id"],
            username=user["username"],
            email=user["email"],
            created_at=user["created_at"]
        )
    )

@app.get("/api/auth/me", response_model=UserResponse)
async def get_me(current_user: dict = Depends(get_current_user)):
    return UserResponse(
        id=current_user["_id"],
        username=current_user["username"],
        email=current_user["email"],
        created_at=current_user["created_at"]
    )

# ==================== STOCK DATA ENDPOINTS ====================

@app.get("/api/stocks/quote/{ticker}", response_model=StockQuote)
async def get_stock_quote(ticker: str):
    try:
        stock = yf.Ticker(ticker.upper())
        info = stock.info
        
        if not info or "regularMarketPrice" not in info:
            raise HTTPException(status_code=404, detail=f"Stock {ticker} not found")
        
        current_price = info.get("regularMarketPrice", info.get("currentPrice", 0))
        previous_close = info.get("previousClose", info.get("regularMarketPreviousClose", 0))
        change = current_price - previous_close if previous_close else 0
        change_percent = (change / previous_close * 100) if previous_close else 0
        
        return StockQuote(
            ticker=ticker.upper(),
            name=info.get("shortName", info.get("longName", ticker)),
            price=format_dollar(current_price),
            change=format_dollar(change),
            change_percent=round(change_percent, 2),
            volume=info.get("volume", 0),
            market_cap=info.get("marketCap"),
            pe_ratio=info.get("trailingPE"),
            dividend_yield=info.get("dividendYield"),
            high_52_week=info.get("fiftyTwoWeekHigh"),
            low_52_week=info.get("fiftyTwoWeekLow"),
            open_price=info.get("open", info.get("regularMarketOpen")),
            previous_close=previous_close,
            day_high=info.get("dayHigh", info.get("regularMarketDayHigh")),
            day_low=info.get("dayLow", info.get("regularMarketDayLow"))
        )
    except Exception as e:
        raise HTTPException(status_code=404, detail=f"Error fetching stock data: {str(e)}")

@app.get("/api/stocks/fundamentals/{ticker}", response_model=FundamentalData)
async def get_fundamentals(ticker: str):
    try:
        stock = yf.Ticker(ticker.upper())
        info = stock.info
        
        if not info:
            raise HTTPException(status_code=404, detail=f"Stock {ticker} not found")
        
        # Get financial data
        try:
            financials = stock.financials
            balance_sheet = stock.balance_sheet
            revenue = financials.loc["Total Revenue"].iloc[0] if "Total Revenue" in financials.index else None
            net_income = financials.loc["Net Income"].iloc[0] if "Net Income" in financials.index else None
            total_assets = balance_sheet.loc["Total Assets"].iloc[0] if "Total Assets" in balance_sheet.index else None
            total_liabilities = balance_sheet.loc["Total Liabilities Net Minority Interest"].iloc[0] if "Total Liabilities Net Minority Interest" in balance_sheet.index else None
        except:
            revenue = net_income = total_assets = total_liabilities = None
        
        return FundamentalData(
            ticker=ticker.upper(),
            name=info.get("shortName", info.get("longName", ticker)),
            sector=info.get("sector"),
            industry=info.get("industry"),
            market_cap=info.get("marketCap"),
            pe_ratio=info.get("trailingPE"),
            pb_ratio=info.get("priceToBook"),
            eps=info.get("trailingEps"),
            dividend_yield=info.get("dividendYield"),
            beta=info.get("beta"),
            revenue=revenue,
            net_income=net_income,
            total_assets=total_assets,
            total_liabilities=total_liabilities,
            profit_margin=info.get("profitMargins"),
            roe=info.get("returnOnEquity")
        )
    except Exception as e:
        raise HTTPException(status_code=404, detail=f"Error fetching fundamental data: {str(e)}")

@app.get("/api/stocks/chart/{ticker}")
async def get_stock_chart(ticker: str, period: str = "1mo", interval: str = "1d"):
    try:
        stock = yf.Ticker(ticker.upper())
        hist = stock.history(period=period, interval=interval)
        
        if hist.empty:
            raise HTTPException(status_code=404, detail=f"No chart data for {ticker}")
        
        chart_data = []
        for index, row in hist.iterrows():
            chart_data.append({
                "date": index.strftime("%Y-%m-%d %H:%M"),
                "open": round(row["Open"], 2),
                "high": round(row["High"], 2),
                "low": round(row["Low"], 2),
                "close": round(row["Close"], 2),
                "volume": int(row["Volume"])
            })
        
        return {"ticker": ticker.upper(), "data": chart_data}
    except Exception as e:
        raise HTTPException(status_code=404, detail=f"Error fetching chart data: {str(e)}")

@app.get("/api/stocks/search")
async def search_stocks(q: str):
    try:
        # Search using yfinance
        tickers = yf.Tickers(q.upper())
        results = []
        
        # Try to get info for the ticker
        try:
            stock = yf.Ticker(q.upper())
            info = stock.info
            if info and "shortName" in info:
                results.append({
                    "ticker": q.upper(),
                    "name": info.get("shortName", q.upper()),
                    "exchange": info.get("exchange", "")
                })
        except:
            pass
        
        return {"results": results}
    except Exception as e:
        return {"results": []}

# ==================== TRADING ENDPOINTS ====================

@app.post("/api/trades", response_model=TradeResponse)
async def execute_trade(trade: TradeRequest, current_user: dict = Depends(get_current_user)):
    user_id = current_user["_id"]
    total_value = trade.quantity * trade.price
    
    # Check for BUY - sufficient balance
    if trade.trade_type == "BUY":
        if current_user.get("cash_balance", 0) < total_value:
            raise HTTPException(status_code=400, detail="Insufficient funds")
    
    # Check for SELL - sufficient position
    if trade.trade_type == "SELL":
        position = await db.positions.find_one({
            "user_id": user_id,
            "stock_ticker": trade.stock_ticker.upper()
        })
        if not position or position.get("total_quantity", 0) < trade.quantity:
            raise HTTPException(status_code=400, detail="Insufficient shares to sell")
    
    # Create trade record
    trade_id = str(uuid.uuid4())
    trade_record = {
        "_id": trade_id,
        "user_id": user_id,
        "stock_ticker": trade.stock_ticker.upper(),
        "quantity": trade.quantity,
        "price": trade.price,
        "trade_type": trade.trade_type,
        "status": "EXECUTED",
        "execution_date": datetime.utcnow(),
        "total_value": total_value
    }
    
    await db.trades.insert_one(trade_record)
    
    # Update position
    position = await db.positions.find_one({
        "user_id": user_id,
        "stock_ticker": trade.stock_ticker.upper()
    })
    
    if trade.trade_type == "BUY":
        if position:
            # Update existing position
            old_qty = position["total_quantity"]
            old_avg = position["average_price"]
            new_qty = old_qty + trade.quantity
            new_avg = ((old_avg * old_qty) + (trade.price * trade.quantity)) / new_qty
            
            await db.positions.update_one(
                {"_id": position["_id"]},
                {"$set": {"total_quantity": new_qty, "average_price": new_avg}}
            )
        else:
            # Create new position
            await db.positions.insert_one({
                "_id": str(uuid.uuid4()),
                "user_id": user_id,
                "stock_ticker": trade.stock_ticker.upper(),
                "total_quantity": trade.quantity,
                "average_price": trade.price
            })
        
        # Deduct cash balance
        await db.users.update_one(
            {"_id": user_id},
            {"$inc": {"cash_balance": -total_value}}
        )
    else:  # SELL
        new_qty = position["total_quantity"] - trade.quantity
        if new_qty <= 0:
            await db.positions.delete_one({"_id": position["_id"]})
        else:
            await db.positions.update_one(
                {"_id": position["_id"]},
                {"$set": {"total_quantity": new_qty}}
            )
        
        # Add cash balance
        await db.users.update_one(
            {"_id": user_id},
            {"$inc": {"cash_balance": total_value}}
        )
    
    return TradeResponse(**trade_record)

@app.get("/api/trades", response_model=List[TradeResponse])
async def get_trades(current_user: dict = Depends(get_current_user)):
    trades = await db.trades.find({"user_id": current_user["_id"]}).sort("execution_date", -1).to_list(100)
    return [TradeResponse(**trade) for trade in trades]

@app.delete("/api/trades/{trade_id}")
async def cancel_trade(trade_id: str, current_user: dict = Depends(get_current_user)):
    trade = await db.trades.find_one({"_id": trade_id, "user_id": current_user["_id"]})
    if not trade:
        raise HTTPException(status_code=404, detail="Trade not found")
    
    if trade["status"] != "PENDING":
        raise HTTPException(status_code=400, detail="Only pending trades can be cancelled")
    
    await db.trades.update_one(
        {"_id": trade_id},
        {"$set": {"status": "CANCELLED"}}
    )
    
    return {"message": "Trade cancelled successfully"}

# ==================== PORTFOLIO ENDPOINTS ====================

@app.get("/api/portfolio/positions", response_model=List[PositionResponse])
async def get_positions(current_user: dict = Depends(get_current_user)):
    positions = await db.positions.find({"user_id": current_user["_id"]}).to_list(100)
    
    result = []
    for pos in positions:
        try:
            stock = yf.Ticker(pos["stock_ticker"])
            info = stock.info
            current_price = info.get("regularMarketPrice", info.get("currentPrice", pos["average_price"]))
            market_value = pos["total_quantity"] * current_price
            cost_basis = pos["total_quantity"] * pos["average_price"]
            unrealized_pnl = market_value - cost_basis
            unrealized_pnl_percent = (unrealized_pnl / cost_basis * 100) if cost_basis > 0 else 0
        except:
            current_price = pos["average_price"]
            market_value = pos["total_quantity"] * current_price
            unrealized_pnl = 0
            unrealized_pnl_percent = 0
        
        result.append(PositionResponse(
            id=pos["_id"],
            user_id=pos["user_id"],
            stock_ticker=pos["stock_ticker"],
            total_quantity=pos["total_quantity"],
            average_price=round(pos["average_price"], 2),
            current_price=round(current_price, 2),
            market_value=round(market_value, 2),
            unrealized_pnl=round(unrealized_pnl, 2),
            unrealized_pnl_percent=round(unrealized_pnl_percent, 2)
        ))
    
    return result

@app.get("/api/portfolio/summary", response_model=PortfolioSummary)
async def get_portfolio_summary(current_user: dict = Depends(get_current_user)):
    positions = await db.positions.find({"user_id": current_user["_id"]}).to_list(100)
    
    total_value = 0
    total_cost = 0
    
    for pos in positions:
        try:
            stock = yf.Ticker(pos["stock_ticker"])
            info = stock.info
            current_price = info.get("regularMarketPrice", info.get("currentPrice", pos["average_price"]))
        except:
            current_price = pos["average_price"]
        
        total_value += pos["total_quantity"] * current_price
        total_cost += pos["total_quantity"] * pos["average_price"]
    
    cash_balance = current_user.get("cash_balance", 0)
    total_gain_loss = total_value - total_cost
    total_gain_loss_percent = (total_gain_loss / total_cost * 100) if total_cost > 0 else 0
    
    return PortfolioSummary(
        total_value=round(total_value + cash_balance, 2),
        total_cost=round(total_cost, 2),
        total_gain_loss=round(total_gain_loss, 2),
        total_gain_loss_percent=round(total_gain_loss_percent, 2),
        positions_count=len(positions),
        cash_balance=round(cash_balance, 2)
    )

# ==================== WATCHLIST ENDPOINTS ====================

@app.get("/api/watchlist")
async def get_watchlist(current_user: dict = Depends(get_current_user)):
    watchlist = await db.watchlists.find_one({"user_id": current_user["_id"]})
    
    if not watchlist:
        return {"stocks": []}
    
    stocks_data = []
    for ticker in watchlist.get("stocks", []):
        try:
            stock = yf.Ticker(ticker)
            info = stock.info
            current_price = info.get("regularMarketPrice", info.get("currentPrice", 0))
            previous_close = info.get("previousClose", info.get("regularMarketPreviousClose", 0))
            change = current_price - previous_close if previous_close else 0
            change_percent = (change / previous_close * 100) if previous_close else 0
            
            stocks_data.append({
                "ticker": ticker,
                "name": info.get("shortName", ticker),
                "price": round(current_price, 2),
                "change": round(change, 2),
                "change_percent": round(change_percent, 2)
            })
        except:
            stocks_data.append({
                "ticker": ticker,
                "name": ticker,
                "price": 0,
                "change": 0,
                "change_percent": 0
            })
    
    return {"stocks": stocks_data}

@app.post("/api/watchlist")
async def add_to_watchlist(item: WatchlistItem, current_user: dict = Depends(get_current_user)):
    ticker = item.stock_ticker.upper()
    
    # Verify stock exists
    try:
        stock = yf.Ticker(ticker)
        info = stock.info
        if not info or "shortName" not in info:
            raise HTTPException(status_code=404, detail=f"Stock {ticker} not found")
    except:
        raise HTTPException(status_code=404, detail=f"Stock {ticker} not found")
    
    watchlist = await db.watchlists.find_one({"user_id": current_user["_id"]})
    
    if watchlist:
        if ticker in watchlist.get("stocks", []):
            raise HTTPException(status_code=400, detail="Stock already in watchlist")
        
        await db.watchlists.update_one(
            {"_id": watchlist["_id"]},
            {"$push": {"stocks": ticker}}
        )
    else:
        await db.watchlists.insert_one({
            "_id": str(uuid.uuid4()),
            "user_id": current_user["_id"],
            "stocks": [ticker]
        })
    
    return {"message": f"{ticker} added to watchlist"}

@app.delete("/api/watchlist/{ticker}")
async def remove_from_watchlist(ticker: str, current_user: dict = Depends(get_current_user)):
    ticker = ticker.upper()
    
    result = await db.watchlists.update_one(
        {"user_id": current_user["_id"]},
        {"$pull": {"stocks": ticker}}
    )
    
    if result.modified_count == 0:
        raise HTTPException(status_code=404, detail="Stock not in watchlist")
    
    return {"message": f"{ticker} removed from watchlist"}

# ==================== MARKET DATA ENDPOINTS ====================

@app.get("/api/market/movers")
async def get_market_movers():
    # Popular stocks to track
    popular_tickers = ["AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "TSLA", "META", "JPM", "V", "WMT"]
    
    movers = []
    for ticker in popular_tickers:
        try:
            stock = yf.Ticker(ticker)
            info = stock.info
            current_price = info.get("regularMarketPrice", info.get("currentPrice", 0))
            previous_close = info.get("previousClose", 0)
            change = current_price - previous_close if previous_close else 0
            change_percent = (change / previous_close * 100) if previous_close else 0
            
            movers.append({
                "ticker": ticker,
                "name": info.get("shortName", ticker),
                "price": round(current_price, 2),
                "change": round(change, 2),
                "change_percent": round(change_percent, 2),
                "volume": info.get("volume", 0)
            })
        except:
            continue
    
    # Sort by absolute change percent
    movers.sort(key=lambda x: abs(x["change_percent"]), reverse=True)
    
    return {"movers": movers}

@app.get("/api/market/indices")
async def get_market_indices():
    indices = {
        "^GSPC": "S&P 500",
        "^DJI": "Dow Jones",
        "^IXIC": "NASDAQ"
    }
    
    result = []
    for ticker, name in indices.items():
        try:
            stock = yf.Ticker(ticker)
            info = stock.info
            current_price = info.get("regularMarketPrice", info.get("currentPrice", 0))
            previous_close = info.get("previousClose", 0)
            change = current_price - previous_close if previous_close else 0
            change_percent = (change / previous_close * 100) if previous_close else 0
            
            result.append({
                "ticker": ticker,
                "name": name,
                "price": round(current_price, 2),
                "change": round(change, 2),
                "change_percent": round(change_percent, 2)
            })
        except:
            continue
    
    return {"indices": result}

# ==================== TD SCHWAB INTEGRATION ====================

TD_SCHWAB_BASE_URL = "https://api.schwabapi.com/v1"
TD_SCHWAB_TOKEN_URL = "https://api.schwabapi.com/v1/oauth/token"

@app.post("/api/schwab/authenticate")
async def authenticate_td_schwab(auth_request: TDSchwabAuthRequest, current_user: dict = Depends(get_current_user)):
    client_id = os.environ.get("TD_SCHWAB_CLIENT_ID")
    client_secret = os.environ.get("TD_SCHWAB_CLIENT_SECRET")
    redirect_uri = os.environ.get("TD_SCHWAB_REDIRECT_URI")
    
    if not client_id or not client_secret:
        raise HTTPException(status_code=500, detail="TD Schwab API credentials not configured")
    
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                TD_SCHWAB_TOKEN_URL,
                data={
                    "grant_type": "authorization_code",
                    "access_type": "offline",
                    "code": auth_request.auth_code,
                    "client_id": client_id,
                    "redirect_uri": redirect_uri
                },
                headers={"Content-Type": "application/x-www-form-urlencoded"}
            )
            
            if response.status_code != 200:
                raise HTTPException(status_code=400, detail="Authentication failed")
            
            token_data = response.json()
            
            # Store tokens in user record
            await db.users.update_one(
                {"_id": current_user["_id"]},
                {"$set": {
                    "td_schwab_access_token": token_data.get("access_token"),
                    "td_schwab_refresh_token": token_data.get("refresh_token"),
                    "td_schwab_token_expiry": datetime.utcnow() + timedelta(seconds=token_data.get("expires_in", 1800))
                }}
            )
            
            return {"message": "TD Schwab account connected successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Authentication error: {str(e)}")

@app.get("/api/schwab/auth-url")
async def get_td_schwab_auth_url():
    client_id = os.environ.get("TD_SCHWAB_CLIENT_ID")
    redirect_uri = os.environ.get("TD_SCHWAB_REDIRECT_URI")
    
    if not client_id:
        raise HTTPException(status_code=500, detail="TD Schwab API not configured")
    
    auth_url = f"https://api.schwabapi.com/v1/oauth/authorize?response_type=code&client_id={client_id}&scope=readonly&redirect_uri={redirect_uri}"
    
    return {"auth_url": auth_url}

# ==================== HEALTH CHECK ====================

@app.get("/api/health")
async def health_check():
    return {"status": "healthy", "timestamp": datetime.utcnow().isoformat()}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
