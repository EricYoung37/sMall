FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /sMall

# Copy shared parent POM and module poms (all relative to root context)
COPY docker-pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

# Preload dependencies (cached step)
RUN mvn -f pom.xml dependency:go-offline -DskipTests

# Copy actual source code (now that cache is primed)
COPY common ./common
COPY auth-service ./auth-service

# Install parent POM (sMall)
RUN mvn -f pom.xml clean install -N -DskipTests

# Build common (explicit install, it's a dependency)
RUN mvn -f common/pom.xml clean install -DskipTests

# Build auth-service
RUN mvn -f auth-service/pom.xml clean package -DskipTests

# === Final runtime image ===
FROM eclipse-temurin:17-alpine
WORKDIR /sMall
COPY --from=builder /sMall/auth-service/target/*.jar auth-service.jar
COPY .env .env
COPY auth-service/.env ./auth-service/.env
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
