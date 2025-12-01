# Etapa 1: construir el JAR usando Maven y Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos solo lo necesario para compilar
COPY pom.xml .
COPY src ./src

# Compilamos el proyecto (sin tests)
RUN mvn clean package -DskipTests

# Etapa 2: imagen final, solo con el JAR
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos el .jar que se generó en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Render te pasa el puerto en la variable PORT
ENV PORT=8080
EXPOSE 8080

# Spring Boot usará el puerto configurado en application.properties (PORT)
ENTRYPOINT ["java", "-jar", "app.jar"]
