"""Health check endpoint tests."""
import pytest
import httpx

TEST_API_URL = "http://localhost:8001"


class TestHealthEndpoints:
    """Test suite for health check endpoints."""
    
    def test_root_endpoint(self, sync_http_client):
        """Test root endpoint returns API info."""
        response = sync_http_client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert "MoneyTeam" in data["message"]
        assert data["status"] == "running"
    
    def test_health_endpoint(self, sync_http_client):
        """Test health endpoint returns healthy status."""
        response = sync_http_client.get("/api/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "timestamp" in data
    
    def test_health_endpoint_returns_valid_timestamp(self, sync_http_client):
        """Test health endpoint returns valid ISO timestamp."""
        from datetime import datetime
        response = sync_http_client.get("/api/health")
        data = response.json()
        # Should not raise exception
        datetime.fromisoformat(data["timestamp"])
