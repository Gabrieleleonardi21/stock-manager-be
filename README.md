> **Materiale di Gabriele Leonardi.** Tutti i diritti riservati, vedi [LICENSE](LICENSE).

# STOCK MANAGER — back-end

API Spring Boot 4.1.1 (Java 25) per il gestionale di magazzino. Database PostgreSQL (Neon).

- Autenticazione con sessione server-side (cookie `__Host-sid`), CSRF, controllo Origin, rate limit.
- Dati per proprietario (magazzini, articoli, movimenti), regola della giacenza sul server.
- Assistente AI proxy (`POST /api/assistente`), report mensili con CSV protetto.

Avvio locale: crea `.env` da `.env.example` e lancia `./mvnw spring-boot:run` (porta 8080).
Test: `scripts/test-dati.sh` con il server avviato.
Piano e decisioni: `docs/PIANO-BACKEND.md`.

## Deploy

Un solo servizio su Render (`render.yaml`): il Dockerfile compila il front-end dalla repository pubblica
[stock-manager-fe](https://github.com/Gabrieleleonardi21/stock-manager-fe) e lo serve insieme alle API.
Dopo il primo deploy imposta `ALLOWED_ORIGIN` con l'URL pubblico del servizio e riavvia.
