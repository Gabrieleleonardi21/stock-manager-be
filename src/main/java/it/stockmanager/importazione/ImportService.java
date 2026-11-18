package it.stockmanager.importazione;

import it.stockmanager.common.ApiException;
import it.stockmanager.importazione.ImportDto.ImportEsito;
import it.stockmanager.importazione.ImportDto.MagazzinoImport;
import it.stockmanager.importazione.ImportDto.OggettoImport;
import it.stockmanager.magazzino.Magazzino;
import it.stockmanager.magazzino.MagazzinoRepository;
import it.stockmanager.movimento.MovimentoDto.MovimentoRequest;
import it.stockmanager.movimento.Movimento;
import it.stockmanager.movimento.MovimentoRepository;
import it.stockmanager.movimento.Tipo;
import it.stockmanager.oggetto.Oggetto;
import it.stockmanager.oggetto.OggettoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Import una tantum dai dati del browser (sezione 9). Idempotente: un magazzino gia' importato non si duplica.
 * I dati vengono controllati con le stesse regole del server: se un articolo non torna, nulla viene salvato.
 */
@Service
public class ImportService {

    private final MagazzinoRepository magazzini;
    private final OggettoRepository oggetti;
    private final MovimentoRepository movimenti;

    public ImportService(MagazzinoRepository magazzini, OggettoRepository oggetti, MovimentoRepository movimenti) {
        this.magazzini = magazzini;
        this.oggetti = oggetti;
        this.movimenti = movimenti;
    }

    @Transactional
    public ImportEsito importa(UUID owner, MagazzinoImport dati) {
        var esistente = magazzini.findByOwnerIdAndImportKey(owner, dati.chiave());
        if (esistente.isPresent()) {
            return new ImportEsito(false, esistente.get().getId());
        }
        // prima si verifica tutto, poi si scrive: nessun salvataggio parziale
        for (OggettoImport o : dati.oggetti()) {
            verificaGiacenza(o);
        }
        Magazzino m = magazzini.save(new Magazzino(owner, dati.nome().trim(), testo(dati.luogo()),
                testo(dati.responsabile()), dati.chiave()));
        for (OggettoImport o : dati.oggetti()) {
            Oggetto salvato = oggetti.save(new Oggetto(owner, m.getId(), testo(o.nome()), testo(o.sku()),
                    o.categoria().trim(), o.unita().trim(), o.sogliaMin()));
            List<Movimento> daSalvare = new ArrayList<>();
            for (MovimentoRequest mv : o.movimenti()) {
                BigDecimal prezzo = null;
                if (mv.tipo() == Tipo.arrivo) {
                    prezzo = mv.prezzo();
                }
                daSalvare.add(new Movimento(owner, salvato.getId(), mv.tipo(), mv.data(), mv.qta(), prezzo,
                        testo(mv.nota())));
            }
            // un solo salvataggio collettivo: con il batch JDBC diventa poche richieste al database
            movimenti.saveAll(daSalvare);
        }
        return new ImportEsito(true, m.getId());
    }

    /** Stessa regola della giacenza del server, applicata in ordine di data. */
    static void verificaGiacenza(OggettoImport o) {
        List<MovimentoRequest> ordinati = o.movimenti().stream()
                .sorted(Comparator.comparing(MovimentoRequest::data))
                .toList();
        BigDecimal saldo = BigDecimal.ZERO;
        for (MovimentoRequest mv : ordinati) {
            if (mv.tipo() == Tipo.arrivo) {
                saldo = saldo.add(mv.qta());
            } else {
                saldo = saldo.subtract(mv.qta());
            }
            if (saldo.signum() < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Dati incoerenti: prelievo oltre la giacenza sull'articolo " + o.nome());
            }
        }
    }

    static String testo(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
