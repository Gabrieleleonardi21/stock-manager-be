package it.stockmanager.movimento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Movimento di magazzino: arrivo (con prezzo d'acquisto) o prelievo. */
@Entity
@Table(name = "movimenti")
public class Movimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "oggetto_id", nullable = false)
    private UUID oggettoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal qta;

    @Column(precision = 14, scale = 4)
    private BigDecimal prezzo;

    @Column(nullable = false, length = 500)
    private String nota;

    protected Movimento() {}

    public Movimento(UUID ownerId, UUID oggettoId, Tipo tipo, LocalDate data, BigDecimal qta, BigDecimal prezzo, String nota) {
        this.ownerId = ownerId;
        this.oggettoId = oggettoId;
        this.tipo = tipo;
        this.data = data;
        this.qta = qta;
        this.prezzo = prezzo;
        this.nota = nota;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getOggettoId() { return oggettoId; }
    public Tipo getTipo() { return tipo; }
    public LocalDate getData() { return data; }
    public BigDecimal getQta() { return qta; }
    public BigDecimal getPrezzo() { return prezzo; }
    public String getNota() { return nota; }
}
