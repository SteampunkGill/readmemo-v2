FROM maven:3.8.5-eclipse-temurin-8 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/gateway-service/target/*.jar gateway-service.jar
COPY --from=build /app/backend/auth-service/target/*.jar auth-service.jar
COPY --from=build /app/backend/document-service/target/*.jar document-service.jar
COPY --from=build /app/backend/study-service/target/*.jar study-service.jar
COPY --from=build /app/backend/user-service/target/*.jar user-service.jar

# ENTRYPOINT removed to allow command override in docker-compose