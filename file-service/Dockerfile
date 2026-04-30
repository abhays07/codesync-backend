FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx192m", "-Xms128m", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
