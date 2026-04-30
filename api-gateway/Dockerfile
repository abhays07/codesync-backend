FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
