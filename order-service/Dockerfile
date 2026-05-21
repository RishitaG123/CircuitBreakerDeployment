FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copy already-built JAR from root build output
COPY order-service/target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]