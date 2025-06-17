# 🃏 card-importer-service

Microservice Java Spring Boot pour importer toutes les cartes Yu-Gi-Oh! depuis l'API publique **YGOPRODeck** et les enregistrer dans une base **MongoDB**. Ce service est destiné à être utilisé dans un projet basé sur des microservices (par exemple, un builder de deck automatisé avec IA).

---

## 🚀 Fonctionnalités

* Connexion à l’API officielle YGOPRODeck
* Extraction de toutes les cartes du jeu
* Mapping vers des entités Java avec Jackson
* Sauvegarde dans MongoDB (NoSQL)
* Support Docker & Docker Compose
* Architecture prête pour CI/CD et microservices

---

## 🛠️ Tech Stack

* Java 21+
* Spring Boot 3
* Maven
* MongoDB
* Docker / Docker Compose
* GitHub Actions (prévu pour CI/CD)

---

## 📁 Arborescence du projet

```
card-importer-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/yugibuilder/cardimporter/
│   │   │       ├── CardImporterApplication.java
│   │   │       ├── config/
│   │   │       │   └── YgoProDeckProperties.java
│   │   │       ├── model/
│   │   │       │   └── YugiohCard.java
│   │   │       ├── repository/
│   │   │       │   └── YugiohCardRepository.java
│   │   │       └── service/
│   │   │           └── CardImporterService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── ...
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## ⚙️ Configuration

### `application.properties`

```properties
server.port=8080
spring.data.mongodb.uri=mongodb://mongo:27017/yugioh
ygoprodeck.api.url=https://db.ygoprodeck.com/api/v7/cardinfo.php
```

---

## ▶️ Lancer localement

### Avec Docker 🐳

```bash
docker-compose up --build
```

Cela démarre :

* le microservice Spring Boot
* une base MongoDB avec le volume monté

---

## 🧪 Tester les cartes importées

Ouvre un terminal :

```bash
docker exec -it <mongo_container_id> mongosh
```

Puis :

```js
use yugioh
db.cards.find().limit(5).pretty()
db.cards.countDocuments()
```

---

## 🧬 Exemple d'appel manuel dans le code

Dans `CardImporterApplication.java`, ajoute :

```java
@SpringBootApplication
public class CardImporterApplication implements CommandLineRunner {
    @Autowired
    private CardImporterService importer;

    public static void main(String[] args) {
        SpringApplication.run(CardImporterApplication.class, args);
    }

    @Override
    public void run(String... args) {
        importer.importAllCards();
    }
}
```

---

## 📦 Exemple de Dockerfile

```dockerfile
# Build
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Exécution
FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 🧱 docker-compose.yml

```yaml
version: '3.8'

services:
  card-importer-service:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mongo
    environment:
      - SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/yugioh

  mongo:
    image: mongo:6
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db

volumes:
  mongo_data:
```

---

## 🔁 CI/CD (GitHub Actions)

### Exemple de `.github/workflows/ci.yml` :

```yaml
name: CI Build

on:
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repo
        uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean verify
```

---

## 📌 À faire

* [ ] Ajouter Swagger/OpenAPI
* [ ] Déclencher import via requête REST
* [ ] Gérer la mise à jour des cartes existantes
* [ ] Séparer les images et sets dans d’autres collections

---

## 📜 Licence

Projet open-source sous [MIT](LICENSE).