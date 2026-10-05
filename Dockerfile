# ----------- Stage 1: Build the application -------------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN for i in 1 2 3 4 5; do \
      mvn -B clean package -DskipTests && break; \
      echo "Maven build failed. Retrying in 10 seconds..."; \
      sleep 10; \
    done

# ----------- Stage 2: Run the application ---------------
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
