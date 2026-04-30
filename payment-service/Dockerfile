FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/payment-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-Xmx192m", "-Xms128m", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
