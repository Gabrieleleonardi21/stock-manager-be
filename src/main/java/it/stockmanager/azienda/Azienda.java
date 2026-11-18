package it.stockmanager.azienda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Dati dell'azienda di un utente: un solo record per utente (la chiave e' l'id utente). */
@Entity
@Table(name = "azienda")
public class Azienda {

    @Id
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 120)
    private String nome;

    protected Azienda() {}

    public Azienda(UUID ownerId, String nome) {
        this.ownerId = ownerId;
        this.nome = nome;
    }

    public void rinomina(String nome) {
        this.nome = nome;
    }

    public UUID getOwnerId() { return ownerId; }
    public String getNome() { return nome; }
}
