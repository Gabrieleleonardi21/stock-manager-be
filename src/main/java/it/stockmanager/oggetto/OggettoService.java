package it.stockmanager.oggetto;

import it.stockmanager.common.ApiException;
import it.stockmanager.magazzino.Magazzino;
import it.stockmanager.magazzino.MagazzinoRepository;
import it.stockmanager.oggetto.OggettoDto.OggettoRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OggettoService {

    static final long MAX_OGGETTI_PER_MAGAZZINO = 500;

    private final OggettoRepository oggetti;
    private final MagazzinoRepository magazzini;

    public OggettoService(OggettoRepository oggetti, MagazzinoRepository magazzini) {
        this.oggetti = oggetti;
        this.magazzini = magazzini;
    }

    @Transactional
    public Oggetto crea(UUID owner, UUID magazzinoId, OggettoRequest r) {
        Magazzino m = magazzini.findByIdAndOwnerId(magazzinoId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Magazzino non trovato"));
        if (oggetti.countByMagazzinoId(m.getId()) >= MAX_OGGETTI_PER_MAGAZZINO) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Troppi articoli in questo magazzino");
        }
        return oggetti.save(new Oggetto(owner, m.getId(), testo(r.nome()), testo(r.sku()),
                r.categoria().trim(), r.unita().trim(), r.sogliaMin()));
    }

    @Transactional
    public Oggetto aggiorna(UUID owner, UUID id, OggettoRequest r) {
        Oggetto o = oggetti.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Articolo non trovato"));
        o.aggiorna(testo(r.nome()), testo(r.sku()), r.categoria().trim(), r.unita().trim(), r.sogliaMin());
        return o;
    }

    @Transactional
    public void elimina(UUID owner, UUID id) {
        Oggetto o = oggetti.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Articolo non trovato"));
        oggetti.delete(o);
    }

    /** Campo testo: null diventa stringa vuota, gli spazi ai bordi si tolgono. */
    static String testo(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
