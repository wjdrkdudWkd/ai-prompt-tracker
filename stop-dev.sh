#!/bin/bash

echo "Stopping AI Prompt Tracker Development Environment..."

cd docker
docker-compose down

echo "Development services stopped!"
