FROM amazoncorretto:21-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-Deureka.client.service-url.defaultZone=http://discovery-service:8761/eureka/", "-jar", "app.jar"]
