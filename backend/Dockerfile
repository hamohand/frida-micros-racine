# ===== Stage 1: Build Spring Boot =====
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier le fichier pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler l'application
RUN mvn clean package -DskipTests -q

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Installer curl pour les healthchecks
USER root
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
USER spring:spring

# Copier le JAR (sois plus spécifique si possible sur le nom du JAR)
COPY --from=builder /app/target/backendAi-*.jar app.jar

# Variables d'environnement par défaut (Spring les reconnaîtra automatiquement)
ENV SERVER_PORT=8080
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fridaaidb
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

EXPOSE ${SERVER_PORT}

# Healthcheck corrigé pour utiliser la variable d'env
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Lancer l'application (les variables ENV sont lues par Spring)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]