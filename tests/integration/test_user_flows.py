"""Integration tests for complete user flows."""
import pytest
import httpx
import uuid

TEST_API_URL = "http://localhost:8001"


class TestCompleteUserJourney:
    """End-to-end tests for complete user journeys."""
    
    @pytest.mark.asyncio
    async def test_new_user_complete_flow(self, http_client):
        """Test complete flow: register -> view stocks -> buy -> check portfolio -> sell."""
        # 1. Register new user
        unique_id = uuid.uuid4().hex[:8]
        user_data = {
            "username": f"e2e_user_{unique_id}",
            "email": f"e2e_{unique_id}@test.com",
            "password": "securepass123"
        }
        
        reg_response = await http_client.post("/api/auth/register", json=user_data)
        assert reg_response.status_code == 200
        token = reg_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        # 2. Verify starting balance
        portfolio = await http_client.get("/api/portfolio/summary", headers=headers)
        assert portfolio.json()["cash_balance"] == 100000.0
        assert portfolio.json()["positions_count"] == 0
        
        # 3. View stock quote
        quote = await http_client.get("/api/stocks/quote/AAPL")
        assert quote.status_code == 200
        stock_price = quote.json()["price"]
        
        # 4. Buy 10 shares
        buy_trade = await http_client.post("/api/trades", json={
            "stock_ticker": "AAPL",
            "quantity": 10,
            "price": stock_price,
            "trade_type": "BUY"
        }, headers=headers)
        assert buy_trade.status_code == 200
        assert buy_trade.json()["status"] == "EXECUTED"
        
        # 5. Verify position created
        positions = await http_client.get("/api/portfolio/positions", headers=headers)
        assert len(positions.json()) == 1
        position = positions.json()[0]
        assert position["stock_ticker"] == "AAPL"
        assert position["total_quantity"] == 10
        
        # 6. Verify cash reduced
        portfolio_after_buy = await http_client.get("/api/portfolio/summary", headers=headers)
        expected_cash = 100000.0 - (10 * stock_price)
        assert portfolio_after_buy.json()["cash_balance"] == pytest.approx(expected_cash, rel=0.01)
        
        # 7. Sell 5 shares
        sell_trade = await http_client.post("/api/trades", json={
            "stock_ticker": "AAPL",
            "quantity": 5,
            "price": stock_price,
            "trade_type": "SELL"
        }, headers=headers)
        assert sell_trade.status_code == 200
        
        # 8. Verify position reduced
        positions_after_sell = await http_client.get("/api/portfolio/positions", headers=headers)
        assert positions_after_sell.json()[0]["total_quantity"] == 5
        
        # 9. Check trade history has 2 trades
        trades = await http_client.get("/api/trades", headers=headers)
        assert len(trades.json()) == 2
    
    @pytest.mark.asyncio
    async def test_watchlist_workflow(self, http_client):
        """Test complete watchlist workflow: add stocks -> view -> remove."""
        # Register user
        unique_id = uuid.uuid4().hex[:8]
        reg_response = await http_client.post("/api/auth/register", json={
            "username": f"watchlist_user_{unique_id}",
            "email": f"watchlist_{unique_id}@test.com",
            "password": "password123"
        })
        token = reg_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        # Add multiple stocks to watchlist
        for ticker in ["AAPL", "MSFT", "GOOGL"]:
            response = await http_client.post("/api/watchlist", 
                json={"stock_ticker": ticker}, 
                headers=headers
            )
            assert response.status_code == 200
        
        # Verify watchlist has 3 stocks
        watchlist = await http_client.get("/api/watchlist", headers=headers)
        assert len(watchlist.json()["stocks"]) == 3
        
        # Remove one stock
        await http_client.delete("/api/watchlist/MSFT", headers=headers)
        
        # Verify watchlist has 2 stocks
        watchlist_after = await http_client.get("/api/watchlist", headers=headers)
        assert len(watchlist_after.json()["stocks"]) == 2
        tickers = [s["ticker"] for s in watchlist_after.json()["stocks"]]
        assert "MSFT" not in tickers
        assert "AAPL" in tickers
        assert "GOOGL" in tickers
    
    @pytest.mark.asyncio
    async def test_multiple_trades_portfolio_accuracy(self, http_client):
        """Test portfolio calculations remain accurate after multiple trades."""
        # Register user
        unique_id = uuid.uuid4().hex[:8]
        reg_response = await http_client.post("/api/auth/register", json={
            "username": f"multi_trade_user_{unique_id}",
            "email": f"multi_{unique_id}@test.com",
            "password": "password123"
        })
        token = reg_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        initial_cash = 100000.0
        total_spent = 0
        
        # Execute multiple buy trades
        trades_to_execute = [
            {"ticker": "AAPL", "qty": 5, "price": 185.92},
            {"ticker": "MSFT", "qty": 3, "price": 378.91},
            {"ticker": "GOOGL", "qty": 10, "price": 141.80},
        ]
        
        for trade in trades_to_execute:
            await http_client.post("/api/trades", json={
                "stock_ticker": trade["ticker"],
                "quantity": trade["qty"],
                "price": trade["price"],
                "trade_type": "BUY"
            }, headers=headers)
            total_spent += trade["qty"] * trade["price"]
        
        # Verify portfolio
        portfolio = await http_client.get("/api/portfolio/summary", headers=headers)
        data = portfolio.json()
        
        assert data["positions_count"] == 3
        assert data["cash_balance"] == pytest.approx(initial_cash - total_spent, rel=0.01)
        assert data["total_cost"] == pytest.approx(total_spent, rel=0.01)


class TestAuthenticationFlows:
    """Integration tests for authentication flows."""
    
    @pytest.mark.asyncio
    async def test_login_logout_login(self, http_client):
        """Test user can login, use token, then login again."""
        unique_id = uuid.uuid4().hex[:8]
        user_data = {
            "username": f"auth_flow_user_{unique_id}",
            "email": f"auth_{unique_id}@test.com",
            "password": "password123"
        }
        
        # Register
        await http_client.post("/api/auth/register", json=user_data)
        
        # Login first time
        login1 = await http_client.post("/api/auth/login", json={
            "username": user_data["username"],
            "password": user_data["password"]
        })
        token1 = login1.json()["access_token"]
        
        # Use token
        me1 = await http_client.get("/api/auth/me", headers={"Authorization": f"Bearer {token1}"})
        assert me1.status_code == 200
        
        # Login second time (simulating re-login)
        login2 = await http_client.post("/api/auth/login", json={
            "username": user_data["username"],
            "password": user_data["password"]
        })
        token2 = login2.json()["access_token"]
        
        # Both tokens should work (no token invalidation)
        me2 = await http_client.get("/api/auth/me", headers={"Authorization": f"Bearer {token2}"})
        assert me2.status_code == 200
    
    @pytest.mark.asyncio
    async def test_data_isolation_between_users(self, http_client):
        """Test that users cannot see each other's data."""
        # Create two users
        users = []
        for i in range(2):
            unique_id = uuid.uuid4().hex[:8]
            reg = await http_client.post("/api/auth/register", json={
                "username": f"isolation_user_{unique_id}",
                "email": f"isolation_{unique_id}@test.com",
                "password": "password123"
            })
            users.append({
                "token": reg.json()["access_token"],
                "headers": {"Authorization": f"Bearer {reg.json()['access_token']}"}
            })
        
        # User 1 buys stock
        await http_client.post("/api/trades", json={
            "stock_ticker": "AAPL",
            "quantity": 10,
            "price": 185.92,
            "trade_type": "BUY"
        }, headers=users[0]["headers"])
        
        # User 1 adds to watchlist
        await http_client.post("/api/watchlist", 
            json={"stock_ticker": "MSFT"}, 
            headers=users[0]["headers"]
        )
        
        # User 2 should have empty portfolio and watchlist
        user2_positions = await http_client.get("/api/portfolio/positions", headers=users[1]["headers"])
        assert user2_positions.json() == []
        
        user2_watchlist = await http_client.get("/api/watchlist", headers=users[1]["headers"])
        assert user2_watchlist.json()["stocks"] == []
        
        user2_trades = await http_client.get("/api/trades", headers=users[1]["headers"])
        assert user2_trades.json() == []


class TestEdgeCases:
    """Integration tests for edge cases and error handling."""
    
    @pytest.mark.asyncio
    async def test_sell_more_than_owned(self, http_client, auth_headers):
        """Test cannot sell more shares than owned."""
        # Buy 5 shares
        await http_client.post("/api/trades", json={
            "stock_ticker": "NVDA",
            "quantity": 5,
            "price": 495.22,
            "trade_type": "BUY"
        }, headers=auth_headers)
        
        # Try to sell 10 shares
        response = await http_client.post("/api/trades", json={
            "stock_ticker": "NVDA",
            "quantity": 10,
            "price": 495.22,
            "trade_type": "SELL"
        }, headers=auth_headers)
        
        assert response.status_code == 400
        assert "insufficient" in response.json()["detail"].lower()
    
    @pytest.mark.asyncio
    async def test_rapid_trades(self, http_client, auth_headers):
        """Test system handles rapid consecutive trades."""
        import asyncio
        
        # Execute 5 trades rapidly
        tasks = []
        for i in range(5):
            task = http_client.post("/api/trades", json={
                "stock_ticker": "AAPL",
                "quantity": 1,
                "price": 185.92,
                "trade_type": "BUY"
            }, headers=auth_headers)
            tasks.append(task)
        
        responses = await asyncio.gather(*tasks)
        
        # All should succeed
        for response in responses:
            assert response.status_code == 200
        
        # Verify correct position
        positions = await http_client.get("/api/portfolio/positions", headers=auth_headers)
        aapl_position = next((p for p in positions.json() if p["stock_ticker"] == "AAPL"), None)
        assert aapl_position is not None
        # Should have at least 5 shares from rapid trades
        assert aapl_position["total_quantity"] >= 5
