package it.stockmanager.azienda;

import it.stockmanager.azienda.AziendaDto.AziendaRequest;
import it.stockmanager.azienda.AziendaDto.AziendaView;
import it.stockmanager.magazzino.Magazzino;
import it.stockmanager.magazzino.MagazzinoDto.MagazzinoView;
import it.stockmanager.magazzino.MagazzinoRepository;
import it.stockmanager.movimento.MovimentoDto.MovimentoView;
import it.stockmanager.movimento.MovimentoRepository;
import it.stockmanager.oggetto.Oggetto;
import it.stockmanager.oggetto.OggettoDto.OggettoView;
import it.stockmanager.oggetto.OggettoRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dati dell'azienda: albero completo per il caricamento e rinomina. */
@Service
public class AziendaService {

    private final AziendaRepository azienda;
    private final MagazzinoRepository magazzini;
    private final OggettoRepository oggetti;
    private final MovimentoRepository movimenti;

    public AziendaService(AziendaRepository azienda, MagazzinoRepository magazzini,
                          OggettoRepository oggetti, MovimentoRepository movimenti) {
        this.azienda = azienda;
        this.magazzini = magazzini;
        this.oggetti = oggetti;
        this.movimenti = movimenti;
    }

    /** Tre letture (magazzini, articoli, movimenti) raggruppate in memoria: niente N+1 query. */
    @Transactional(readOnly = true)
    public AziendaView albero(UUID owner) {
        Map<UUID, List<MovimentoView>> movPerOggetto = movimenti.findByOwnerId(owner).stream()
                .sorted(Comparator.comparing((it.stockmanager.movimento.Movimento m) -> m.getData()))
                .collect(Collectors.groupingBy(m -> m.getOggettoId(),
                        Collectors.mapping(MovimentoView::from, Collectors.toList())));
        Map<UUID, List<OggettoView>> oggPerMagazzino = oggetti.findByOwnerId(owner).stream()
                .collect(Collectors.groupingBy(Oggetto::getMagazzinoId,
                        Collectors.mapping(o -> OggettoView.from(o,
                                movPerOggetto.getOrDefault(o.getId(), List.of())), Collectors.toList())));
        List<MagazzinoView> viste = magazzini.findByOwnerId(owner).stream()
                .sorted(Comparator.comparing(Magazzino::getNome))
                .map(m -> MagazzinoView.from(m, oggPerMagazzino.getOrDefault(m.getId(), List.of())))
                .toList();
        String nome = azienda.findById(owner).map(Azienda::getNome).orElse("");
        return new AziendaView(nome, viste);
    }

    @Transactional
    public void rinomina(UUID owner, AziendaRequest r) {
        String nome = "";
        if (r.nome() != null) {
            nome = r.nome().trim();
        }
        Azienda a = azienda.findById(owner).orElseGet(() -> new Azienda(owner, ""));
        a.rinomina(nome);
        azienda.save(a);
    }
}
