"""Watchlist endpoint tests."""
import pytest
import httpx

TEST_API_URL = "http://localhost:8001"


class TestWatchlistOperations:
    """Test suite for watchlist operations."""
    
    @pytest.mark.asyncio
    async def test_get_empty_watchlist(self, http_client, test_user_data):
        """Test getting watchlist for new user."""
        # Register new user
        reg_response = await http_client.post("/api/auth/register", json=test_user_data)
        token = reg_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        response = await http_client.get("/api/watchlist", headers=headers)
        assert response.status_code == 200
        data = response.json()
        assert "stocks" in data
        assert data["stocks"] == []
    
    @pytest.mark.asyncio
    async def test_add_to_watchlist(self, http_client, auth_headers):
        """Test adding stock to watchlist."""
        response = await http_client.post("/api/watchlist", 
            json={"stock_ticker": "AAPL"}, 
            headers=auth_headers
        )
        assert response.status_code == 200
        assert "added" in response.json()["message"].lower()
    
    @pytest.mark.asyncio
    async def test_watchlist_contains_added_stock(self, http_client, auth_headers):
        """Test watchlist contains added stock with price data."""
        # Add stock
        await http_client.post("/api/watchlist", 
            json={"stock_ticker": "MSFT"}, 
            headers=auth_headers
        )
        
        # Get watchlist
        response = await http_client.get("/api/watchlist", headers=auth_headers)
        data = response.json()
        
        # Find MSFT in watchlist
        msft = next((s for s in data["stocks"] if s["ticker"] == "MSFT"), None)
        assert msft is not None
        assert "name" in msft
        assert "price" in msft
        assert "change" in msft
        assert "change_percent" in msft
    
    @pytest.mark.asyncio
    async def test_add_duplicate_stock_fails(self, http_client, auth_headers):
        """Test adding duplicate stock to watchlist fails."""
        # Add stock first time
        await http_client.post("/api/watchlist", 
            json={"stock_ticker": "GOOGL"}, 
            headers=auth_headers
        )
        
        # Try adding again
        response = await http_client.post("/api/watchlist", 
            json={"stock_ticker": "GOOGL"}, 
            headers=auth_headers
        )
        assert response.status_code == 400
        assert "already" in response.json()["detail"].lower()
    
    @pytest.mark.asyncio
    async def test_add_invalid_ticker_fails(self, http_client, auth_headers):
        """Test adding invalid ticker to watchlist fails."""
        response = await http_client.post("/api/watchlist", 
            json={"stock_ticker": "INVALID123XYZ"}, 
            headers=auth_headers
        )
        assert response.status_code == 404
    
    @pytest.mark.asyncio
    async def test_remove_from_watchlist(self, http_client, auth_headers):
        """Test removing stock from watchlist."""
        # Add stock
        await http_client.post("/api/watchlist", 
            json={"stock_ticker": "AMZN"}, 
            headers=auth_headers
        )
        
        # Remove stock
        response = await http_client.delete("/api/watchlist/AMZN", headers=auth_headers)
        assert response.status_code == 200
        assert "removed" in response.json()["message"].lower()
    
    @pytest.mark.asyncio
    async def test_watchlist_after_removal(self, http_client, auth_headers):
        """Test watchlist no longer contains removed stock."""
        # Add and remove stock
        await http_client.post("/api/watchlist", 
            json={"stock_ticker": "NVDA"}, 
            headers=auth_headers
        )
        await http_client.delete("/api/watchlist/NVDA", headers=auth_headers)
        
        # Check watchlist
        response = await http_client.get("/api/watchlist", headers=auth_headers)
        stocks = response.json()["stocks"]
        nvda = next((s for s in stocks if s["ticker"] == "NVDA"), None)
        assert nvda is None
    
    @pytest.mark.asyncio
    async def test_remove_nonexistent_stock_fails(self, http_client, auth_headers):
        """Test removing non-existent stock from watchlist fails."""
        response = await http_client.delete("/api/watchlist/NONEXISTENT", headers=auth_headers)
        assert response.status_code == 404
    
    @pytest.mark.asyncio
    async def test_watchlist_ticker_case_insensitive(self, http_client, auth_headers):
        """Test watchlist handles ticker case insensitively."""
        # Add with lowercase
        response = await http_client.post("/api/watchlist", 
            json={"stock_ticker": "meta"}, 
            headers=auth_headers
        )
        assert response.status_code == 200
        
        # Check it's stored as uppercase
        watchlist = await http_client.get("/api/watchlist", headers=auth_headers)
        stocks = watchlist.json()["stocks"]
        meta = next((s for s in stocks if s["ticker"] == "META"), None)
        assert meta is not None
    
    @pytest.mark.asyncio
    async def test_watchlist_without_auth_fails(self, http_client):
        """Test watchlist endpoints require authentication."""
        # GET without auth
        response = await http_client.get("/api/watchlist")
        assert response.status_code == 403
        
        # POST without auth
        response = await http_client.post("/api/watchlist", json={"stock_ticker": "AAPL"})
        assert response.status_code == 403
        
        # DELETE without auth
        response = await http_client.delete("/api/watchlist/AAPL")
        assert response.status_code == 403
