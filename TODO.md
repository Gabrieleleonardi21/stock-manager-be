# TODO — back-end STOCK MANAGER

Stato di riferimento: fasi 1–7 del piano (`docs/PIANO-BACKEND.md`) completate e in produzione.
Legenda: 🔴 priorità alta · 🟡 media · 🟢 bassa

## Assistente e ordini
- 🔴 **Invio delle bozze al fornitore**: oggi le bozze restano in stato `bozza`. Decidere il canale (email, API del fornitore) prima di implementare, e aggiungere la conferma esplicita dell'utente.
- 🟡 **Stati delle bozze**: aggiungere `confermata`, `annullata` e i relativi endpoint (`PUT /api/ordini/{id}`, `DELETE /api/ordini/{id}`).
- 🟡 **Test di sicurezza degli strumenti**: verificare con un secondo utente che `prepara_ordine` e `articoli_da_riordinare` non accedano mai a magazzini altrui.
- 🟡 **Costo per domanda**: la quota giornaliera conta le richieste, non le chiamate al modello (fino a 5 per domanda). Contare anche i giri e registrare i token usati.
- 🟡 **Difesa dal prompt injection negli strumenti**: i nomi degli articoli arrivano dal database e finiscono nel contesto del modello. Limitare la lunghezza dei campi restituiti e valutare un filtro sull'output.
- 🟢 **Log di audit dell'assistente**: registrare chi ha chiesto cosa (senza il testo completo) e quali strumenti ha usato.

## Test
- 🔴 **Test JUnit con Testcontainers** (Postgres 17): autorizzazione (401/404), CSRF/Origin (403), rate limit (429), dimensione del corpo (413), regola della giacenza (400), cookie `__Host-sid` con i flag corretti. Docker è disponibile: sostituiscono gradualmente `scripts/test-dati.sh`.
- 🟡 **Test della concorrenza** su movimenti e bozze (blocco di riga) dentro JUnit.
- 🟢 **Test di carico leggero** sull'import di magazzini grandi.

## Sicurezza
- 🟡 **Richiesta senza token CSRF**: oggi risponde 401, il piano prevede 403. È sicuro, ma va allineato e testato.
- 🟡 **Corpi chunked**: il limite di 100 KB (2 MB per l'import) controlla solo `Content-Length`. Aggiungere un limite sul flusso in lettura.
- 🟡 **Rate limit in memoria**: funziona con una sola istanza. Con più istanze passare a Redis.
- 🟡 **Retention dei log di accesso** (`login_attempt`): definire quanto tenerli e aggiungere la pulizia periodica.
- 🟡 **Recupero password e verifica email**: non ci sono. Da decidere se servono.
- 🟢 **Avviso di password generata** all'avvio (`UserDetailsServiceAutoConfiguration`): non serve, si può disattivare.

## Dati e schema
- 🟡 **Tipi colonna `mese`**: `report_mensili.mese` è `CHAR(7)` per allineare lo schema esistente. Uniformare i tipi nel prossimo cambio di schema.
- 🟡 **`GET /api/azienda` carica tutto**: con account molto grandi serve la paginazione per magazzino.
- 🟢 **Migrazioni dello schema**: lo schema è `db/schema.sql` con `IF NOT EXISTS`. Con modifiche non additive (rinomine, tipi) serve uno strumento di migrazione.

## Import dei dati locali
- 🟡 **Pulizia del vecchio flusso**: quando tutti gli utenti hanno importato i dati, rimuovere `services/legacy.ts` e `POST /api/import/*` (oppure tenerli come percorso di recupero).

## Deploy e operatività
- 🔴 **Rotazione della password Neon**: la password è comparsa nei log della sessione di sviluppo. Ruotarla e aggiornare `DB_PASSWORD` su Render.
- 🟡 **Utenti di test in produzione**: eliminare gli account `@example.com` creati durante le verifiche.
- 🟡 **Piano Render**: il piano gratuito va in pausa e il primo avvio ha impiegato circa 3 minuti. Valutare un piano a pagamento.
- 🟡 **Processo di rilascio del front-end**: il `Dockerfile` fissa il commit del front-end (`FE_REF`). Ad ogni nuova versione del front-end va aggiornato, e poi fatto un deploy con "Clear build cache".
- 🟢 **Auto-deploy**: decidere se il deploy parte da ogni commit o solo manualmente.
- 🟢 **Monitoraggio**: metriche e alert su errori 5xx e tempi di risposta.

## Dipendenze
- 🟡 **Vulnerabilità `picomatch`** (solo dev, ReDoS): nessuna versione corretta pubblicata al momento. Riverificare periodicamente e aggiornare quando esce il fix.
- 🟢 **Aggiornamento di Spring Boot e Anthropic SDK**: controllare le nuove versioni con regolarità.
