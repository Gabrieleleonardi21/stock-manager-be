package it.stockmanager.importazione;

import it.stockmanager.importazione.ImportDto.ImportEsito;
import it.stockmanager.importazione.ImportDto.MagazzinoImport;
import it.stockmanager.security.CurrentUsers;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService servizio;

    public ImportController(ImportService servizio) {
        this.servizio = servizio;
    }

    /** 201 se il magazzino e' stato creato, 200 se era gia' stato importato. */
    @PostMapping("/magazzini")
    public ResponseEntity<ImportEsito> magazzino(@Valid @RequestBody MagazzinoImport dati, Authentication auth) {
        ImportEsito esito = servizio.importa(CurrentUsers.require(auth), dati);
        HttpStatus stato = HttpStatus.OK;
        if (esito.nuovo()) {
            stato = HttpStatus.CREATED;
        }
        return ResponseEntity.status(stato).body(esito);
    }
}
