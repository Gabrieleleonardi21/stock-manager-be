# Piano back-end — STOCK MANAGER

Obiettivo: portare auth, dati e chiamate AI sul server, con la sicurezza più alta possibile.
Stack scelto per coerenza con il progetto di riferimento `BUILD-WEEK-5` (stesso team e stesse convenzioni).

## 0. Stack e convenzioni

| Area | Scelta | Motivo |
|---|---|---|
| Framework | Spring Boot 4.1.1, Java 25 | come `BUILD-WEEK-5` |
| Database | PostgreSQL 17 | come `BUILD-WEEK-5` |
| Sessione | `HttpSession` + Spring Session JDBC | decisione D01: niente JWT |
| CSRF | attivo, `CookieCsrfTokenRepository` + header `X-XSRF-TOKEN` | decisione del team |
| Password | BCrypt 12 (`PasswordEncoder`) | decisione del team |
| Schema DB | `db/schema.sql` come fonte di verità, `ddl-auto=validate` | decisione D17, niente Flyway |
| Id | `UUID` con `GenerationType.UUID` | decisione D02 |
| Date | `OffsetDateTime` | decisione D18 |
| Errori | `ProblemDetail` (RFC 9457), risposte come DTO `record` | un solo formato |
| Test | JUnit 5, Spring Boot test, Testcontainers Postgres 17 | come `BUILD-WEEK-5` |
| Pacchetti | per funzionalità: `auth/`, `magazzino/`, `oggetto/`, `movimento/`, `report/`, `assistente/`, `security/`, `common/` | meno conflitti |
| Package base | `it.stockmanager` (da confermare) | — |

Convenzioni: commenti e messaggi in italiano, nomi di classi e colonne in inglese, **niente operatori ternari** (`if`/`else`), commit `<tipo>: <descrizione>` (feat, fix, refactor, docs, test, chore), nessun `Co-Authored-By`.

## 1. Architettura

- Il server vive in `server/` nello stesso repository. Stessa origine della SPA.
- **Sviluppo**: Vite inoltra `/api` al server (`server.proxy`). **Produzione**: il server serve `dist/` oppure sta dietro un reverse proxy sullo stesso dominio.
- Nessun header CORS: con la stessa origine non serve.
- Dev server (Vite e Spring) in ascolto solo su `localhost`.
- Docker Compose alla radice: `db` (postgres:17) e `server`. Render con `render.yaml` solo in fase 6.
- Segreti solo da variabili d'ambiente (`.env` locale, dashboard in produzione). Mai nel repository.

## 2. Autenticazione

- Registrazione e login in `auth/AuthController` e `auth/AuthService`. Nessun altro endpoint di login.
- Password: minimo **12 caratteri**, BCrypt 12, confronto a tempo costante (già garantito da `PasswordEncoder.matches`).
- Sessione: cookie `__Host-sid` con `HttpOnly; Secure; SameSite=Strict; Path=/`. Configurazione in `application.yaml` (`server.servlet.session.cookie.*`).
- Rotazione dell'id di sessione al login (default di Spring Security, verificato nei test).
- Timeout di inattività 30 minuti; timeout assoluto 12 ore con un filtro dedicato (Spring non lo fornisce).
- Logout: invalida la sessione lato server (`HttpSession.invalidate()`), non solo il cookie.
- Rate limit su login e registrazione, **per IP e per account**, con risposta 429. Implementazione in memoria (Bucket4j) con nota: va spostato su Redis se si scala a più istanze.
- Messaggi generici: "credenziali non valide", senza rivelare se l'email esiste.
- `GET /api/auth/me` al caricamento della pagina: ripristina la sessione dopo F5.
- La modalità Ospite resta solo nel client, senza sessione né dati sul server.
- Se un utente viene disattivato o cambia ruolo, le sue sessioni si cancellano (`FindByIndexNameSessionRepository`).

## 3. CSRF e origine

- CSRF attivo (decisione del team), con token letto dal cookie `XSRF-TOKEN` e inviato nell'header `X-XSRF-TOKEN`.
- In più un filtro `Origin`: su POST, PUT, PATCH e DELETE l'header `Origin` deve corrispondere all'origine configurata. Se manca, 403.
- Le richieste accettano solo `Content-Type: application/json`, altrimenti 415.

## 4. Autorizzazione e IDOR

- Ogni tabella di dati ha `owner_id` (UUID dell'utente). Ogni query filtra per l'utente della sessione.
- L'utente corrente si ottiene solo dalla sessione (`CurrentUsers.require(principal)`), mai da body o parametri.
- Risorsa altrui o inesistente: **404**, così non si rivela che esiste.
- Le regole di accesso stanno nei **service**, non solo nei controller.
- Alle entità JPA non si accede mai dai controller: solo DTO `record` con `static from(entity)`.

## 5. Validazione lato server

- Bean Validation (`@NotBlank`, `@Size`, `@Positive`, `@PositiveOrZero`, `@Pattern`) su ogni DTO di richiesta.
- Limiti: nomi 120 caratteri, note 500, SKU 64, email 254.
- Numeri: `qta > 0`, `prezzo >= 0`, valori finiti. Date in formato ISO (`LocalDate`).
- Limite del corpo richiesta: 100 KB, con filtro che risponde 413 sopra soglia (Spring non offre un limite JSON diretto).
- Limiti sulle collezioni: massimo 500 articoli per magazzino, 5.000 movimenti per articolo, paginazione con `size` massimo 50.
- **Regole di business nel server**: la giacenza si calcola sul server. Un prelievo maggiore della giacenza è rifiutato con 400 (la modalità Ospite non passa di qui).
- Ordinamento dinamico, se servirà: whitelist dei campi. Nessuna concatenazione di stringhe in query.

## 6. Proxy AI, `POST /api/assistente`

- Solo utenti autenticati. La chiave è in `ANTHROPIC_API_KEY`, letta dal server.
- Il server scrive il prompt di sistema; il client invia solo i messaggi dell'utente.
- Modello fisso lato server: `claude-haiku-5-5`. `max_tokens` limitato (1000).
- Cronologia massima di 20 messaggi, ciascuno di 2000 caratteri al massimo.
- Quota: massimo 50 richieste al giorno per utente (tabella `ai_usage`) e rate limit al minuto.
- Usare l'SDK ufficiale Java (`com.anthropic:anthropic-java`), non chiamate HTTP a mano: la regola del progetto è usare l'SDK dove esiste. Verificarne la documentazione prima di scrivere il codice.
- Gestire `stop_reason: "refusal"` e gli errori con messaggi generici, senza esporre dettagli dell'API.
- Fine fase 3: rimuovere dal frontend `@anthropic-ai/sdk`, `dangerouslyAllowBrowser`, `VITE_ANTHROPIC_API_KEY` in `vite-env.d.ts` e in `.env.example`.

## 7. Header di sicurezza

- Spring Security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` restrittiva.
- HSTS solo in produzione.
- CSP per l'app:
  `default-src 'self'; script-src 'self'; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`
  Self-hosting dei font (fase 6) permette di togliere i domini esterni.
- Gli stili inline di React passano dal CSSOM e non violano questa CSP.

## 8. Logging e osservabilità

- Mai loggare password, cookie, id di sessione, chiave API o corpo delle richieste di login.
- Log di audit dei tentativi di login (esito, IP, timestamp, senza password).
- In produzione niente stack trace nelle risposte: `ProblemDetail` generico, dettaglio solo nel log.
- Actuator: esposto solo `health`, pubblico; il resto richiede autenticazione o è disabilitato.

## 9. Migrazione del frontend

- `services/storage.ts` diventa un client API con `fetch(..., { credentials: "same-origin" })` e token CSRF letto dal cookie `XSRF-TOKEN`.
- `authService` chiama `/api/auth/*`. Si eliminano `utils/sha256.ts` e la chiave `stock_account` in localStorage.
- Import una tantum (fase 4): un endpoint riceve i dati esistenti di `localStorage`, li valida interamente e il client li cancella solo dopo conferma.
- Stato di sessione all'avvio: `GET /api/auth/me`.

## 10. Dipendenze

- Upgrade di `vite` e `vitest` a una major dentro questo lavoro: chiude le 5 vulnerabilità alte del server di sviluppo.
- `npm audit` a ogni fase. Per il back-end, lo strumento equivalente è `./mvnw dependency-check` (OWASP) oppure Dependabot.

## 11. Test di sicurezza (obbligatori)

Test di integrazione con Testcontainers:
- **401** senza sessione su ogni rotta protetta;
- **404** su risorsa di un altro utente (IDOR);
- **403** su richiesta senza `Origin` o con `Origin` estraneo;
- **429** oltre il rate limit di login;
- **413** con corpo oltre 100 KB;
- **400** su prelievo maggiore della giacenza;
- **Set-Cookie** con `HttpOnly`, `Secure`, `SameSite=Strict`, nome `__Host-sid`;
- BCrypt: la password non compare mai in risposta né nei log;
- Regressione: `grep "sk-ant" dist` deve restituire 0.

`auth.spec.ts` si aggiorna per il login reale. Aggiungere una e2e per il ripristino dopo F5.

## 12. Endpoint

| Metodo | Percorso | Note |
|---|---|---|
| POST | `/api/auth/register` | pubblico, rate limit |
| POST | `/api/auth/login` | pubblico, rate limit |
| POST | `/api/auth/logout` | sessione richiesta |
| GET | `/api/auth/me` | restituisce l'utente o 401 |
| GET / PUT | `/api/azienda` | nome azienda |
| GET / POST | `/api/magazzini` | elenco e creazione |
| GET / PUT / DELETE | `/api/magazzini/{id}` | |
| GET / POST | `/api/magazzini/{id}/oggetti` | |
| GET / PUT / DELETE | `/api/oggetti/{id}` | |
| GET / POST | `/api/oggetti/{id}/movimenti` | regole di giacenza nel server |
| DELETE | `/api/movimenti/{id}` | |
| GET | `/api/report` | elenco paginato |
| POST | `/api/magazzini/{id}/report` | genera e archivia un mese |
| GET | `/api/report/{id}/csv` | esportazione con neutralizzazione CSV |
| POST | `/api/assistente` | proxy AI |
| GET | `/actuator/health` | pubblico |

## 13. Fasi

**Stato:** tutte le fasi (1–6) completate. Font self-hosted e CSP same-origin (senza domini esterni). Deploy: `Dockerfile` unico (app + server), costruito e verificato con Docker (immagine di circa 676 MB, utente non root, nessun `.env` dentro, assistente funzionante nel container), e `render.yaml`. Audit: 0 vulnerabilità in produzione; restano 6 segnalazioni nel solo ramo di sviluppo, tutte dalla libreria `picomatch` (ReDoS), per cui non esiste ancora una versione corretta pubblicata. E2E 6/6 anche contro il jar di produzione.

Ogni fase è verificabile da sola: a fine fase si mostra il risultato e si attende l'approvazione prima della successiva.

1. **Scheletro server**: progetto Spring Boot, Docker Compose con Postgres, schema, header di sicurezza, health, proxy Vite. Test: avvio e `/actuator/health`.
2. **Autenticazione e frontend**: register, login, logout, `me`, rate limit, CSRF, Origin, BCrypt. Migrazione di `authService`, F5 risolto. Test di sicurezza della sezione 11 per l'auth.
3. **Proxy AI**: endpoint, quota, rimozione della chiave dal client. Test: nessuna chiave nel bundle.
4. **Dati**: magazzini, oggetti, movimenti con autorizzazione e regole di giacenza. Import da `localStorage`. Test IDOR e validazione.
5. **Report**: generazione, archivio, esportazione CSV neutralizzata.
6. **Chiusura**: CSP definitiva, self-hosting dei font, upgrade `vite`/`vitest`, `render.yaml`, documentazione.

## 14. Decisioni aperte

1. Confermare il package base `it.stockmanager`.
2. Confermare BCrypt 12 come da convenzione del team. In alternativa Argon2id: richiede una decisione nuova da registrare nei decisioni del progetto.
3. Rate limit in memoria (una sola istanza) oppure Redis fin dall'inizio.
