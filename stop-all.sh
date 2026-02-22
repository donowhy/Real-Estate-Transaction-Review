#!/bin/bash

echo "Stopping all MSA Services..."

# Gradle bootRun 프로세스를 찾아 죽임
pkill -f "org.micro.myservice"

# 8080 (Gateway), 8761 (Eureka), 8888 (Config), 8081 (User), 8082 (Review) 포트를 확인하고 죽임
ports=(8080 8761 8888 8081 8082)

for port in "${ports[@]}"
do
    pid=$(lsof -t -i:$port)
    if [ -n "$pid" ]; then
        echo "Cleaning up port $port (PID: $pid)..."
        kill -9 $pid
    fi
done

echo "All services stopped."
