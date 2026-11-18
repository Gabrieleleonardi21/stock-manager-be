package it.stockmanager.report;

import it.stockmanager.report.ReportDto.GeneraRequest;
import it.stockmanager.report.ReportDto.ReportImport;
import it.stockmanager.report.ReportDto.ReportSintesi;
import it.stockmanager.report.ReportDto.ReportView;
import it.stockmanager.security.CurrentUsers;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final ReportService servizio;

    public ReportController(ReportService servizio) {
        this.servizio = servizio;
    }

    @PostMapping("/api/magazzini/{magazzinoId}/report")
    public ReportView genera(@PathVariable UUID magazzinoId, @Valid @RequestBody GeneraRequest r, Authentication auth) {
        return servizio.genera(CurrentUsers.require(auth), magazzinoId, r.mese());
    }

    @GetMapping("/api/report")
    public Page<ReportSintesi> elenco(@RequestParam(defaultValue = "0") int pagina,
                                      @RequestParam(defaultValue = "20") int dimensione, Authentication auth) {
        return servizio.elenco(CurrentUsers.require(auth), pagina, dimensione);
    }

    @GetMapping("/api/report/{id}")
    public ReportView dettaglio(@PathVariable UUID id, Authentication auth) {
        return servizio.dettaglio(CurrentUsers.require(auth), id);
    }

    @GetMapping("/api/report/{id}/csv")
    public ResponseEntity<String> csv(@PathVariable UUID id, Authentication auth) {
        String contenuto = servizio.csv(CurrentUsers.require(auth), id);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("report-" + id + ".csv").build().toString())
                .body(contenuto);
    }

    /** Report archiviati nel browser (fasi precedenti): import idempotente. */
    @PostMapping("/api/import/report")
    public ResponseEntity<Map<String, UUID>> importa(@Valid @RequestBody ReportImport dati, Authentication auth) {
        return ResponseEntity.ok(Map.of("id", servizio.importa(CurrentUsers.require(auth), dati)));
    }
}
