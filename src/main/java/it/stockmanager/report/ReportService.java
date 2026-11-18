package it.stockmanager.report;

import it.stockmanager.common.ApiException;
import it.stockmanager.magazzino.Magazzino;
import it.stockmanager.magazzino.MagazzinoRepository;
import it.stockmanager.movimento.Movimento;
import it.stockmanager.movimento.MovimentoRepository;
import it.stockmanager.movimento.Tipo;
import it.stockmanager.oggetto.Oggetto;
import it.stockmanager.oggetto.OggettoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.stockmanager.azienda.Azienda;
import it.stockmanager.azienda.AziendaRepository;
import it.stockmanager.report.ReportDto.ReportImport;
import it.stockmanager.report.ReportDto.ReportSintesi;
import it.stockmanager.report.ReportDto.ReportView;

/** Report mensili: calcolati sul server dai movimenti, stessa logica della versione precedente nel browser. */
@Service
public class ReportService {

    static final int MAX_PAGE_SIZE = 50;
    private static final BigDecimal CENTO = BigDecimal.valueOf(100);

    private final ReportRepository report;
    private final MagazzinoRepository magazzini;
    private final OggettoRepository oggetti;
    private final MovimentoRepository movimenti;
    private final AziendaRepository aziende;

    public ReportService(ReportRepository report, MagazzinoRepository magazzini, OggettoRepository oggetti,
                         MovimentoRepository movimenti, AziendaRepository aziende) {
        this.report = report;
        this.magazzini = magazzini;
        this.oggetti = oggetti;
        this.movimenti = movimenti;
        this.aziende = aziende;
    }

    /** Genera (o rigenera) il report di un mese per un magazzino dell'utente. */
    @Transactional
    public ReportView genera(UUID owner, UUID magazzinoId, String mese) {
        Magazzino m = magazzini.findByIdAndOwnerId(magazzinoId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Magazzino non trovato"));
        List<RigaReport> righe = calcola(owner, m.getId(), mese);
        if (righe.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nessun movimento in questo mese: niente da archiviare");
        }
        String azienda = aziende.findById(owner).map(Azienda::getNome).orElse("");
        ReportMensile r = report.findByOwnerIdAndMagazzinoIdAndMese(owner, m.getId(), mese)
                .orElseGet(() -> new ReportMensile(owner, m.getId(), mese, azienda, m.getNome(), "", righe, null));
        r.rinominaAzienda(azienda);
        r.aggiorna(mese, righe);
        return ReportView.from(report.save(r));
    }

    /** Elenco paginato, senza righe. La dimensione della pagina e' limitata (sezione 1 di SECURITY.md). */
    @Transactional(readOnly = true)
    public Page<ReportSintesi> elenco(UUID owner, int pagina, int dimensione) {
        int misura = Math.min(Math.max(dimensione, 1), MAX_PAGE_SIZE);
        return report.findByOwnerIdOrderByMeseDescCreatedAtDesc(owner, PageRequest.of(Math.max(pagina, 0), misura))
                .map(ReportSintesi::from);
    }

    @Transactional(readOnly = true)
    public ReportView dettaglio(UUID owner, UUID id) {
        return ReportView.from(trova(owner, id));
    }

    /** CSV con separatore ";" (Excel italiano) e le celle di testo protette dalle formule. */
    @Transactional(readOnly = true)
    public String csv(UUID owner, UUID id) {
        ReportMensile r = trova(owner, id);
        List<String> righe = new ArrayList<>();
        righe.add(riga("Articolo", "SKU", "Categoria", "Unita", "Giacenza iniziale", "Arrivi", "Prelievi",
                "Giacenza finale", "Valore acquisti EUR"));
        for (RigaReport x : r.getRighe()) {
            righe.add(riga(x.nome(), x.sku(), x.categoria(), x.unita(), qta(x.giacenzaIniziale()), qta(x.arrivi()),
                    qta(x.prelievi()), qta(x.giacenzaFinale()), euro(x.valoreAcquisti())));
        }
        righe.add(riga("TOTALE", "", "", "", "", qta(r.getTotArrivi()), qta(r.getTotPrelievi()), "",
                euro(r.getTotValoreAcquisti())));
        return "﻿" + String.join("\n", righe);
    }

    /** Import di un report archiviato nel browser. Idempotente: la stessa chiave non crea un duplicato. */
    @Transactional
    public UUID importa(UUID owner, ReportImport dati) {
        var esistente = report.findByOwnerIdAndImportKey(owner, dati.chiave());
        if (esistente.isPresent()) {
            return esistente.get().getId();
        }
        ReportMensile r = new ReportMensile(owner, null, dati.mese(), testoOVuoto(dati.azienda()),
                dati.magazzinoNome(), testoOVuoto(dati.autore()), righeOVuote(dati.righe()), dati.chiave());
        return report.save(r).getId();
    }

    /** Calcolo del mese: giacenza iniziale dai movimenti precedenti, poi arrivi, prelievi e valore acquisti. */
    List<RigaReport> calcola(UUID owner, UUID magazzinoId, String mese) {
        Map<UUID, List<Movimento>> perOggetto = movimenti.findByOwnerId(owner).stream()
                .collect(Collectors.groupingBy(Movimento::getOggettoId));
        List<RigaReport> righe = new ArrayList<>();
        for (Oggetto o : oggetti.findByOwnerId(owner)) {
            if (!o.getMagazzinoId().equals(magazzinoId)) {
                continue;
            }
            BigDecimal iniziale = BigDecimal.ZERO;
            BigDecimal arrivi = BigDecimal.ZERO;
            BigDecimal prelievi = BigDecimal.ZERO;
            BigDecimal valore = BigDecimal.ZERO;
            List<Movimento> lista = perOggetto.getOrDefault(o.getId(), List.of());
            for (Movimento mv : lista) {
                String mm = mv.getData().toString().substring(0, 7);
                BigDecimal delta = mv.getQta();
                if (mv.getTipo() != Tipo.arrivo) {
                    delta = mv.getQta().negate();
                }
                if (mm.compareTo(mese) < 0) {
                    iniziale = iniziale.add(delta);
                } else if (mm.equals(mese)) {
                    if (mv.getTipo() == Tipo.arrivo) {
                        arrivi = arrivi.add(mv.getQta());
                        if (mv.getPrezzo() != null) {
                            valore = valore.add(mv.getQta().multiply(mv.getPrezzo()));
                        }
                    } else {
                        prelievi = prelievi.add(mv.getQta());
                    }
                }
            }
            // come nella versione precedente: entrano nel report solo gli articoli con movimenti o giacenza
            boolean attivo = arrivi.signum() != 0 || prelievi.signum() != 0 || iniziale.signum() != 0;
            if (attivo) {
                BigDecimal finale = iniziale.add(arrivi).subtract(prelievi);
                righe.add(new RigaReport(o.getNome(), o.getSku(), o.getCategoria(), o.getUnita(),
                        iniziale, arrivi, prelievi, finale, valore));
            }
        }
        righe.sort(Comparator.comparing(RigaReport::nome));
        return righe;
    }

    private ReportMensile trova(UUID owner, UUID id) {
        return report.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report non trovato"));
    }

    /** Una cella protetta: il testo che inizia con = + - @ (o tab/a capo) diventerebbe una formula in Excel. */
    static String cella(String s) {
        String base = testoOVuoto(s);
        if (!base.isEmpty() && "=+-@\t\r".indexOf(base.charAt(0)) >= 0) {
            base = "'" + base;
        }
        String escaped = base.replace("\"", "\"\"");
        if (escaped.contains(";") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    static String riga(String... celle) {
        List<String> out = new ArrayList<>();
        for (String c : celle) {
            out.add(cella(c));
        }
        return String.join(";", out);
    }

    /** Quantita': senza zeri inutili, come nella versione precedente. */
    static String qta(BigDecimal v) {
        if (v == null) {
            return "";
        }
        return v.stripTrailingZeros().toPlainString();
    }

    /** Valori in euro: due decimali. */
    static String euro(BigDecimal v) {
        if (v == null) {
            return "";
        }
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    static String testoOVuoto(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    static List<RigaReport> righeOVuote(List<RigaReport> righe) {
        if (righe == null) {
            return List.of();
        }
        return righe;
    }
}
