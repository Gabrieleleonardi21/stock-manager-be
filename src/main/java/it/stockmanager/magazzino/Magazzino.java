package it.stockmanager.magazzino;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** Magazzino di proprieta' di un utente. */
@Entity
@Table(name = "magazzini")
public class Magazzino {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 200)
    private String luogo;

    @Column(nullable = false, length = 120)
    private String responsabile;

    @Column(name = "import_key", length = 64)
    private String importKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Magazzino() {}

    public Magazzino(UUID ownerId, String nome, String luogo, String responsabile, String importKey) {
        this.ownerId = ownerId;
        this.nome = nome;
        this.luogo = luogo;
        this.responsabile = responsabile;
        this.importKey = importKey;
    }

    public void aggiorna(String nome, String luogo, String responsabile) {
        this.nome = nome;
        this.luogo = luogo;
        this.responsabile = responsabile;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getNome() { return nome; }
    public String getLuogo() { return luogo; }
    public String getResponsabile() { return responsabile; }
    public String getImportKey() { return importKey; }
}
