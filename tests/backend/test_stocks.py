"""Stock data endpoint tests."""
import pytest
import httpx

TEST_API_URL = "http://localhost:8001"

# Demo stocks that should always work
DEMO_TICKERS = ["AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "TSLA", "META", "JPM", "V", "WMT"]


class TestStockQuotes:
    """Test suite for stock quote endpoints."""
    
    @pytest.mark.asyncio
    @pytest.mark.parametrize("ticker", DEMO_TICKERS[:3])
    async def test_get_stock_quote(self, http_client, ticker):
        """Test getting stock quote for demo tickers."""
        response = await http_client.get(f"/api/stocks/quote/{ticker}")
        assert response.status_code == 200
        data = response.json()
        
        assert data["ticker"] == ticker
        assert "name" in data
        assert "price" in data
        assert data["price"] > 0
        assert "change" in data
        assert "change_percent" in data
        assert "volume" in data
    
    @pytest.mark.asyncio
    async def test_get_stock_quote_case_insensitive(self, http_client):
        """Test stock quote works with lowercase ticker."""
        response = await http_client.get("/api/stocks/quote/aapl")
        assert response.status_code == 200
        assert response.json()["ticker"] == "AAPL"
    
    @pytest.mark.asyncio
    async def test_get_stock_quote_invalid_ticker(self, http_client):
        """Test stock quote returns 404 for invalid ticker."""
        response = await http_client.get("/api/stocks/quote/INVALID123XYZ")
        assert response.status_code == 404


class TestStockFundamentals:
    """Test suite for stock fundamentals endpoint."""
    
    @pytest.mark.asyncio
    async def test_get_fundamentals(self, http_client):
        """Test getting stock fundamentals."""
        response = await http_client.get("/api/stocks/fundamentals/AAPL")
        assert response.status_code == 200
        data = response.json()
        
        assert data["ticker"] == "AAPL"
        assert "name" in data
        assert "sector" in data
        assert "market_cap" in data
        assert "pe_ratio" in data
    
    @pytest.mark.asyncio
    async def test_fundamentals_financial_ratios(self, http_client):
        """Test fundamentals include financial ratios."""
        response = await http_client.get("/api/stocks/fundamentals/MSFT")
        data = response.json()
        
        # Check optional financial fields exist
        expected_fields = ["eps", "pb_ratio", "dividend_yield", "beta"]
        for field in expected_fields:
            assert field in data


class TestStockCharts:
    """Test suite for stock chart endpoint."""
    
    @pytest.mark.asyncio
    async def test_get_chart_default_period(self, http_client):
        """Test getting chart with default period."""
        response = await http_client.get("/api/stocks/chart/AAPL")
        assert response.status_code == 200
        data = response.json()
        
        assert data["ticker"] == "AAPL"
        assert "data" in data
        assert len(data["data"]) > 0
    
    @pytest.mark.asyncio
    @pytest.mark.parametrize("period", ["1d", "5d", "1mo", "3mo", "1y"])
    async def test_get_chart_different_periods(self, http_client, period):
        """Test getting chart with different time periods."""
        response = await http_client.get(f"/api/stocks/chart/AAPL?period={period}")
        assert response.status_code == 200
        data = response.json()
        assert len(data["data"]) > 0
    
    @pytest.mark.asyncio
    async def test_chart_data_structure(self, http_client):
        """Test chart data has correct OHLCV structure."""
        response = await http_client.get("/api/stocks/chart/AAPL")
        data = response.json()["data"]
        
        if len(data) > 0:
            point = data[0]
            assert "date" in point
            assert "open" in point
            assert "high" in point
            assert "low" in point
            assert "close" in point
            assert "volume" in point


class TestMarketData:
    """Test suite for market data endpoints."""
    
    @pytest.mark.asyncio
    async def test_get_market_movers(self, http_client):
        """Test getting market movers."""
        response = await http_client.get("/api/market/movers")
        assert response.status_code == 200
        data = response.json()
        
        assert "movers" in data
        assert len(data["movers"]) > 0
        
        mover = data["movers"][0]
        assert "ticker" in mover
        assert "name" in mover
        assert "price" in mover
        assert "change" in mover
        assert "change_percent" in mover
    
    @pytest.mark.asyncio
    async def test_market_movers_sorted_by_change(self, http_client):
        """Test market movers are sorted by absolute change percent."""
        response = await http_client.get("/api/market/movers")
        movers = response.json()["movers"]
        
        if len(movers) > 1:
            # Check sorted by absolute change percent descending
            for i in range(len(movers) - 1):
                assert abs(movers[i]["change_percent"]) >= abs(movers[i+1]["change_percent"])
