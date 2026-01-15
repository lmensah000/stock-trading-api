#!/usr/bin/env python3
"""
MoneyTeam Stock Trading API Backend Test Suite
Tests all core functionality including auth, trading, portfolio, and watchlist
"""

import requests
import sys
import json
from datetime import datetime
import time
import uuid

class MoneyTeamAPITester:
    def __init__(self, base_url="http://localhost:8001"):
        self.base_url = base_url
        self.token = None
        self.user_id = None
        self.tests_run = 0
        self.tests_passed = 0
        self.test_results = []
        
        # Generate unique test user
        self.test_username = f"testuser_{int(time.time())}"
        self.test_email = f"test_{int(time.time())}@example.com"
        self.test_password = "TestPass123!"

    def log_result(self, test_name, success, details="", response_data=None):
        """Log test result"""
        self.tests_run += 1
        if success:
            self.tests_passed += 1
            print(f"✅ {test_name} - PASSED")
        else:
            print(f"❌ {test_name} - FAILED: {details}")
        
        self.test_results.append({
            "test": test_name,
            "success": success,
            "details": details,
            "response_data": response_data
        })

    def run_test(self, name, method, endpoint, expected_status, data=None, headers=None):
        """Run a single API test"""
        url = f"{self.base_url}{endpoint}"
        test_headers = {'Content-Type': 'application/json'}
        
        if self.token:
            test_headers['Authorization'] = f'Bearer {self.token}'
        
        if headers:
            test_headers.update(headers)

        print(f"\n🔍 Testing {name}...")
        print(f"   URL: {method} {url}")
        
        try:
            if method == 'GET':
                response = requests.get(url, headers=test_headers, timeout=10)
            elif method == 'POST':
                response = requests.post(url, json=data, headers=test_headers, timeout=10)
            elif method == 'DELETE':
                response = requests.delete(url, headers=test_headers, timeout=10)
            else:
                raise ValueError(f"Unsupported method: {method}")

            print(f"   Status: {response.status_code}")
            
            success = response.status_code == expected_status
            response_data = None
            
            try:
                response_data = response.json()
                if success:
                    print(f"   Response: {json.dumps(response_data, indent=2)[:200]}...")
                else:
                    print(f"   Error Response: {response_data}")
            except:
                if not success:
                    print(f"   Raw Response: {response.text[:200]}...")

            self.log_result(name, success, 
                          f"Expected {expected_status}, got {response.status_code}" if not success else "",
                          response_data)
            
            return success, response_data

        except Exception as e:
            error_msg = f"Request failed: {str(e)}"
            print(f"   ❌ {error_msg}")
            self.log_result(name, False, error_msg)
            return False, {}

    def test_health_check(self):
        """Test basic health endpoint"""
        return self.run_test("Health Check", "GET", "/", 200)

    def test_api_health(self):
        """Test API health endpoint"""
        return self.run_test("API Health Check", "GET", "/api/health", 200)

    def test_user_registration(self):
        """Test user registration"""
        success, response = self.run_test(
            "User Registration",
            "POST",
            "/api/auth/register",
            200,
            data={
                "username": self.test_username,
                "password": self.test_password,
                "email": self.test_email
            }
        )
        
        if success and response:
            self.token = response.get('access_token')
            if response.get('user'):
                self.user_id = response['user'].get('id')
            print(f"   ✅ Token obtained: {self.token[:20]}...")
            print(f"   ✅ User ID: {self.user_id}")
        
        return success

    def test_user_login(self):
        """Test user login"""
        success, response = self.run_test(
            "User Login",
            "POST",
            "/api/auth/login",
            200,
            data={
                "username": self.test_username,
                "password": self.test_password
            }
        )
        
        if success and response:
            self.token = response.get('access_token')
            if response.get('user'):
                self.user_id = response['user'].get('id')
            print(f"   ✅ Login successful, token: {self.token[:20]}...")
        
        return success

    def test_get_current_user(self):
        """Test getting current user info"""
        return self.run_test("Get Current User", "GET", "/api/auth/me", 200)

    def test_stock_quote(self, ticker="AAPL"):
        """Test getting stock quote"""
        success, response = self.run_test(
            f"Get Stock Quote ({ticker})",
            "GET",
            f"/api/stocks/quote/{ticker}",
            200
        )
        
        if success and response:
            print(f"   ✅ Stock: {response.get('name', 'N/A')}")
            print(f"   ✅ Price: ${response.get('price', 0)}")
            print(f"   ✅ Change: {response.get('change_percent', 0)}%")
        
        return success, response

    def test_stock_fundamentals(self, ticker="AAPL"):
        """Test getting stock fundamentals"""
        return self.run_test(
            f"Get Stock Fundamentals ({ticker})",
            "GET",
            f"/api/stocks/fundamentals/{ticker}",
            200
        )

    def test_stock_chart(self, ticker="AAPL"):
        """Test getting stock chart data"""
        return self.run_test(
            f"Get Stock Chart ({ticker})",
            "GET",
            f"/api/stocks/chart/{ticker}?period=1mo",
            200
        )

    def test_market_movers(self):
        """Test getting market movers"""
        success, response = self.run_test("Get Market Movers", "GET", "/api/market/movers", 200)
        
        if success and response:
            movers = response.get('movers', [])
            print(f"   ✅ Found {len(movers)} market movers")
            if movers:
                print(f"   ✅ Top mover: {movers[0].get('ticker', 'N/A')} ({movers[0].get('change_percent', 0)}%)")
        
        return success

    def test_portfolio_summary(self):
        """Test getting portfolio summary"""
        success, response = self.run_test("Get Portfolio Summary", "GET", "/api/portfolio/summary", 200)
        
        if success and response:
            print(f"   ✅ Total Value: ${response.get('total_value', 0)}")
            print(f"   ✅ Cash Balance: ${response.get('cash_balance', 0)}")
            print(f"   ✅ Positions: {response.get('positions_count', 0)}")
        
        return success, response

    def test_portfolio_positions(self):
        """Test getting portfolio positions"""
        success, response = self.run_test("Get Portfolio Positions", "GET", "/api/portfolio/positions", 200)
        
        if success and response:
            print(f"   ✅ Found {len(response)} positions")
        
        return success, response

    def test_execute_buy_trade(self, ticker="AAPL", quantity=1.0, price=185.0):
        """Test executing a BUY trade"""
        success, response = self.run_test(
            f"Execute BUY Trade ({quantity} shares of {ticker})",
            "POST",
            "/api/trades",
            200,
            data={
                "stock_ticker": ticker,
                "quantity": quantity,
                "price": price,
                "trade_type": "BUY"
            }
        )
        
        if success and response:
            print(f"   ✅ Trade ID: {response.get('id', 'N/A')}")
            print(f"   ✅ Status: {response.get('status', 'N/A')}")
            print(f"   ✅ Total Value: ${response.get('total_value', 0)}")
        
        return success, response

    def test_execute_sell_trade(self, ticker="AAPL", quantity=0.5, price=185.0):
        """Test executing a SELL trade"""
        success, response = self.run_test(
            f"Execute SELL Trade ({quantity} shares of {ticker})",
            "POST",
            "/api/trades",
            200,
            data={
                "stock_ticker": ticker,
                "quantity": quantity,
                "price": price,
                "trade_type": "SELL"
            }
        )
        
        if success and response:
            print(f"   ✅ Trade ID: {response.get('id', 'N/A')}")
            print(f"   ✅ Status: {response.get('status', 'N/A')}")
            print(f"   ✅ Total Value: ${response.get('total_value', 0)}")
        
        return success, response

    def test_get_trade_history(self):
        """Test getting trade history"""
        success, response = self.run_test("Get Trade History", "GET", "/api/trades", 200)
        
        if success and response:
            print(f"   ✅ Found {len(response)} trades")
            if response:
                latest_trade = response[0]
                print(f"   ✅ Latest: {latest_trade.get('trade_type', 'N/A')} {latest_trade.get('stock_ticker', 'N/A')}")
        
        return success, response

    def test_add_to_watchlist(self, ticker="MSFT"):
        """Test adding stock to watchlist"""
        success, response = self.run_test(
            f"Add to Watchlist ({ticker})",
            "POST",
            "/api/watchlist",
            200,
            data={"stock_ticker": ticker}
        )
        
        return success, response

    def test_get_watchlist(self):
        """Test getting watchlist"""
        success, response = self.run_test("Get Watchlist", "GET", "/api/watchlist", 200)
        
        if success and response:
            stocks = response.get('stocks', [])
            print(f"   ✅ Found {len(stocks)} stocks in watchlist")
            if stocks:
                print(f"   ✅ First stock: {stocks[0].get('ticker', 'N/A')}")
        
        return success, response

    def test_remove_from_watchlist(self, ticker="MSFT"):
        """Test removing stock from watchlist"""
        return self.run_test(
            f"Remove from Watchlist ({ticker})",
            "DELETE",
            f"/api/watchlist/{ticker}",
            200
        )

    def run_comprehensive_test_suite(self):
        """Run the complete test suite"""
        print("=" * 80)
        print("🚀 MONEYTEAM STOCK TRADING API - COMPREHENSIVE TEST SUITE")
        print("=" * 80)
        print(f"Testing against: {self.base_url}")
        print(f"Test user: {self.test_username}")
        print(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 80)

        # 1. Health Checks
        print("\n📋 PHASE 1: HEALTH CHECKS")
        self.test_health_check()
        self.test_api_health()

        # 2. Authentication Tests
        print("\n🔐 PHASE 2: AUTHENTICATION")
        if not self.test_user_registration():
            print("❌ Registration failed - stopping tests")
            return self.generate_summary()
        
        # Test login with the same user
        if not self.test_user_login():
            print("❌ Login failed - stopping tests")
            return self.generate_summary()
        
        self.test_get_current_user()

        # 3. Stock Data Tests
        print("\n📈 PHASE 3: STOCK DATA")
        quote_success, quote_data = self.test_stock_quote("AAPL")
        self.test_stock_fundamentals("AAPL")
        self.test_stock_chart("AAPL")
        self.test_market_movers()

        # 4. Portfolio Tests (Initial State)
        print("\n💼 PHASE 4: PORTFOLIO (INITIAL STATE)")
        portfolio_success, initial_portfolio = self.test_portfolio_summary()
        positions_success, initial_positions = self.test_portfolio_positions()

        # 5. Trading Tests
        print("\n💰 PHASE 5: TRADING")
        
        # Get current stock price for realistic trading
        stock_price = 185.0  # Default fallback
        if quote_success and quote_data:
            stock_price = quote_data.get('price', 185.0)
        
        # Execute BUY trade
        buy_success, buy_trade = self.test_execute_buy_trade("AAPL", 2.0, stock_price)
        
        # Check portfolio after buy
        if buy_success:
            print("\n   📊 Checking portfolio after BUY trade...")
            self.test_portfolio_summary()
            self.test_portfolio_positions()
        
        # Execute SELL trade (partial)
        sell_success, sell_trade = self.test_execute_sell_trade("AAPL", 1.0, stock_price)
        
        # Check portfolio after sell
        if sell_success:
            print("\n   📊 Checking portfolio after SELL trade...")
            self.test_portfolio_summary()
            self.test_portfolio_positions()

        # 6. Trade History
        print("\n📜 PHASE 6: TRADE HISTORY")
        self.test_get_trade_history()

        # 7. Watchlist Tests
        print("\n👁️ PHASE 7: WATCHLIST")
        self.test_add_to_watchlist("MSFT")
        self.test_add_to_watchlist("GOOGL")
        self.test_get_watchlist()
        self.test_remove_from_watchlist("MSFT")
        self.test_get_watchlist()  # Check after removal

        return self.generate_summary()

    def generate_summary(self):
        """Generate test summary"""
        print("\n" + "=" * 80)
        print("📊 TEST SUMMARY")
        print("=" * 80)
        
        success_rate = (self.tests_passed / self.tests_run * 100) if self.tests_run > 0 else 0
        
        print(f"Total Tests: {self.tests_run}")
        print(f"Passed: {self.tests_passed}")
        print(f"Failed: {self.tests_run - self.tests_passed}")
        print(f"Success Rate: {success_rate:.1f}%")
        
        # Categorize results
        failed_tests = [r for r in self.test_results if not r['success']]
        critical_failures = []
        minor_failures = []
        
        for test in failed_tests:
            if any(keyword in test['test'].lower() for keyword in ['registration', 'login', 'health']):
                critical_failures.append(test)
            else:
                minor_failures.append(test)
        
        if critical_failures:
            print(f"\n🚨 CRITICAL FAILURES ({len(critical_failures)}):")
            for test in critical_failures:
                print(f"   ❌ {test['test']}: {test['details']}")
        
        if minor_failures:
            print(f"\n⚠️  MINOR FAILURES ({len(minor_failures)}):")
            for test in minor_failures:
                print(f"   ❌ {test['test']}: {test['details']}")
        
        if success_rate >= 90:
            print(f"\n🎉 EXCELLENT! API is working very well ({success_rate:.1f}% success)")
            return 0
        elif success_rate >= 70:
            print(f"\n✅ GOOD! API is mostly functional ({success_rate:.1f}% success)")
            return 0
        elif success_rate >= 50:
            print(f"\n⚠️  MODERATE! API has some issues ({success_rate:.1f}% success)")
            return 1
        else:
            print(f"\n🚨 POOR! API has significant issues ({success_rate:.1f}% success)")
            return 1

def main():
    """Main test execution"""
    import argparse
    
    parser = argparse.ArgumentParser(description='MoneyTeam API Backend Tester')
    parser.add_argument('--url', default='http://localhost:8001', 
                       help='Base URL for the API (default: http://localhost:8001)')
    parser.add_argument('--verbose', action='store_true', 
                       help='Enable verbose output')
    
    args = parser.parse_args()
    
    tester = MoneyTeamAPITester(args.url)
    return tester.run_comprehensive_test_suite()

if __name__ == "__main__":
    sys.exit(main())