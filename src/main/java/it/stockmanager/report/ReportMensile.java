package it.stockmanager.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Report mensile archiviato: fotografia dei dati di un magazzino in un mese. */
@Entity
@Table(name = "report_mensili")
public class ReportMensile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "magazzino_id")
    private UUID magazzinoId;

    // CHAR(7) come nello schema (db/schema.sql): Hibernate in validate richiede lo stesso tipo
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 7)
    private String mese;

    @Column(nullable = false, length = 120)
    private String azienda;

    @Column(name = "magazzino_nome", nullable = false, length = 120)
    private String magazzinoNome;

    @Column(nullable = false, length = 120)
    private String autore;

    @Column(name = "tot_arrivi", nullable = false, precision = 16, scale = 3)
    private BigDecimal totArrivi;

    @Column(name = "tot_prelievi", nullable = false, precision = 16, scale = 3)
    private BigDecimal totPrelievi;

    @Column(name = "tot_valore_acquisti", nullable = false, precision = 18, scale = 4)
    private BigDecimal totValoreAcquisti;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<RigaReport> righe;

    @Column(name = "import_key", length = 64)
    private String importKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ReportMensile() {}

    public ReportMensile(UUID ownerId, UUID magazzinoId, String mese, String azienda, String magazzinoNome,
                         String autore, List<RigaReport> righe, String importKey) {
        this.ownerId = ownerId;
        this.magazzinoId = magazzinoId;
        this.azienda = azienda;
        this.magazzinoNome = magazzinoNome;
        this.autore = autore;
        this.importKey = importKey;
        this.aggiorna(mese, righe);
    }

    /** Ricalcola i totali dalle righe: i totali non si possono modificare separatamente. */
    public void aggiorna(String mese, List<RigaReport> righe) {
        this.mese = mese;
        this.righe = righe;
        this.totArrivi = righe.stream().map(RigaReport::arrivi).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totPrelievi = righe.stream().map(RigaReport::prelievi).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totValoreAcquisti = righe.stream().map(RigaReport::valoreAcquisti).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void rinominaAzienda(String nuova) { this.azienda = nuova; }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getMagazzinoId() { return magazzinoId; }
    public String getMese() { return mese; }
    public String getAzienda() { return azienda; }
    public String getMagazzinoNome() { return magazzinoNome; }
    public String getAutore() { return autore; }
    public BigDecimal getTotArrivi() { return totArrivi; }
    public BigDecimal getTotPrelievi() { return totPrelievi; }
    public BigDecimal getTotValoreAcquisti() { return totValoreAcquisti; }
    public List<RigaReport> getRighe() { return righe; }
    public String getImportKey() { return importKey; }
}
