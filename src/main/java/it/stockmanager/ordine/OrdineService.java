package it.stockmanager.ordine;

import it.stockmanager.common.ApiException;
import it.stockmanager.magazzino.Magazzino;
import it.stockmanager.magazzino.MagazzinoRepository;
import it.stockmanager.oggetto.Oggetto;
import it.stockmanager.oggetto.OggettoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prepara bozze d'ordine. Ogni articolo deve appartenere al magazzino indicato e all'utente:
 * i riferimenti vengono verificati uno per uno, mai fidandosi dell'input del modello.
 */
@Service
public class OrdineService {

    static final int MAX_RIGHE = 100;
    static final BigDecimal MAX_QUANTITA = BigDecimal.valueOf(1_000_000);

    private final OrdineRepository ordini;
    private final MagazzinoRepository magazzini;
    private final OggettoRepository oggetti;

    public OrdineService(OrdineRepository ordini, MagazzinoRepository magazzini, OggettoRepository oggetti) {
        this.ordini = ordini;
        this.magazzini = magazzini;
        this.oggetti = oggetti;
    }

    @Transactional
    public OrdineView prepara(UUID owner, UUID magazzinoId, List<RichiestaRiga> richieste, String nota) {
        Magazzino m = magazzini.findByIdAndOwnerId(magazzinoId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Magazzino non trovato"));
        if (richieste == null || richieste.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La bozza deve contenere almeno un articolo");
        }
        if (richieste.size() > MAX_RIGHE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Troppe righe nella bozza");
        }
        List<RigaOrdine> righe = new ArrayList<>();
        for (RichiestaRiga r : richieste) {
            if (r.quantita() == null || r.quantita().signum() <= 0 || r.quantita().compareTo(MAX_QUANTITA) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Quantita' non valida");
            }
            Oggetto o = oggetti.findByIdAndOwnerId(r.oggettoId(), owner)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Articolo non trovato"));
            if (!o.getMagazzinoId().equals(m.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "L'articolo non appartiene al magazzino indicato");
            }
            righe.add(new RigaOrdine(o.getId(), o.getNome(), r.quantita()));
        }
        String notaPulita = "";
        if (nota != null) {
            notaPulita = nota.trim();
        }
        return OrdineView.from(ordini.save(new Ordine(owner, m.getId(), notaPulita, righe)));
    }

    /** Richiesta di una riga, come la produce il modello. */
    public record RichiestaRiga(UUID oggettoId, BigDecimal quantita) {}
}
