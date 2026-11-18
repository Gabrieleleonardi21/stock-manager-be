-- Schema STOCK MANAGER: fonte di verita' (decisione D17). Idempotente: si puo' rieseguire.
-- Le tabelle delle sessioni (spring_session*) le crea Spring Session JDBC, non questo file.

CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,  -- BCrypt: hash, mai la password in chiaro
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Audit dei tentativi di accesso: niente password, solo esito e origine.
CREATE TABLE IF NOT EXISTS login_attempt (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(254) NOT NULL,
    ip         VARCHAR(64)  NOT NULL,
    success    BOOLEAN      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_login_attempt_email_time ON login_attempt (email, created_at);

-- Quota giornaliera dell'assistente AI per utente (fase 3): una riga per utente e giorno.
CREATE TABLE IF NOT EXISTS ai_usage (
    owner_id  UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    giorno    DATE    NOT NULL,
    richieste INTEGER NOT NULL DEFAULT 0 CHECK (richieste >= 0),
    PRIMARY KEY (owner_id, giorno)
);

-- Dati del gestionale (fase 4). Ogni riga ha owner_id: ogni query filtra per il proprietario (sezione 4).
CREATE TABLE IF NOT EXISTS azienda (
    owner_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    nome     VARCHAR(120) NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS magazzini (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    nome         VARCHAR(120) NOT NULL,
    luogo        VARCHAR(200) NOT NULL DEFAULT '',
    responsabile VARCHAR(120) NOT NULL DEFAULT '',
    import_key   VARCHAR(64),                       -- id locale del browser, per l'import idempotente
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_magazzini_owner ON magazzini (owner_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_magazzini_import ON magazzini (owner_id, import_key) WHERE import_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS oggetti (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    magazzino_id UUID          NOT NULL REFERENCES magazzini (id) ON DELETE CASCADE,
    nome         VARCHAR(120)  NOT NULL DEFAULT '',
    sku          VARCHAR(64)   NOT NULL DEFAULT '',
    categoria    VARCHAR(60)   NOT NULL,
    unita        VARCHAR(16)   NOT NULL,
    soglia_min   NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (soglia_min >= 0),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oggetti_owner ON oggetti (owner_id);
CREATE INDEX IF NOT EXISTS idx_oggetti_magazzino ON oggetti (magazzino_id);

CREATE TABLE IF NOT EXISTS movimenti (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id   UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    oggetto_id UUID          NOT NULL REFERENCES oggetti (id) ON DELETE CASCADE,
    tipo       VARCHAR(10)   NOT NULL CHECK (tipo IN ('arrivo', 'prelievo')),
    data       DATE          NOT NULL,
    qta        NUMERIC(14,3) NOT NULL CHECK (qta > 0),
    prezzo     NUMERIC(14,4) CHECK (prezzo IS NULL OR prezzo >= 0),
    nota       VARCHAR(500)  NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_movimenti_owner ON movimenti (owner_id);
CREATE INDEX IF NOT EXISTS idx_movimenti_oggetto ON movimenti (oggetto_id);

-- Report mensili archiviati (fase 5). Una riga per magazzino e mese: rigenerare sostituisce il precedente.
CREATE TABLE IF NOT EXISTS report_mensili (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    magazzino_id        UUID          REFERENCES magazzini (id) ON DELETE SET NULL,
    mese                CHAR(7)       NOT NULL CHECK (mese ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    azienda             VARCHAR(120)  NOT NULL DEFAULT '',
    magazzino_nome      VARCHAR(120)  NOT NULL,
    autore              VARCHAR(120)  NOT NULL DEFAULT '',
    tot_arrivi          NUMERIC(16,3) NOT NULL,
    tot_prelievi        NUMERIC(16,3) NOT NULL,
    tot_valore_acquisti NUMERIC(18,4) NOT NULL,
    righe               JSONB         NOT NULL,
    import_key          VARCHAR(64),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (owner_id, magazzino_id, mese)
);
CREATE INDEX IF NOT EXISTS idx_report_owner ON report_mensili (owner_id, mese);
CREATE UNIQUE INDEX IF NOT EXISTS uq_report_import ON report_mensili (owner_id, import_key) WHERE import_key IS NOT NULL;

-- Bozze d'ordine preparate dall'assistente (fase 7). Non vengono inviate a nessuno: restano in stato 'bozza'.
CREATE TABLE IF NOT EXISTS ordini (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    magazzino_id UUID         NOT NULL REFERENCES magazzini (id) ON DELETE CASCADE,
    stato        VARCHAR(20)  NOT NULL DEFAULT 'bozza' CHECK (stato IN ('bozza')),
    nota         VARCHAR(500) NOT NULL DEFAULT '',
    righe        JSONB        NOT NULL,
    creato_il    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ordini_owner ON ordini (owner_id);
