package it.stockmanager.magazzino;

import it.stockmanager.magazzino.MagazzinoDto.MagazzinoRequest;
import it.stockmanager.magazzino.MagazzinoDto.MagazzinoView;
import it.stockmanager.security.CurrentUsers;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/magazzini")
public class MagazzinoController {

    private final MagazzinoService servizio;

    public MagazzinoController(MagazzinoService servizio) {
        this.servizio = servizio;
    }

    @PostMapping
    public ResponseEntity<MagazzinoView> crea(@Valid @RequestBody MagazzinoRequest r, Authentication auth) {
        Magazzino m = servizio.crea(CurrentUsers.require(auth), r);
        return ResponseEntity.status(HttpStatus.CREATED).body(MagazzinoView.from(m, java.util.List.of()));
    }

    @PutMapping("/{id}")
    public MagazzinoView aggiorna(@PathVariable UUID id, @Valid @RequestBody MagazzinoRequest r, Authentication auth) {
        return MagazzinoView.from(servizio.aggiorna(CurrentUsers.require(auth), id, r), java.util.List.of());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> elimina(@PathVariable UUID id, Authentication auth) {
        servizio.elimina(CurrentUsers.require(auth), id);
        return ResponseEntity.noContent().build();
    }
}
