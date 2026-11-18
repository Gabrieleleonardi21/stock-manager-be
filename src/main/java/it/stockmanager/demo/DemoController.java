package it.stockmanager.demo;

import it.stockmanager.importazione.ImportDto.ImportEsito;
import it.stockmanager.importazione.ImportService;
import it.stockmanager.security.CurrentUsers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dati di prova per mostrare il gestionale. Crea un magazzino demo nell'account dell'utente.
 * Idempotente: la chiave fissa evita duplicati se il pulsante viene premuto piu' volte.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final ImportService importazione;

    public DemoController(ImportService importazione) {
        this.importazione = importazione;
    }

    /** 201 alla prima creazione, 200 se i dati di prova ci sono gia'. */
    @PostMapping
    public ResponseEntity<ImportEsito> carica(Authentication auth) {
        ImportEsito esito = importazione.importa(CurrentUsers.require(auth), DatiDemo.magazzino());
        HttpStatus stato = HttpStatus.OK;
        if (esito.nuovo()) {
            stato = HttpStatus.CREATED;
        }
        return ResponseEntity.status(stato).body(esito);
    }
}
