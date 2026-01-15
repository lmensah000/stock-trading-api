#!/bin/bash
# Run all backend tests

set -e

echo "========================================"
echo "Running MoneyTeam Backend Tests"
echo "========================================"

# Ensure we're in the right directory
cd /app

# Check if backend is running
if ! curl -s http://localhost:8001/api/health > /dev/null 2>&1; then
    echo "ERROR: Backend is not running. Please start it first."
    echo "Run: sudo supervisorctl restart backend"
    exit 1
fi

echo ""
echo "Backend is running. Starting tests..."
echo ""

# Run pytest with coverage
python -m pytest tests/backend/ -v --tb=short

echo ""
echo "========================================"
echo "Backend Tests Complete"
echo "========================================"
