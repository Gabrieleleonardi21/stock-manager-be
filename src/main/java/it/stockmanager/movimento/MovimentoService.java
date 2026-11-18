package it.stockmanager.movimento;

import it.stockmanager.common.ApiException;
import it.stockmanager.movimento.MovimentoDto.MovimentoRequest;
import it.stockmanager.oggetto.Oggetto;
import it.stockmanager.oggetto.OggettoRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Movimenti di magazzino e regola della giacenza (sezione 5 del piano). */
@Service
public class MovimentoService {

    static final long MAX_MOVIMENTI_PER_OGGETTO = 5000;

    private final MovimentoRepository movimenti;
    private final OggettoRepository oggetti;

    public MovimentoService(MovimentoRepository movimenti, OggettoRepository oggetti) {
        this.movimenti = movimenti;
        this.oggetti = oggetti;
    }

    /**
     * Registra un movimento. Il prelievo non puo' superare la giacenza: il calcolo avviene sul server,
     * con la riga dell'articolo bloccata per evitare prelievi simultanei oltre il disponibile.
     */
    @Transactional
    public Movimento crea(UUID owner, UUID oggettoId, MovimentoRequest r) {
        Oggetto oggetto = oggetti.bloccaPerMovimento(oggettoId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Articolo non trovato"));
        if (movimenti.countByOggettoId(oggettoId) >= MAX_MOVIMENTI_PER_OGGETTO) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Troppi movimenti per questo articolo");
        }
        if (r.tipo() == Tipo.prelievo) {
            BigDecimal arrivi = movimenti.sommaQta(oggettoId, Tipo.arrivo);
            BigDecimal prelievi = movimenti.sommaQta(oggettoId, Tipo.prelievo);
            BigDecimal giacenza = arrivi.subtract(prelievi);
            if (r.qta().compareTo(giacenza) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Quantità superiore alla giacenza disponibile (" + giacenza.stripTrailingZeros().toPlainString() + ")");
            }
        }
        // il prezzo d'acquisto ha senso solo per gli arrivi
        BigDecimal prezzo = null;
        if (r.tipo() == Tipo.arrivo) {
            prezzo = r.prezzo();
        }
        String nota = "";
        if (r.nota() != null) {
            nota = r.nota().trim();
        }
        return movimenti.save(new Movimento(owner, oggetto.getId(), r.tipo(), r.data(), r.qta(), prezzo, nota));
    }

    @Transactional
    public void elimina(UUID owner, UUID movimentoId) {
        Movimento m = movimenti.findByIdAndOwnerId(movimentoId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movimento non trovato"));
        movimenti.delete(m);
    }
}
