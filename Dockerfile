# Stage 1: Build the application using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy Maven descriptor and project files
COPY pom.xml .
COPY src ./src

# Build production JAR without running tests
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight runtime environment with Java 21 JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create directories for persistent storage (H2 database and video uploads)
RUN mkdir -p /app/data /app/vlearn-uploads/videos

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Render automatically provides the PORT environment variable (defaults to 8080 locally)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot application binding to Render's dynamic PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
