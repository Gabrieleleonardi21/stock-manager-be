package it.stockmanager.importazione;

import it.stockmanager.movimento.MovimentoDto.MovimentoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Un magazzino del browser da importare. `chiave` e' l'id locale: rende l'import ripetibile senza duplicati. */
public final class ImportDto {

    private ImportDto() {}

    public record MagazzinoImport(
            @NotBlank @Size(max = 64) String chiave,
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 200) String luogo,
            @Size(max = 120) String responsabile,
            @Size(max = 500) List<@Valid OggettoImport> oggetti) {}

    public record OggettoImport(
            @Size(max = 120) String nome,
            @Size(max = 64) String sku,
            @NotBlank @Size(max = 60) String categoria,
            @NotBlank @Size(max = 16) String unita,
            @NotNull @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal sogliaMin,
            @Size(max = 5000) List<@Valid MovimentoRequest> movimenti) {}

    public record ImportEsito(boolean nuovo, java.util.UUID magazzinoId) {}
}
