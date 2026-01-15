#!/bin/bash
# Run all tests (backend + integration)

set -e

echo "========================================"
echo "Running All MoneyTeam Tests"
echo "========================================"

cd /app

# Check if backend is running
if ! curl -s http://localhost:8001/api/health > /dev/null 2>&1; then
    echo "ERROR: Backend is not running. Please start it first."
    exit 1
fi

echo ""
echo "Running all tests..."
echo ""

python -m pytest tests/ -v --tb=short

echo ""
echo "========================================"
echo "All Tests Complete"
echo "========================================"
