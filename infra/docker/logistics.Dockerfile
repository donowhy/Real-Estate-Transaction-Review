FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
ARG SERVICE
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew ":${SERVICE}:bootJar" --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
ARG SERVICE
COPY --from=builder /workspace/${SERVICE}/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
