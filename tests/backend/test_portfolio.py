"""Portfolio endpoint tests."""
import pytest
import httpx

TEST_API_URL = "http://localhost:8001"


class TestPortfolioSummary:
    """Test suite for portfolio summary endpoint."""
    
    @pytest.mark.asyncio
    async def test_get_portfolio_summary(self, http_client, auth_headers):
        """Test getting portfolio summary."""
        response = await http_client.get("/api/portfolio/summary", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        
        assert "total_value" in data
        assert "total_cost" in data
        assert "total_gain_loss" in data
        assert "total_gain_loss_percent" in data
        assert "positions_count" in data
        assert "cash_balance" in data
    
    @pytest.mark.asyncio
    async def test_new_user_starting_balance(self, http_client, test_user_data):
        """Test new user has $100,000 starting balance."""
        # Register new user
        reg_response = await http_client.post("/api/auth/register", json=test_user_data)
        token = reg_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        response = await http_client.get("/api/portfolio/summary", headers=headers)
        data = response.json()
        
        assert data["cash_balance"] == 100000.0
        assert data["total_value"] == 100000.0
        assert data["positions_count"] == 0
    
    @pytest.mark.asyncio
    async def test_portfolio_after_buy(self, http_client, auth_headers):
        """Test portfolio updates correctly after BUY trade."""
        # Get initial portfolio
        initial = await http_client.get("/api/portfolio/summary", headers=auth_headers)
        initial_cash = initial.json()["cash_balance"]
        
        # Buy shares
        trade_value = 5 * 185.92  # 5 shares of AAPL
        await http_client.post("/api/trades", json={
            "stock_ticker": "AAPL",
            "quantity": 5,
            "price": 185.92,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        # Check updated portfolio
        updated = await http_client.get("/api/portfolio/summary", headers=auth_headers)
        updated_data = updated.json()
        
        # Cash should decrease
        expected_cash = initial_cash - trade_value
        assert updated_data["cash_balance"] == pytest.approx(expected_cash, rel=0.01)
    
    @pytest.mark.asyncio
    async def test_portfolio_after_sell(self, http_client, auth_headers):
        """Test portfolio updates correctly after SELL trade."""
        # First buy some shares
        await http_client.post("/api/trades", json={
            "stock_ticker": "META",
            "quantity": 10,
            "price": 355.64,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        # Get portfolio after buy
        after_buy = await http_client.get("/api/portfolio/summary", headers=auth_headers)
        cash_after_buy = after_buy.json()["cash_balance"]
        
        # Sell some shares
        sell_value = 5 * 360.00  # Selling at higher price
        await http_client.post("/api/trades", json={
            "stock_ticker": "META",
            "quantity": 5,
            "price": 360.00,
            "trade_type": "SELL"
        }, headers=auth_headers)
        
        # Check portfolio after sell
        after_sell = await http_client.get("/api/portfolio/summary", headers=auth_headers)
        cash_after_sell = after_sell.json()["cash_balance"]
        
        # Cash should increase
        assert cash_after_sell == pytest.approx(cash_after_buy + sell_value, rel=0.01)


class TestPortfolioPositions:
    """Test suite for portfolio positions endpoint."""
    
    @pytest.mark.asyncio
    async def test_get_empty_positions(self, http_client, test_user_data):
        """Test getting positions for new user with no trades."""
        # Register new user
        reg_response = await http_client.post("/api/auth/register", json=test_user_data)
        token = reg_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        response = await http_client.get("/api/portfolio/positions", headers=headers)
        assert response.status_code == 200
        positions = response.json()
        assert positions == []
    
    @pytest.mark.asyncio
    async def test_position_created_after_buy(self, http_client, auth_headers):
        """Test position is created after BUY trade."""
        # Buy shares
        await http_client.post("/api/trades", json={
            "stock_ticker": "JPM",
            "quantity": 10,
            "price": 172.45,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        # Check positions
        response = await http_client.get("/api/portfolio/positions", headers=auth_headers)
        positions = response.json()
        
        # Find JPM position
        jpm_position = next((p for p in positions if p["stock_ticker"] == "JPM"), None)
        assert jpm_position is not None
        assert jpm_position["total_quantity"] == 10
        assert jpm_position["average_price"] == 172.45
    
    @pytest.mark.asyncio
    async def test_position_structure(self, http_client, auth_headers):
        """Test position has correct data structure."""
        # Create a position
        await http_client.post("/api/trades", json={
            "stock_ticker": "V",
            "quantity": 5,
            "price": 265.80,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        response = await http_client.get("/api/portfolio/positions", headers=auth_headers)
        positions = response.json()
        position = next((p for p in positions if p["stock_ticker"] == "V"), None)
        
        assert "id" in position
        assert "user_id" in position
        assert "stock_ticker" in position
        assert "total_quantity" in position
        assert "average_price" in position
        assert "current_price" in position
        assert "market_value" in position
        assert "unrealized_pnl" in position
        assert "unrealized_pnl_percent" in position
    
    @pytest.mark.asyncio
    async def test_position_average_price_updates(self, http_client, auth_headers):
        """Test position average price updates correctly on multiple buys."""
        ticker = "WMT"
        
        # First buy: 10 shares at $160
        await http_client.post("/api/trades", json={
            "stock_ticker": ticker,
            "quantity": 10,
            "price": 160.0,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        # Second buy: 10 shares at $170
        await http_client.post("/api/trades", json={
            "stock_ticker": ticker,
            "quantity": 10,
            "price": 170.0,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        # Expected average: (10*160 + 10*170) / 20 = $165
        response = await http_client.get("/api/portfolio/positions", headers=auth_headers)
        positions = response.json()
        position = next((p for p in positions if p["stock_ticker"] == ticker), None)
        
        assert position["total_quantity"] == 20
        assert position["average_price"] == pytest.approx(165.0, rel=0.01)
    
    @pytest.mark.asyncio
    async def test_position_removed_when_fully_sold(self, http_client, auth_headers):
        """Test position is removed when all shares are sold."""
        ticker = "TSLA"
        
        # Buy shares
        await http_client.post("/api/trades", json={
            "stock_ticker": ticker,
            "quantity": 5,
            "price": 248.50,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        # Sell all shares
        await http_client.post("/api/trades", json={
            "stock_ticker": ticker,
            "quantity": 5,
            "price": 250.00,
            "trade_type": "SELL"
        }, headers=auth_headers)
        
        # Check position is removed
        response = await http_client.get("/api/portfolio/positions", headers=auth_headers)
        positions = response.json()
        tsla_position = next((p for p in positions if p["stock_ticker"] == ticker), None)
        
        assert tsla_position is None
