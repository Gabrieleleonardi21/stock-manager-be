package it.stockmanager.demo;

import it.stockmanager.importazione.ImportDto.MagazzinoImport;
import it.stockmanager.importazione.ImportDto.OggettoImport;
import it.stockmanager.movimento.MovimentoDto.MovimentoRequest;
import it.stockmanager.movimento.Tipo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Magazzino di prova. Le giacenze rispettano la regola del server (nessun prelievo oltre lo stock),
 * cosi' l'import non viene rifiutato. Due mesi di movimenti, per mostrare anche i report.
 */
final class DatiDemo {

    private DatiDemo() {}

    static MagazzinoImport magazzino() {
        return new MagazzinoImport(
                "demo-v1",
                "Magazzino demo",
                "Milano",
                "Responsabile demo",
                List.of(
                        articolo("Viti M6 inox", "VT-M6-01", "Ricambi", "pz", 200,
                                arrivo("2026-10-05", 800, "0.12", "Fornitore A"),
                                prelievo("2026-10-20", 350, "0.25", "Linea 2"),
                                prelievo("2026-11-03", 300, "0.25", "Linea 1")),
                        articolo("Olio lubrificante", "OL-22", "Consumabili", "l", 40,
                                arrivo("2026-11-02", 120, "4.80", "Lotto 88"),
                                prelievo("2026-11-10", 30, "9.50", "Manutenzione")),
                        articolo("Pallet EUR 120x80", "PL-EU", "Imballaggi", "pallet", 10,
                                arrivo("2026-10-12", 24, "7.50", "Consegna mensile")),
                        articolo("Cartone 40x30", "CT-40", "Imballaggi", "pz", 150,
                                arrivo("2026-11-06", 500, "0.45", ""),
                                prelievo("2026-11-14", 420, "0.80", "Spedizioni"))));
    }

    private static OggettoImport articolo(String nome, String sku, String categoria, String unita,
                                          int sogliaMin, MovimentoRequest... movimenti) {
        return new OggettoImport(nome, sku, categoria, unita, BigDecimal.valueOf(sogliaMin), List.of(movimenti));
    }

    private static MovimentoRequest arrivo(String data, int qta, String prezzo, String nota) {
        return new MovimentoRequest(Tipo.arrivo, LocalDate.parse(data), BigDecimal.valueOf(qta),
                new BigDecimal(prezzo), nota);
    }

    private static MovimentoRequest prelievo(String data, int qta, String prezzo, String nota) {
        return new MovimentoRequest(Tipo.prelievo, LocalDate.parse(data), BigDecimal.valueOf(qta),
                new BigDecimal(prezzo), nota);
    }
}
