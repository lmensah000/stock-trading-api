import pytest
import asyncio
from httpx import AsyncClient
from server import app
import os

# Test configuration
TEST_USER_EMAIL = "testuser@innate.fitness"
TEST_USER_PASSWORD = "testpass123"
TEST_USER_NAME = "Test User"

@pytest.fixture(scope="session")
def event_loop():
    """Create event loop for async tests"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()

@pytest.fixture
async def client():
    """Create test client"""
    async with AsyncClient(app=app, base_url="http://test") as ac:
        yield ac

@pytest.fixture
async def auth_headers(client):
    """Create authenticated user and return headers"""
    # Register user
    register_data = {
        "email": TEST_USER_EMAIL,
        "name": TEST_USER_NAME,
        "password": TEST_USER_PASSWORD
    }
    response = await client.post("/api/auth/register", json=register_data)
    
    if response.status_code == 200:
        data = response.json()
        token = data["access_token"]
        return {"Authorization": f"Bearer {token}"}
    else:
        # User might already exist, try login
        login_data = {
            "email": TEST_USER_EMAIL,
            "password": TEST_USER_PASSWORD
        }
        response = await client.post("/api/auth/login", json=login_data)
        data = response.json()
        token = data["access_token"]
        return {"Authorization": f"Bearer {token}"}

class TestAuthentication:
    """Test authentication endpoints"""
    
    @pytest.mark.asyncio
    async def test_register_new_user(self, client):
        """Test user registration"""
        import uuid
        unique_email = f"newuser{uuid.uuid4().hex[:8]}@test.com"
        
        response = await client.post("/api/auth/register", json={
            "email": unique_email,
            "name": "New User",
            "password": "password123"
        })
        
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert data["user"]["email"] == unique_email
        assert "level" in data["user"]
        assert data["user"]["level"] == 1
        assert "referral_code" in data["user"]
    
    @pytest.mark.asyncio
    async def test_login_existing_user(self, client):
        """Test user login"""
        response = await client.post("/api/auth/login", json={
            "email": TEST_USER_EMAIL,
            "password": TEST_USER_PASSWORD
        })
        
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert data["user"]["email"] == TEST_USER_EMAIL
    
    @pytest.mark.asyncio
    async def test_get_current_user(self, client, auth_headers):
        """Test getting current user info"""
        response = await client.get("/api/auth/me", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert data["email"] == TEST_USER_EMAIL

class TestMealPlanning:
    """Test meal planning endpoints"""
    
    @pytest.mark.asyncio
    async def test_generate_meal_plan(self, client, auth_headers):
        """Test AI meal plan generation"""
        response = await client.post(
            "/api/meals/generate",
            headers=auth_headers,
            json={
                "groceries": [
                    {"id": "1", "name": "Chicken", "quantity": "500", "unit": "g"},
                    {"id": "2", "name": "Rice", "quantity": "200", "unit": "g"}
                ],
                "preferences": "High protein"
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert "meal_name" in data or "raw_response" in data
    
    @pytest.mark.asyncio
    async def test_get_meal_plans(self, client, auth_headers):
        """Test retrieving meal plans"""
        response = await client.get("/api/meals", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

class TestGoals:
    """Test goals endpoints"""
    
    @pytest.mark.asyncio
    async def test_create_goal(self, client, auth_headers):
        """Test goal creation"""
        response = await client.post(
            "/api/goals",
            headers=auth_headers,
            json={
                "title": "Lose 5kg",
                "description": "Weight loss goal",
                "target_value": 5,
                "unit": "kg"
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["title"] == "Lose 5kg"
        assert data["target_value"] == 5
    
    @pytest.mark.asyncio
    async def test_get_goals(self, client, auth_headers):
        """Test retrieving goals"""
        response = await client.get("/api/goals", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

class TestWorkouts:
    """Test workout endpoints"""
    
    @pytest.mark.asyncio
    async def test_log_workout(self, client, auth_headers):
        """Test logging a workout"""
        response = await client.post(
            "/api/workouts/log",
            headers=auth_headers,
            json={
                "workout_type": "Running",
                "duration_minutes": 30,
                "calories_burned": 300
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["workout_type"] == "Running"
        assert data["points_earned"] > 0
    
    @pytest.mark.asyncio
    async def test_get_workout_logs(self, client, auth_headers):
        """Test retrieving workout logs"""
        response = await client.get("/api/workouts/logs", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

class TestGamification:
    """Test gamification features"""
    
    @pytest.mark.asyncio
    async def test_get_levels(self, client):
        """Test getting level information"""
        response = await client.get("/api/levels")
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 8
        assert data[0]["level"] == 1
        assert data[0]["name"] == "Seedling"
    
    @pytest.mark.asyncio
    async def test_get_badges(self, client):
        """Test getting badge information"""
        response = await client.get("/api/badges")
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 9
    
    @pytest.mark.asyncio
    async def test_get_user_progress(self, client, auth_headers):
        """Test getting user progress"""
        response = await client.get("/api/my/progress", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert "level" in data
        assert "xp" in data
        assert "avatar_stage" in data
        assert "badges" in data
    
    @pytest.mark.asyncio
    async def test_complete_onboarding(self, client, auth_headers):
        """Test completing onboarding"""
        response = await client.post("/api/onboarding/complete", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert "points_earned" in data or "already completed" in data["message"].lower()

class TestReferrals:
    """Test referral system"""
    
    @pytest.mark.asyncio
    async def test_get_my_referrals(self, client, auth_headers):
        """Test getting user's referrals"""
        response = await client.get("/api/referrals/my", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert "referral_code" in data
        assert "total_referrals" in data
        assert len(data["referral_code"]) > 0

class TestShop:
    """Test shop and rewards"""
    
    @pytest.mark.asyncio
    async def test_get_shop_items(self, client, auth_headers):
        """Test getting shop items"""
        response = await client.get("/api/shop/items", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
    
    @pytest.mark.asyncio
    async def test_get_points_balance(self, client, auth_headers):
        """Test getting points balance"""
        response = await client.get("/api/points/balance", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert "points" in data

class TestGroupGoals:
    """Test group goals"""
    
    @pytest.mark.asyncio
    async def test_create_group_goal(self, client, auth_headers):
        """Test creating a group goal"""
        response = await client.post(
            "/api/groups",
            headers=auth_headers,
            json={
                "name": "Team Fitness Challenge",
                "description": "100km running together",
                "target_value": 100,
                "unit": "km"
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "Team Fitness Challenge"
    
    @pytest.mark.asyncio
    async def test_get_group_goals(self, client, auth_headers):
        """Test retrieving group goals"""
        response = await client.get("/api/groups", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

class TestWeightTracking:
    """Test weight tracking"""
    
    @pytest.mark.asyncio
    async def test_add_weight_entry(self, client, auth_headers):
        """Test adding weight entry"""
        response = await client.post(
            "/api/weight",
            headers=auth_headers,
            json={
                "weight": 75.5,
                "unit": "kg"
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data["weight"] == 75.5
    
    @pytest.mark.asyncio
    async def test_get_weight_entries(self, client, auth_headers):
        """Test retrieving weight entries"""
        response = await client.get("/api/weight", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

class TestDashboard:
    """Test dashboard stats"""
    
    @pytest.mark.asyncio
    async def test_get_dashboard_stats(self, client, auth_headers):
        """Test getting dashboard statistics"""
        response = await client.get("/api/dashboard/stats", headers=auth_headers)
        
        assert response.status_code == 200
        data = response.json()
        assert "active_goals" in data
        assert "points" in data
        assert "total_workouts" in data