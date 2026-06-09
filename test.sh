#!/bin/bash

# 1. Java 서비스 빌드 (빌드 속도를 위해 테스트 생략)
echo "Building Java services..."
cd myService
./gradlew analysis-service:bootJar -x test
cd ..

# 2. Docker 컨테이너 실행
echo "Starting Docker containers..."
cd myService
docker-compose up -d --build zookeeper kafka redis postgres crawler-service analysis-service

# 3. 서비스가 뜰 때까지 잠시 대기
echo "Waiting for services to stabilize (20s)..."
sleep 20

# 4. 크롤링 트리거 (BBC, CNN, Reuters 등 수집)
echo "Triggering global news crawl..."
curl -X POST http://localhost:8000/api/v1/crawl/all

# 5. 로그 확인 (분석 과정 모니터링)
echo "Monitoring Analysis Service Logs (Ctrl+C to stop)..."
docker logs -f analysis-service
