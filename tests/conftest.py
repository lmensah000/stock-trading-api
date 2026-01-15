"""Shared pytest fixtures and configuration for all tests."""
import pytest
import asyncio
import httpx
import sys
import os

# Add backend to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'backend'))

from motor.motor_asyncio import AsyncIOMotorClient

# Test configuration
TEST_API_URL = "http://localhost:8001"
TEST_MONGO_URL = os.environ.get("MONGO_URL", "mongodb://localhost:27017")
TEST_DB_NAME = "moneyteam_test"

@pytest.fixture(scope="session")
def event_loop():
    """Create an event loop for async tests."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()

@pytest.fixture(scope="session")
async def mongo_client():
    """Create MongoDB client for tests."""
    client = AsyncIOMotorClient(TEST_MONGO_URL)
    yield client
    client.close()

@pytest.fixture(scope="session")
async def test_db(mongo_client):
    """Get test database."""
    db = mongo_client[TEST_DB_NAME]
    yield db
    # Cleanup after all tests
    await mongo_client.drop_database(TEST_DB_NAME)

@pytest.fixture
async def http_client():
    """Create async HTTP client for API tests."""
    async with httpx.AsyncClient(base_url=TEST_API_URL, timeout=30.0) as client:
        yield client

@pytest.fixture
def sync_http_client():
    """Create sync HTTP client for simple tests."""
    with httpx.Client(base_url=TEST_API_URL, timeout=30.0) as client:
        yield client

@pytest.fixture
async def auth_headers(http_client):
    """Create authenticated user and return headers with JWT token."""
    import uuid
    username = f"testuser_{uuid.uuid4().hex[:8]}"
    
    # Register user
    response = await http_client.post("/api/auth/register", json={
        "username": username,
        "email": f"{username}@test.com",
        "password": "testpass123"
    })
    
    if response.status_code == 200:
        token = response.json()["access_token"]
        return {"Authorization": f"Bearer {token}"}
    
    # If registration fails (user exists), try login
    response = await http_client.post("/api/auth/login", json={
        "username": username,
        "password": "testpass123"
    })
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

@pytest.fixture
def test_user_data():
    """Generate unique test user data."""
    import uuid
    unique_id = uuid.uuid4().hex[:8]
    return {
        "username": f"testuser_{unique_id}",
        "email": f"testuser_{unique_id}@test.com",
        "password": "securepass123"
    }

@pytest.fixture
def sample_trade_data():
    """Sample trade data for testing."""
    return {
        "stock_ticker": "AAPL",
        "quantity": 5,
        "price": 185.92,
        "trade_type": "BUY"
    }
