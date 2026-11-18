package it.stockmanager.magazzino;

import it.stockmanager.common.ApiException;
import it.stockmanager.magazzino.MagazzinoDto.MagazzinoRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MagazzinoService {

    static final long MAX_MAGAZZINI_PER_UTENTE = 200;

    private final MagazzinoRepository magazzini;

    public MagazzinoService(MagazzinoRepository magazzini) {
        this.magazzini = magazzini;
    }

    @Transactional
    public Magazzino crea(UUID owner, MagazzinoRequest r) {
        if (magazzini.countByOwnerId(owner) >= MAX_MAGAZZINI_PER_UTENTE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Troppi magazzini");
        }
        return magazzini.save(new Magazzino(owner, r.nome().trim(), testo(r.luogo()), testo(r.responsabile()), null));
    }

    @Transactional
    public Magazzino aggiorna(UUID owner, UUID id, MagazzinoRequest r) {
        Magazzino m = trova(owner, id);
        m.aggiorna(r.nome().trim(), testo(r.luogo()), testo(r.responsabile()));
        return m;
    }

    /** Cancellazione a cascata: articoli e movimenti sono rimossi dal database (ON DELETE CASCADE). */
    @Transactional
    public void elimina(UUID owner, UUID id) {
        magazzini.delete(trova(owner, id));
    }

    Magazzino trova(UUID owner, UUID id) {
        return magazzini.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Magazzino non trovato"));
    }

    static String testo(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
