package it.stockmanager.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limite di richieste a finestra fissa, in memoria (sezione 2 del piano).
 * Va bene con una sola istanza: con piu' istanze passare a Redis.
 */
@Component
public class RateLimiter {

    private record Finestra(int conteggio, Instant inizio) {}

    private final Map<String, Finestra> finestre = new ConcurrentHashMap<>();

    /**
     * Registra un tentativo sulla chiave e dice se e' ancora consentito.
     * @return true se il tentativo rientra nel limite
     */
    public boolean consenti(String chiave, int max, Duration finestra) {
        Instant ora = Instant.now();
        Finestra f = finestre.compute(chiave, (k, vecchia) -> {
            if (vecchia == null || vecchia.inizio().plus(finestra).isBefore(ora)) {
                return new Finestra(1, ora);
            }
            return new Finestra(vecchia.conteggio() + 1, vecchia.inizio());
        });
        return f.conteggio() <= max;
    }

    /** Azzera la chiave (es. dopo un login riuscito). */
    public void reset(String chiave) {
        finestre.remove(chiave);
    }
}
