package it.stockmanager.oggetto;

import it.stockmanager.oggetto.OggettoDto.OggettoRequest;
import it.stockmanager.oggetto.OggettoDto.OggettoView;
import it.stockmanager.security.CurrentUsers;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OggettoController {

    private final OggettoService servizio;

    public OggettoController(OggettoService servizio) {
        this.servizio = servizio;
    }

    @PostMapping("/api/magazzini/{magazzinoId}/oggetti")
    public ResponseEntity<OggettoView> crea(@PathVariable UUID magazzinoId, @Valid @RequestBody OggettoRequest r,
                                            Authentication auth) {
        Oggetto o = servizio.crea(CurrentUsers.require(auth), magazzinoId, r);
        return ResponseEntity.status(HttpStatus.CREATED).body(OggettoView.from(o, List.of()));
    }

    @PutMapping("/api/oggetti/{id}")
    public OggettoView aggiorna(@PathVariable UUID id, @Valid @RequestBody OggettoRequest r, Authentication auth) {
        Oggetto o = servizio.aggiorna(CurrentUsers.require(auth), id, r);
        return OggettoView.from(o, List.of());
    }

    @DeleteMapping("/api/oggetti/{id}")
    public ResponseEntity<Void> elimina(@PathVariable UUID id, Authentication auth) {
        servizio.elimina(CurrentUsers.require(auth), id);
        return ResponseEntity.noContent().build();
    }
}
