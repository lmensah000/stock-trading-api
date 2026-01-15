"""Trading endpoint tests."""
import pytest
import httpx
import uuid

TEST_API_URL = "http://localhost:8001"


class TestTradeExecution:
    """Test suite for trade execution."""
    
    @pytest.mark.asyncio
    async def test_execute_buy_trade(self, http_client, auth_headers):
        """Test executing a BUY trade."""
        trade_data = {
            "stock_ticker": "AAPL",
            "quantity": 5,
            "price": 185.92,
            "trade_type": "BUY"
        }
        
        response = await http_client.post("/api/trades", json=trade_data, headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        
        assert data["stock_ticker"] == "AAPL"
        assert data["quantity"] == 5
        assert data["trade_type"] == "BUY"
        assert data["status"] == "EXECUTED"
        assert "id" in data
        assert "execution_date" in data
        assert data["total_value"] == pytest.approx(5 * 185.92, rel=0.01)
    
    @pytest.mark.asyncio
    async def test_execute_sell_trade_with_position(self, http_client, auth_headers):
        """Test executing a SELL trade when user has position."""
        # First buy some shares
        buy_trade = {
            "stock_ticker": "MSFT",
            "quantity": 10,
            "price": 378.91,
            "trade_type": "BUY"
        }
        await http_client.post("/api/trades", json=buy_trade, headers=auth_headers)
        
        # Now sell some
        sell_trade = {
            "stock_ticker": "MSFT",
            "quantity": 5,
            "price": 380.00,
            "trade_type": "SELL"
        }
        response = await http_client.post("/api/trades", json=sell_trade, headers=auth_headers)
        assert response.status_code == 200
        assert response.json()["trade_type"] == "SELL"
    
    @pytest.mark.asyncio
    async def test_sell_without_position_fails(self, http_client, auth_headers):
        """Test SELL trade fails without sufficient position."""
        sell_trade = {
            "stock_ticker": "GOOGL",
            "quantity": 100,
            "price": 141.80,
            "trade_type": "SELL"
        }
        response = await http_client.post("/api/trades", json=sell_trade, headers=auth_headers)
        assert response.status_code == 400
        assert "insufficient" in response.json()["detail"].lower()
    
    @pytest.mark.asyncio
    async def test_buy_exceeds_balance_fails(self, http_client, auth_headers):
        """Test BUY trade fails when exceeding cash balance."""
        # Try to buy way more than $100k balance
        trade_data = {
            "stock_ticker": "AAPL",
            "quantity": 10000,
            "price": 185.92,
            "trade_type": "BUY"
        }
        response = await http_client.post("/api/trades", json=trade_data, headers=auth_headers)
        assert response.status_code == 400
        assert "insufficient" in response.json()["detail"].lower()
    
    @pytest.mark.asyncio
    async def test_trade_invalid_type(self, http_client, auth_headers):
        """Test trade with invalid type fails."""
        trade_data = {
            "stock_ticker": "AAPL",
            "quantity": 5,
            "price": 185.92,
            "trade_type": "INVALID"
        }
        response = await http_client.post("/api/trades", json=trade_data, headers=auth_headers)
        assert response.status_code == 422  # Validation error
    
    @pytest.mark.asyncio
    async def test_trade_negative_quantity_fails(self, http_client, auth_headers):
        """Test trade with negative quantity fails."""
        trade_data = {
            "stock_ticker": "AAPL",
            "quantity": -5,
            "price": 185.92,
            "trade_type": "BUY"
        }
        response = await http_client.post("/api/trades", json=trade_data, headers=auth_headers)
        assert response.status_code == 422
    
    @pytest.mark.asyncio
    async def test_trade_zero_quantity_fails(self, http_client, auth_headers):
        """Test trade with zero quantity fails."""
        trade_data = {
            "stock_ticker": "AAPL",
            "quantity": 0,
            "price": 185.92,
            "trade_type": "BUY"
        }
        response = await http_client.post("/api/trades", json=trade_data, headers=auth_headers)
        assert response.status_code == 422
    
    @pytest.mark.asyncio
    async def test_trade_without_auth_fails(self, http_client):
        """Test trade without authentication fails."""
        trade_data = {
            "stock_ticker": "AAPL",
            "quantity": 5,
            "price": 185.92,
            "trade_type": "BUY"
        }
        response = await http_client.post("/api/trades", json=trade_data)
        assert response.status_code == 403


class TestTradeHistory:
    """Test suite for trade history."""
    
    @pytest.mark.asyncio
    async def test_get_trade_history(self, http_client, auth_headers):
        """Test getting trade history."""
        # Execute a trade first
        trade_data = {
            "stock_ticker": "NVDA",
            "quantity": 2,
            "price": 495.22,
            "trade_type": "BUY"
        }
        await http_client.post("/api/trades", json=trade_data, headers=auth_headers)
        
        # Get history
        response = await http_client.get("/api/trades", headers=auth_headers)
        assert response.status_code == 200
        trades = response.json()
        
        assert isinstance(trades, list)
        assert len(trades) > 0
        
        # Verify trade structure
        trade = trades[0]
        assert "id" in trade
        assert "stock_ticker" in trade
        assert "quantity" in trade
        assert "price" in trade
        assert "trade_type" in trade
        assert "status" in trade
        assert "execution_date" in trade
    
    @pytest.mark.asyncio
    async def test_trade_history_sorted_by_date(self, http_client, auth_headers):
        """Test trade history is sorted by execution date descending."""
        # Execute multiple trades
        for ticker in ["AAPL", "MSFT", "GOOGL"]:
            await http_client.post("/api/trades", json={
                "stock_ticker": ticker,
                "quantity": 1,
                "price": 100.0,
                "trade_type": "BUY"
            }, headers=auth_headers)
        
        response = await http_client.get("/api/trades", headers=auth_headers)
        trades = response.json()
        
        # Check sorted by date descending (most recent first)
        if len(trades) > 1:
            from datetime import datetime
            for i in range(len(trades) - 1):
                date1 = datetime.fromisoformat(trades[i]["execution_date"].replace("Z", "+00:00"))
                date2 = datetime.fromisoformat(trades[i+1]["execution_date"].replace("Z", "+00:00"))
                assert date1 >= date2
