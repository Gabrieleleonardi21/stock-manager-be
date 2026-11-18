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

    /**
     * Entrate del mese: somma dei prelievi valorizzati al prezzo di vendita.
     * Le righe dei report vecchi non hanno il campo: si contano come zero.
     */
    public static BigDecimal totaleEntrate(List<RigaReport> righe) {
        BigDecimal totale = BigDecimal.ZERO;
        for (RigaReport r : righe) {
            if (r.entrate() != null) {
                totale = totale.add(r.entrate());
            }
        }
        return totale;
    }

    /** Vista completa (con righe), usata per il dettaglio. Il risultato e' entrate meno uscite (valore acquisti). */
    public record ReportView(UUID id, String mese, String azienda, String magazzinoNome, String autore,
                             BigDecimal totArrivi, BigDecimal totPrelievi, BigDecimal totValoreAcquisti,
                             BigDecimal totEntrate, BigDecimal totRisultato, List<RigaReport> righe) {
        public static ReportView from(ReportMensile r) {
            BigDecimal entrate = totaleEntrate(r.getRighe());
            return new ReportView(r.getId(), r.getMese(), r.getAzienda(), r.getMagazzinoNome(), r.getAutore(),
                    r.getTotArrivi(), r.getTotPrelievi(), r.getTotValoreAcquisti(), entrate,
                    entrate.subtract(r.getTotValoreAcquisti()), r.getRighe());
        }
    }

    /** Vista leggera per l'elenco: senza righe. */
    public record ReportSintesi(UUID id, String mese, String azienda, String magazzinoNome,
                                BigDecimal totArrivi, BigDecimal totPrelievi, BigDecimal totValoreAcquisti,
                                BigDecimal totEntrate, BigDecimal totRisultato) {
        public static ReportSintesi from(ReportMensile r) {
            BigDecimal entrate = totaleEntrate(r.getRighe());
            return new ReportSintesi(r.getId(), r.getMese(), r.getAzienda(), r.getMagazzinoNome(),
                    r.getTotArrivi(), r.getTotPrelievi(), r.getTotValoreAcquisti(), entrate,
                    entrate.subtract(r.getTotValoreAcquisti()));
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
