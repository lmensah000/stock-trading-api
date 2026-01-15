"""Authentication endpoint tests."""
import pytest
import httpx
import uuid

TEST_API_URL = "http://localhost:8001"


class TestUserRegistration:
    """Test suite for user registration."""
    
    @pytest.mark.asyncio
    async def test_register_new_user(self, http_client, test_user_data):
        """Test successful user registration."""
        response = await http_client.post("/api/auth/register", json=test_user_data)
        assert response.status_code == 200
        data = response.json()
        
        assert "access_token" in data
        assert data["token_type"] == "bearer"
        assert "user" in data
        assert data["user"]["username"] == test_user_data["username"]
        assert data["user"]["email"] == test_user_data["email"]
    
    @pytest.mark.asyncio
    async def test_register_duplicate_username(self, http_client, test_user_data):
        """Test registration fails with duplicate username."""
        # First registration
        await http_client.post("/api/auth/register", json=test_user_data)
        
        # Second registration with same username
        response = await http_client.post("/api/auth/register", json=test_user_data)
        assert response.status_code == 400
        assert "already registered" in response.json()["detail"].lower()
    
    @pytest.mark.asyncio
    async def test_register_invalid_email(self, http_client):
        """Test registration fails with invalid email."""
        response = await http_client.post("/api/auth/register", json={
            "username": f"user_{uuid.uuid4().hex[:8]}",
            "email": "invalid-email",
            "password": "password123"
        })
        assert response.status_code == 422  # Validation error
    
    @pytest.mark.asyncio
    async def test_register_short_password(self, http_client):
        """Test registration fails with short password."""
        response = await http_client.post("/api/auth/register", json={
            "username": f"user_{uuid.uuid4().hex[:8]}",
            "email": "test@example.com",
            "password": "123"  # Too short
        })
        assert response.status_code == 422
    
    @pytest.mark.asyncio
    async def test_register_starting_balance(self, http_client, test_user_data):
        """Test new user gets $100,000 starting balance."""
        response = await http_client.post("/api/auth/register", json=test_user_data)
        token = response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        # Check portfolio summary
        portfolio = await http_client.get("/api/portfolio/summary", headers=headers)
        data = portfolio.json()
        assert data["cash_balance"] == 100000.0


class TestUserLogin:
    """Test suite for user login."""
    
    @pytest.mark.asyncio
    async def test_login_success(self, http_client, test_user_data):
        """Test successful login."""
        # Register first
        await http_client.post("/api/auth/register", json=test_user_data)
        
        # Login
        response = await http_client.post("/api/auth/login", json={
            "username": test_user_data["username"],
            "password": test_user_data["password"]
        })
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert data["user"]["username"] == test_user_data["username"]
    
    @pytest.mark.asyncio
    async def test_login_wrong_password(self, http_client, test_user_data):
        """Test login fails with wrong password."""
        # Register first
        await http_client.post("/api/auth/register", json=test_user_data)
        
        # Login with wrong password
        response = await http_client.post("/api/auth/login", json={
            "username": test_user_data["username"],
            "password": "wrongpassword"
        })
        assert response.status_code == 401
    
    @pytest.mark.asyncio
    async def test_login_nonexistent_user(self, http_client):
        """Test login fails for non-existent user."""
        response = await http_client.post("/api/auth/login", json={
            "username": "nonexistent_user_12345",
            "password": "anypassword"
        })
        assert response.status_code == 401


class TestAuthenticatedEndpoints:
    """Test suite for authenticated endpoint access."""
    
    @pytest.mark.asyncio
    async def test_get_current_user(self, http_client, auth_headers):
        """Test getting current user info."""
        response = await http_client.get("/api/auth/me", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert "id" in data
        assert "username" in data
        assert "email" in data
    
    @pytest.mark.asyncio
    async def test_access_without_token(self, http_client):
        """Test protected endpoint without token returns 403."""
        response = await http_client.get("/api/auth/me")
        assert response.status_code == 403
    
    @pytest.mark.asyncio
    async def test_access_with_invalid_token(self, http_client):
        """Test protected endpoint with invalid token returns 401."""
        headers = {"Authorization": "Bearer invalid_token_here"}
        response = await http_client.get("/api/auth/me", headers=headers)
        assert response.status_code == 401
