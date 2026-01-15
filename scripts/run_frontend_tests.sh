#!/bin/bash
# Run frontend tests

set -e

echo "========================================"
echo "Running MoneyTeam Frontend Tests"
echo "========================================"

cd /app/frontend

# Run Jest tests
npm test -- --watchAll=false --coverage

echo ""
echo "========================================"
echo "Frontend Tests Complete"
echo "========================================"
