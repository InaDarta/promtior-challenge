# syntax=docker/dockerfile:1

# --- Stage 1: frontend (React + Vite) ---
FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- Stage 2: backend build (Maven + JDK) ---
FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw
RUN --mount=type=cache,id=m2,target=/root/.m2 ./mvnw dependency:go-offline -B
COPY src/ src/
COPY --from=frontend-build /frontend/dist/ src/main/resources/static/
RUN --mount=type=cache,id=m2,target=/root/.m2 ./mvnw package -B -DskipTests

# --- Stage 3: runtime (JRE only, non-root) ---
FROM eclipse-temurin:25-jre AS runtime
RUN addgroup --system booking && adduser --system --ingroup booking booking
WORKDIR /app
COPY --from=build --chown=booking:booking /app/target/*.jar app.jar
USER booking

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
