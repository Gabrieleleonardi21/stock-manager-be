package it.stockmanager.magazzino;

import it.stockmanager.oggetto.OggettoDto.OggettoView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class MagazzinoDto {

    private MagazzinoDto() {}

    public record MagazzinoRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 200) String luogo,
            @Size(max = 120) String responsabile) {}

    public record MagazzinoView(UUID id, String nome, String luogo, String responsabile, List<OggettoView> oggetti) {
        public static MagazzinoView from(Magazzino m, List<OggettoView> oggetti) {
            return new MagazzinoView(m.getId(), m.getNome(), m.getLuogo(), m.getResponsabile(), oggetti);
        }
    }
}
