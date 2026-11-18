package it.stockmanager.oggetto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Articolo di un magazzino. La giacenza non e' un campo: e' derivata dai movimenti. */
@Entity
@Table(name = "oggetti")
public class Oggetto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "magazzino_id", nullable = false)
    private UUID magazzinoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, length = 16)
    private String unita;

    @Column(name = "soglia_min", nullable = false, precision = 14, scale = 3)
    private BigDecimal sogliaMin;

    protected Oggetto() {}

    public Oggetto(UUID ownerId, UUID magazzinoId, String nome, String sku, String categoria, String unita, BigDecimal sogliaMin) {
        this.ownerId = ownerId;
        this.magazzinoId = magazzinoId;
        this.nome = nome;
        this.sku = sku;
        this.categoria = categoria;
        this.unita = unita;
        this.sogliaMin = sogliaMin;
    }

    public void aggiorna(String nome, String sku, String categoria, String unita, BigDecimal sogliaMin) {
        this.nome = nome;
        this.sku = sku;
        this.categoria = categoria;
        this.unita = unita;
        this.sogliaMin = sogliaMin;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getMagazzinoId() { return magazzinoId; }
    public String getNome() { return nome; }
    public String getSku() { return sku; }
    public String getCategoria() { return categoria; }
    public String getUnita() { return unita; }
    public BigDecimal getSogliaMin() { return sogliaMin; }
}
