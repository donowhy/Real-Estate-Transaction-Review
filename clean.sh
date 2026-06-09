#!/bin/bash

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🧹 1. 환경 정리 시작 (컨테이너, 볼륨, 네트워크 삭제)...${NC}"
docker-compose down -v
docker rm -f $(docker ps -a -q) 2>/dev/null || true
docker network prune -f

echo -e "${BLUE}🔨 2. 모든 Java 프로젝트 빌드 시작 (이게 중요합니다!)...${NC}"
# 모든 서브 프로젝트의 bootJar를 한 번에 생성합니다.
./gradlew clean bootJar -x test

# 빌드 성공 여부 체크
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 빌드에 실패했습니다. 코드를 확인해주세요.${NC}"
    exit 1
fi

echo -e "${BLUE}🚀 3. 핵심 인프라 기동 (Discovery, Config, Zookeeper, Kafka, Redis, Postgres)...${NC}"
docker-compose up -d discovery-service config-service zookeeper kafka redis postgres

echo -e "${GREEN}⏳ 인프라 서비스 안정화 대기 중 (45초)...${NC}"
# Config Server와 Discovery가 완전히 떠야 다른 서비스들이 정상 동작합니다.
sleep 45

echo -e "${BLUE}📡 4. 메인 서비스 기동 (Crawler, Analysis)...${NC}"
docker-compose up -d --build crawler-service analysis-service

echo -e "${BLUE}🔄 5. Redis 초기화 및 크롤링 트리거...${NC}"
sleep 15
docker exec -it redis redis-cli flushall
curl -X POST http://localhost:8000/api/v1/crawl/all

echo -e "${GREEN}✅ 모든 작업이 완료되었습니다! 분석 로그를 모니터링합니다...${NC}"
docker logs -f analysis-service
