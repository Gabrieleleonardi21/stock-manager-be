package it.stockmanager.assistente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTO dell'assistente. Il client manda solo i messaggi: il prompt di sistema lo scrive il server. */
public final class AssistenteDto {

    private AssistenteDto() {}

    /** Ruolo del messaggio: esattamente i valori accettati dall'API di Anthropic. */
    public enum Ruolo { user, assistant }

    /** Un messaggio: massimo 2000 caratteri. */
    public record Messaggio(@NotNull Ruolo ruolo, @NotBlank @Size(max = 2000) String contenuto) {}

    /** Cronologia: da 1 a 20 messaggi (limite della sezione 6 del piano). */
    public record AssistenteRequest(@NotNull @Size(min = 1, max = 20) List<@Valid Messaggio> messaggi) {}

    public record AssistenteResponse(String risposta) {}
}
