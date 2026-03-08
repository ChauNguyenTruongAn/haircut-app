# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy the source code
COPY src src

# Make the Gradle wrapper executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew build -x test

# Expose the port the app runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "build/libs/Haircut_Booking-0.0.1-SNAPSHOT.jar"] 