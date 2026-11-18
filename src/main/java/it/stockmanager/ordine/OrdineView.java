package it.stockmanager.ordine;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Vista della bozza d'ordine: mai l'entita' JPA nelle risposte. */
public record OrdineView(UUID id, UUID magazzinoId, String stato, String nota, List<RigaOrdine> righe,
                         OffsetDateTime creatoIl) {
    public static OrdineView from(Ordine o) {
        return new OrdineView(o.getId(), o.getMagazzinoId(), o.getStato(), o.getNota(), o.getRighe(), o.getCreatoIl());
    }
}
