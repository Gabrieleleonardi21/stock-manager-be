package it.stockmanager.movimento;

import it.stockmanager.movimento.MovimentoDto.MovimentoRequest;
import it.stockmanager.movimento.MovimentoDto.MovimentoView;
import it.stockmanager.security.CurrentUsers;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MovimentoController {

    private final MovimentoService servizio;

    public MovimentoController(MovimentoService servizio) {
        this.servizio = servizio;
    }

    @PostMapping("/api/oggetti/{oggettoId}/movimenti")
    public ResponseEntity<MovimentoView> crea(@PathVariable UUID oggettoId, @Valid @RequestBody MovimentoRequest r,
                                              Authentication auth) {
        Movimento m = servizio.crea(CurrentUsers.require(auth), oggettoId, r);
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimentoView.from(m));
    }

    @DeleteMapping("/api/movimenti/{id}")
    public ResponseEntity<Void> elimina(@PathVariable UUID id, Authentication auth) {
        servizio.elimina(CurrentUsers.require(auth), id);
        return ResponseEntity.noContent().build();
    }
}
