# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .

# Build and package without tests
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Optional: allow dynamic profile + JVM opts
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=main

EXPOSE 8080

# ✅ Use shell form ENTRYPOINT to allow proper env variable substitution
ENTRYPOINT sh -c 'java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar'
