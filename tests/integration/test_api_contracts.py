"""API contract tests to ensure API responses match expected schemas."""
import pytest
import httpx
from typing import Any, Dict, List

TEST_API_URL = "http://localhost:8001"


def validate_schema(data: Dict[str, Any], schema: Dict[str, type]) -> bool:
    """Validate data matches expected schema types."""
    for key, expected_type in schema.items():
        if key not in data:
            return False
        if data[key] is not None and not isinstance(data[key], expected_type):
            return False
    return True


class TestAPIContracts:
    """Test API response schemas match frontend expectations."""
    
    @pytest.mark.asyncio
    async def test_user_response_schema(self, http_client, test_user_data):
        """Test user response matches expected schema."""
        response = await http_client.post("/api/auth/register", json=test_user_data)
        data = response.json()
        
        # Token response schema
        assert "access_token" in data
        assert "token_type" in data
        assert "user" in data
        
        # User schema
        user = data["user"]
        user_schema = {
            "id": str,
            "username": str,
            "email": str,
            "created_at": str
        }
        assert validate_schema(user, user_schema)
    
    @pytest.mark.asyncio
    async def test_stock_quote_schema(self, http_client):
        """Test stock quote response matches expected schema."""
        response = await http_client.get("/api/stocks/quote/AAPL")
        data = response.json()
        
        required_fields = [
            "ticker", "name", "price", "change", "change_percent",
            "volume", "market_cap", "pe_ratio", "dividend_yield",
            "high_52_week", "low_52_week", "open_price", "previous_close",
            "day_high", "day_low"
        ]
        
        for field in required_fields:
            assert field in data, f"Missing field: {field}"
        
        # Type checks for non-nullable fields
        assert isinstance(data["ticker"], str)
        assert isinstance(data["name"], str)
        assert isinstance(data["price"], (int, float))
        assert isinstance(data["change"], (int, float))
        assert isinstance(data["change_percent"], (int, float))
    
    @pytest.mark.asyncio
    async def test_trade_response_schema(self, http_client, auth_headers):
        """Test trade response matches expected schema."""
        response = await http_client.post("/api/trades", json={
            "stock_ticker": "AAPL",
            "quantity": 1,
            "price": 185.92,
            "trade_type": "BUY"
        }, headers=auth_headers)
        data = response.json()
        
        trade_schema = {
            "id": str,
            "user_id": str,
            "stock_ticker": str,
            "quantity": (int, float),
            "price": (int, float),
            "trade_type": str,
            "status": str,
            "execution_date": str,
            "total_value": (int, float)
        }
        
        for field, expected_type in trade_schema.items():
            assert field in data, f"Missing field: {field}"
            assert isinstance(data[field], expected_type), f"Wrong type for {field}"
    
    @pytest.mark.asyncio
    async def test_position_response_schema(self, http_client, auth_headers):
        """Test position response matches expected schema."""
        # Create a position first
        await http_client.post("/api/trades", json={
            "stock_ticker": "MSFT",
            "quantity": 5,
            "price": 378.91,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        response = await http_client.get("/api/portfolio/positions", headers=auth_headers)
        positions = response.json()
        
        assert isinstance(positions, list)
        assert len(positions) > 0
        
        position = positions[0]
        position_schema = {
            "id": str,
            "user_id": str,
            "stock_ticker": str,
            "total_quantity": (int, float),
            "average_price": (int, float),
            "current_price": (int, float),
            "market_value": (int, float),
            "unrealized_pnl": (int, float),
            "unrealized_pnl_percent": (int, float)
        }
        
        for field, expected_type in position_schema.items():
            assert field in position, f"Missing field: {field}"
    
    @pytest.mark.asyncio
    async def test_portfolio_summary_schema(self, http_client, auth_headers):
        """Test portfolio summary response matches expected schema."""
        response = await http_client.get("/api/portfolio/summary", headers=auth_headers)
        data = response.json()
        
        summary_schema = {
            "total_value": (int, float),
            "total_cost": (int, float),
            "total_gain_loss": (int, float),
            "total_gain_loss_percent": (int, float),
            "positions_count": int,
            "cash_balance": (int, float)
        }
        
        for field, expected_type in summary_schema.items():
            assert field in data, f"Missing field: {field}"
            assert isinstance(data[field], expected_type), f"Wrong type for {field}"
    
    @pytest.mark.asyncio
    async def test_watchlist_response_schema(self, http_client, auth_headers):
        """Test watchlist response matches expected schema."""
        # Add to watchlist first
        await http_client.post("/api/watchlist", 
            json={"stock_ticker": "GOOGL"}, 
            headers=auth_headers
        )
        
        response = await http_client.get("/api/watchlist", headers=auth_headers)
        data = response.json()
        
        assert "stocks" in data
        assert isinstance(data["stocks"], list)
        
        if len(data["stocks"]) > 0:
            stock = data["stocks"][0]
            stock_schema = {
                "ticker": str,
                "name": str,
                "price": (int, float),
                "change": (int, float),
                "change_percent": (int, float)
            }
            
            for field, expected_type in stock_schema.items():
                assert field in stock, f"Missing field: {field}"
    
    @pytest.mark.asyncio
    async def test_market_movers_schema(self, http_client):
        """Test market movers response matches expected schema."""
        response = await http_client.get("/api/market/movers")
        data = response.json()
        
        assert "movers" in data
        assert isinstance(data["movers"], list)
        
        if len(data["movers"]) > 0:
            mover = data["movers"][0]
            mover_schema = {
                "ticker": str,
                "name": str,
                "price": (int, float),
                "change": (int, float),
                "change_percent": (int, float),
                "volume": int
            }
            
            for field, expected_type in mover_schema.items():
                assert field in mover, f"Missing field: {field}"
    
    @pytest.mark.asyncio
    async def test_chart_data_schema(self, http_client):
        """Test chart data response matches expected schema."""
        response = await http_client.get("/api/stocks/chart/AAPL")
        data = response.json()
        
        assert "ticker" in data
        assert "data" in data
        assert isinstance(data["data"], list)
        
        if len(data["data"]) > 0:
            point = data["data"][0]
            point_schema = {
                "date": str,
                "open": (int, float),
                "high": (int, float),
                "low": (int, float),
                "close": (int, float),
                "volume": int
            }
            
            for field, expected_type in point_schema.items():
                assert field in point, f"Missing field: {field}"
