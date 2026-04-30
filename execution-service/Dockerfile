FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache docker-cli

COPY target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]