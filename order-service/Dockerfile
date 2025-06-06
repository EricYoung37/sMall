FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /sMall

# Copy shared parent POM and module poms (all relative to root context)
COPY docker-pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY order-service/pom.xml ./order-service/pom.xml

# Preload dependencies (cached step)
RUN mvn -f pom.xml dependency:go-offline -DskipTests

# Copy actual source code (now that cache is primed)
COPY common ./common
COPY order-service ./order-service

# Install parent POM (sMall)
RUN mvn -f pom.xml clean install -N -DskipTests

# Build common (explicit install, it's a dependency)
RUN mvn -f common/pom.xml clean install -DskipTests

# Build order-service
RUN mvn -f order-service/pom.xml clean package -DskipTests

# === Final runtime image ===
FROM eclipse-temurin:17-alpine
WORKDIR /sMall
COPY --from=builder /sMall/order-service/target/*.jar order-service.jar
COPY .env .env
ENTRYPOINT ["java", "-jar", "order-service.jar"]
