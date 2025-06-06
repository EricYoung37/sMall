FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /sMall

# Copy shared parent POM and module poms (all relative to root context)
COPY docker-pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY service-discoverer/pom.xml ./service-discoverer/pom.xml

# Preload dependencies (cached step)
RUN mvn -f pom.xml dependency:go-offline -DskipTests

# Copy actual source code (now that cache is primed)
COPY common ./common
COPY service-discoverer ./service-discoverer

# Install parent POM (sMall)
RUN mvn -f pom.xml clean install -N -DskipTests

# Build common (explicit install, it's a dependency)
RUN mvn -f common/pom.xml clean install -DskipTests

# Build service-discoverer
RUN mvn -f service-discoverer/pom.xml clean package -DskipTests

# === Final runtime image ===
FROM eclipse-temurin:17-alpine
WORKDIR /sMall

# Install curl for healthcheck
RUN apk add --no-cache curl

COPY --from=builder /sMall/service-discoverer/target/*.jar service-discoverer.jar
COPY .env .env
ENTRYPOINT ["java", "-jar", "service-discoverer.jar"]
