# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/safe-sql-lint-1.0.0-SNAPSHOT.jar /app/safe-sql-lint.jar
ENTRYPOINT ["java", "-jar", "/app/safe-sql-lint.jar"]
CMD ["--help"]
