# =======================
# Étape 1 : Build
# =======================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copier pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Build l'application (skip tests pour build plus rapide)
RUN mvn clean package -DskipTests

# Renommer le fichier pour simplifier
RUN mv /app/target/*.war /app/target/app.war

# =======================
# Étape 2 : Runtime
# =======================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copier le WAR depuis l'étape de build
COPY --from=build /app/target/app.war app.war

# Exposer le port
EXPOSE 8081

# Variable d'environnement pour Java
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Commande de démarrage
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.war"]