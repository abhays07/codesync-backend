# Step 1: Use an image with Java 17 to run the application
FROM eclipse-temurin:17-jre-alpine

# Step 2: Set the working directory inside the container
WORKDIR /app

# Step 3: Copy the compiled JAR file from the target folder to the container
# Note: You must run 'mvn clean package' locally before building the image
COPY target/*.jar app.jar

# Step 4: Expose the port the service runs on
EXPOSE 8081

# Step 5: Command to run the application
# Step 5: Command to run the application with memory optimization
# -Xmx200m: Limits maximum memory to 200MB
# -XX:TieredStopAtLevel=1: Speeds up startup for small services
ENTRYPOINT ["java", "-Xmx200m", "-Xms100m", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
