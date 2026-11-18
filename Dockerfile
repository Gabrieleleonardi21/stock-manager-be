# Back-end STOCK MANAGER: solo API (Spring Boot). Il frontend ha una repository propria.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /srv
COPY . .
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /srv/target/stock-manager-server-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8080
EXPOSE 8080
USER 1000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
