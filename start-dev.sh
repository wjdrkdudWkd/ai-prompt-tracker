#!/bin/bash

echo "Starting AI Prompt Tracker Development Environment..."

# Start PostgreSQL and Redis
echo "Starting PostgreSQL and Redis..."
cd docker
docker-compose up -d postgres redis

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 5

# Check if services are healthy
docker-compose ps

echo ""
echo "Development services are ready!"
echo ""
echo "PostgreSQL: localhost:5432"
echo "  Database: ai_prompt_tracker"
echo "  User: postgres"
echo "  Password: postgres"
echo ""
echo "Redis: localhost:6379"
echo ""
echo "To start the backend:"
echo "  cd backend && ./gradlew bootRun"
echo ""
echo "To start the frontend:"
echo "  cd frontend && npm install && npm run dev"
echo ""
echo "To stop services:"
echo "  cd docker && docker-compose down"
