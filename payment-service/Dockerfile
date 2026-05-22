FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copy already-built JAR from root build output
COPY payment-service/target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]