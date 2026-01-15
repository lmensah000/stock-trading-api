#!/bin/bash
# Run all integration tests

set -e

echo "========================================"
echo "Running MoneyTeam Integration Tests"
echo "========================================"

cd /app

# Check if backend is running
if ! curl -s http://localhost:8001/api/health > /dev/null 2>&1; then
    echo "ERROR: Backend is not running. Please start it first."
    exit 1
fi

echo ""
echo "Backend is running. Starting integration tests..."
echo ""

python -m pytest tests/integration/ -v --tb=short

echo ""
echo "========================================"
echo "Integration Tests Complete"
echo "========================================"
