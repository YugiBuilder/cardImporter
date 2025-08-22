# Dockerfile multi-stage optimisé pour Spring Boot + Java 21
# Version corrigée pour résoudre les problèmes de timeout Maven et versions Alpine

FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Métadonnées
LABEL stage=builder
LABEL maintainer="votre-email@domaine.com"
LABEL description="Build stage pour application Spring Boot Java 21"

# Variables d'environnement pour Maven avec optimisations de connexion
ENV MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository -Xmx512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.http.retryHandler.count=5"
ENV MAVEN_CONFIG=/root/.m2

# Installer APR et les dépendances natives
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata \
    apr \
    apr-dev \
    apr-util \
    apr-util-dev \
    && rm -rf /var/cache/apk/*

# Création du répertoire de travail
WORKDIR /app

# Création d'un fichier settings.xml avec configuration de timeout
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>central</id>
      <configuration>
        <httpConfiguration>
          <all>
            <connectionTimeout>300000</connectionTimeout>
            <readTimeout>600000</readTimeout>
          </all>
        </httpConfiguration>
      </configuration>
    </server>
  </servers>
</settings>
EOF

# Copie des fichiers de configuration Maven
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
COPY mvnw.cmd .

# Test de connectivité réseau avant le téléchargement des dépendances
RUN echo "Test de connectivité vers Maven Central..." && \
    curl -f --connect-timeout 30 --max-time 60 https://repo.maven.apache.org/maven2/ || \
    echo "ATTENTION: Problème de connectivité vers Maven Central"

# Téléchargement des dépendances avec retry et timeouts étendus
RUN mvn dependency:resolve dependency:resolve-sources -B \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
    -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.pool=false \
    -Dhttp.keepAlive=false \
    || mvn dependency:go-offline -B \
       -Dmaven.wagon.http.ssl.insecure=true \
       -Dmaven.wagon.http.ssl.allowall=true \
       -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
       -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
       -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
       -Dmaven.wagon.http.retryHandler.count=3

# Copie du code source
COPY src/ src/

# Build de l'application
RUN mvn clean package -DskipTests -B \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
    && rm -rf /root/.m2/repository/com/votre/package \
    && ls -la target/

# ==========================================
# Stage 2: Runtime optimisé
# ==========================================

FROM eclipse-temurin:21-jre-alpine AS runtime

# Métadonnées de l'image finale
LABEL org.opencontainers.image.title="Spring Boot MongoDB App"
LABEL org.opencontainers.image.description="Application Spring Boot avec MongoDB et Java 21"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="votre-email@domaine.com"
LABEL org.opencontainers.image.source="https://github.com/votre-org/votre-repo"

# Installation des packages nécessaires et nettoyage (sans versions spécifiques)
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata \
    && rm -rf /var/cache/apk/*

# Configuration du timezone
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Création des répertoires nécessaires
RUN mkdir -p /app/logs /app/config && \
    chown -R appuser:appgroup /app

# Variables d'environnement pour l'application
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# Copie du JAR depuis l'étape de build
COPY --from=build --chown=appuser:appgroup /app/target/*.jar /app/app.jar

# Passage à l'utilisateur non-root
USER appuser

# Répertoire de travail
WORKDIR /app

# Exposition du port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Point d'entrée avec dumb-init pour un signal handling correct
ENTRYPOINT ["dumb-init", "--"]

# Commande de démarrage
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]