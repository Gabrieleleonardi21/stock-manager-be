package it.stockmanager.azienda;

import it.stockmanager.azienda.AziendaDto.AziendaRequest;
import it.stockmanager.azienda.AziendaDto.AziendaView;
import it.stockmanager.security.CurrentUsers;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/azienda")
public class AziendaController {

    private final AziendaService servizio;

    public AziendaController(AziendaService servizio) {
        this.servizio = servizio;
    }

    @GetMapping
    public AziendaView albero(Authentication auth) {
        return servizio.albero(CurrentUsers.require(auth));
    }

    @PutMapping
    public ResponseEntity<Void> rinomina(@Valid @RequestBody AziendaRequest r, Authentication auth) {
        servizio.rinomina(CurrentUsers.require(auth), r);
        return ResponseEntity.noContent().build();
    }
}
