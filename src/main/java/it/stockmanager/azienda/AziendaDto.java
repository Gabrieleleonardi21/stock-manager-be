package it.stockmanager.azienda;

import it.stockmanager.magazzino.MagazzinoDto.MagazzinoView;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AziendaDto {

    private AziendaDto() {}

    public record AziendaRequest(@Size(max = 120) String nome) {}

    /** Albero completo: azienda, magazzini, articoli e movimenti dell'utente. */
    public record AziendaView(String nome, List<MagazzinoView> magazzini) {}
}
