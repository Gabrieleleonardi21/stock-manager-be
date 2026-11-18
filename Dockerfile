# Immagine unica sullo stesso dominio: il front-end viene scaricato dalla sua repository pubblica,
# compilato e inserito tra le risorse statiche del server. Niente CORS, cookie SameSite=Strict invariato.

# 1) Front-end: clone della repository pubblica e build
FROM node:24-alpine AS web
RUN apk add --no-cache git
ARG FE_REPO=https://github.com/Gabrieleleonardi21/stock-manager-fe.git
ARG FE_REF=main
WORKDIR /fe
RUN git clone --depth 1 --branch "$FE_REF" "$FE_REPO" .
RUN npm ci && npm run build

# 2) Server: build del jar con l'app dentro
FROM eclipse-temurin:25-jdk AS build
WORKDIR /srv
COPY . .
COPY --from=web /fe/dist ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

# 3) Runtime minimo, utente non root
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /srv/target/stock-manager-server-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8080
EXPOSE 8080
USER 1000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
