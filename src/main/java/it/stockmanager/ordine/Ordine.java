package it.stockmanager.ordine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Bozza d'ordine di un magazzino. Lo stato e' sempre 'bozza': l'invio al fornitore non e' implementato. */
@Entity
@Table(name = "ordini")
public class Ordine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "magazzino_id", nullable = false)
    private UUID magazzinoId;

    @Column(nullable = false, length = 20)
    private String stato;

    @Column(nullable = false, length = 500)
    private String nota;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<RigaOrdine> righe;

    @CreationTimestamp
    @Column(name = "creato_il", nullable = false, updatable = false)
    private OffsetDateTime creatoIl;

    protected Ordine() {}

    public Ordine(UUID ownerId, UUID magazzinoId, String nota, List<RigaOrdine> righe) {
        this.ownerId = ownerId;
        this.magazzinoId = magazzinoId;
        this.stato = "bozza";
        this.nota = nota;
        this.righe = righe;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getMagazzinoId() { return magazzinoId; }
    public String getStato() { return stato; }
    public String getNota() { return nota; }
    public List<RigaOrdine> getRighe() { return righe; }
    public OffsetDateTime getCreatoIl() { return creatoIl; }
}
