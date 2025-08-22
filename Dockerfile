# Dockerfile optimisé pour Spring Boot avec multi-stage builds
# Basé sur votre Dockerfile existant mais avec des améliorations pour CI/CD

# ==========================================
# Stage 1: Build Dependencies Cache
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS dependencies

LABEL stage=dependencies
LABEL maintainer="votre-email@domaine.com"

# Installation des outils système nécessaires
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata \
    && rm -rf /var/cache/apk/*

# Configuration Maven avec optimisations pour CI
ENV MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository -Xmx512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ENV MAVEN_CONFIG=/root/.m2

WORKDIR /app

# Configuration Maven pour timeouts optimisés
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

# Copie des fichiers de configuration Maven seulement
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
COPY mvnw.cmd .

# Permissions pour Maven wrapper
RUN chmod +x mvnw

# Téléchargement des dépendances avec cache
RUN ./mvnw dependency:go-offline -B || \
    ./mvnw dependency:resolve dependency:resolve-sources -B

# ==========================================
# Stage 2: Build Application
# ==========================================
FROM dependencies AS build

LABEL stage=builder

# Copie du code source
COPY src/ src/

# Build de l'application avec tests désactivés (tests dans CI)
RUN ./mvnw clean package -DskipTests -B \
    && ls -la target/ \
    && echo "Build completed successfully"

# ==========================================
# Stage 3: Runtime Environment
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="Spring Boot MongoDB App"
LABEL org.opencontainers.image.description="Application Spring Boot avec MongoDB et Java 21"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="votre-email@domaine.com"

# Installation des outils runtime nécessaires
RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata \
    && rm -rf /var/cache/apk/*

# Configuration timezone
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Création utilisateur non-root pour sécurité
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Création répertoires application
RUN mkdir -p /app/logs /app/config && \
    chown -R appuser:appgroup /app

# Variables d'environnement optimisées
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# Copie du JAR depuis le stage de build
COPY --from=build --chown=appuser:appgroup /app/target/*.jar /app/app.jar

# Passage à l'utilisateur non-root
USER appuser

WORKDIR /app

# Exposition du port
EXPOSE 8080

# Health check optimisé
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Point d'entrée avec dumb-init
ENTRYPOINT ["dumb-init", "--"]

# Commande de démarrage
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ==========================================
# Stage 4: Development (optionnel)
# ==========================================
FROM build AS development

LABEL stage=development

# Outils de développement
RUN apk add --no-cache \
    git \
    vim \
    less

# Configuration pour le développement
ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_DEVTOOLS_RESTART_ENABLED=true
ENV JAVA_OPTS="-Xms128m -Xmx256m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Port de debug
EXPOSE 5005

# Volume pour hot reload
VOLUME ["/app/src", "/app/target"]

CMD ["sh", "-c", "java $JAVA_OPTS -jar target/*.jar"]