FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY market-app/pom.xml ./market-app/
COPY payment-service/pom.xml ./payment-service/
COPY payment-api/pom.xml ./payment-api/
RUN mvn dependency:go-offline

COPY market-app/src ./market-app/src
COPY payment-service/src ./payment-service/src
COPY payment-api/src ./payment-api/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN mkdir -p /app/logs

COPY --from=build /app/market-app/target/market-app-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]