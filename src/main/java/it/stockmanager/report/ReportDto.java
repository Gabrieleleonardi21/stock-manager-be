package it.stockmanager.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ReportDto {

    private ReportDto() {}

    public static final String MESE_REGEX = "^[0-9]{4}-(0[1-9]|1[0-2])$";

    public record GeneraRequest(@NotBlank @Pattern(regexp = MESE_REGEX) String mese) {}

    /** Vista completa (con righe), usata per il dettaglio. */
    public record ReportView(UUID id, String mese, String azienda, String magazzinoNome, String autore,
                             BigDecimal totArrivi, BigDecimal totPrelievi, BigDecimal totValoreAcquisti,
                             List<RigaReport> righe) {
        public static ReportView from(ReportMensile r) {
            return new ReportView(r.getId(), r.getMese(), r.getAzienda(), r.getMagazzinoNome(), r.getAutore(),
                    r.getTotArrivi(), r.getTotPrelievi(), r.getTotValoreAcquisti(), r.getRighe());
        }
    }

    /** Vista leggera per l'elenco: senza righe. */
    public record ReportSintesi(UUID id, String mese, String azienda, String magazzinoNome,
                                BigDecimal totArrivi, BigDecimal totPrelievi, BigDecimal totValoreAcquisti) {
        public static ReportSintesi from(ReportMensile r) {
            return new ReportSintesi(r.getId(), r.getMese(), r.getAzienda(), r.getMagazzinoNome(),
                    r.getTotArrivi(), r.getTotPrelievi(), r.getTotValoreAcquisti());
        }
    }

    /** Report gia' archiviati nel browser (fasi precedenti). `chiave` rende l'import ripetibile. */
    public record ReportImport(
            @NotBlank @Size(max = 64) String chiave,
            @NotBlank @Pattern(regexp = MESE_REGEX) String mese,
            @Size(max = 120) String azienda,
            @NotBlank @Size(max = 120) String magazzinoNome,
            @Size(max = 120) String autore,
            @Size(max = 500) List<RigaReport> righe) {}
}
