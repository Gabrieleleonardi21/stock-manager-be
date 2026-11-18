package it.stockmanager.assistente;

import it.stockmanager.assistente.AssistenteDto.AssistenteRequest;
import it.stockmanager.assistente.AssistenteDto.AssistenteResponse;
import it.stockmanager.security.CurrentUsers;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Assistente AI: solo utenti autenticati (la regola generale di SecurityConfig). */
@RestController
@RequestMapping("/api/assistente")
public class AssistenteController {

    private final AssistenteService servizio;

    public AssistenteController(AssistenteService servizio) {
        this.servizio = servizio;
    }

    @PostMapping
    public AssistenteResponse chiedi(@Valid @RequestBody AssistenteRequest richiesta, Authentication auth) {
        String risposta = servizio.chiedi(CurrentUsers.require(auth), richiesta.messaggi());
        return new AssistenteResponse(risposta);
    }
}
