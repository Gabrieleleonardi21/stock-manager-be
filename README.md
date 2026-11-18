# STOCK MANAGER — back-end

API Spring Boot 4.1.1 (Java 25) per il gestionale di magazzino. Database PostgreSQL (Neon).

- Autenticazione con sessione server-side (cookie `__Host-sid`), CSRF, controllo Origin, rate limit.
- Dati per proprietario (magazzini, articoli, movimenti), regola della giacenza sul server.
- Assistente AI proxy (`POST /api/assistente`), report mensili con CSV protetto.

Avvio locale: crea `.env` da `.env.example` e lancia `./mvnw spring-boot:run` (porta 8080).
Test: `scripts/test-dati.sh` con il server avviato.
Piano e decisioni: `docs/PIANO-BACKEND.md`.
