#!/bin/bash

echo "Starting MSA Services..."

# 1. Eureka Server (Discovery Service) 실행
echo "1. Starting Discovery Service..."
./gradlew :discovery-service:bootRun > discovery.log 2>&1 &
sleep 15 # Eureka가 완전히 뜰 때까지 대기

# 2. Config Server 실행
echo "2. Starting Config Service..."
./gradlew :config-service:bootRun > config.log 2>&1 &
sleep 20 # Config Server가 GitHub 연동 및 구동될 때까지 넉넉히 대기

# 3. 나머지 서비스들 병렬 실행
echo "3. Starting Gateway, User, and Review Services..."
./gradlew :gateway-service:bootRun > gateway.log 2>&1 &
./gradlew :user-service:bootRun > user.log 2>&1 &
./gradlew :review-service:bootRun > review.log 2>&1 &

echo "----------------------------------------------------"
echo "All services are starting in the background."
echo "You can check the logs using: tail -f [service-name].log"
echo "To stop all services, use: ./stop-all.sh"
echo "----------------------------------------------------"
