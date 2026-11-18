package it.stockmanager.movimento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentoRepository extends JpaRepository<Movimento, UUID> {

    Optional<Movimento> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Movimento> findByOwnerId(UUID ownerId);

    long countByOggettoId(UUID oggettoId);

    /** Somma delle quantita' di un tipo per un articolo: la giacenza si calcola da qui, mai salvata. */
    @Query("select coalesce(sum(m.qta), 0) from Movimento m where m.oggettoId = :oggettoId and m.tipo = :tipo")
    BigDecimal sommaQta(@Param("oggettoId") UUID oggettoId, @Param("tipo") Tipo tipo);
}
