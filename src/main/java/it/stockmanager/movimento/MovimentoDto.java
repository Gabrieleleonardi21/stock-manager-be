package it.stockmanager.movimento;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class MovimentoDto {

    private MovimentoDto() {}

    public record MovimentoRequest(
            @NotNull Tipo tipo,
            @NotNull LocalDate data,
            @NotNull @jakarta.validation.constraints.Positive @Digits(integer = 11, fraction = 3) BigDecimal qta,
            @PositiveOrZero @Digits(integer = 10, fraction = 4) BigDecimal prezzo,
            @Size(max = 500) String nota) {}

    public record MovimentoView(UUID id, Tipo tipo, LocalDate data, BigDecimal qta, BigDecimal prezzo, String nota) {
        public static MovimentoView from(Movimento m) {
            return new MovimentoView(m.getId(), m.getTipo(), m.getData(), m.getQta(), m.getPrezzo(), m.getNota());
        }
    }
}
