package it.stockmanager.oggetto;

import it.stockmanager.movimento.MovimentoDto.MovimentoView;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class OggettoDto {

    private OggettoDto() {}

    /** Il nome puo' essere vuoto mentre l'utente compila l'articolo; categoria e unita' sono obbligatorie. */
    public record OggettoRequest(
            @Size(max = 120) String nome,
            @Size(max = 64) String sku,
            @NotBlank @Size(max = 60) String categoria,
            @NotBlank @Size(max = 16) String unita,
            @NotNull @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal sogliaMin) {}

    public record OggettoView(UUID id, String nome, String sku, String categoria, String unita,
                              BigDecimal sogliaMin, List<MovimentoView> movimenti) {
        public static OggettoView from(Oggetto o, List<MovimentoView> movimenti) {
            return new OggettoView(o.getId(), o.getNome(), o.getSku(), o.getCategoria(), o.getUnita(),
                    o.getSogliaMin(), movimenti);
        }
    }
}
