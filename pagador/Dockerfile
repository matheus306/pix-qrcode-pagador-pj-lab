# syntax=docker/dockerfile:1
# Build a partir da raiz do repositório: docker compose cuida disso.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl pagador -am package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=build /src/pagador/target/pagador-0.1.0.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
