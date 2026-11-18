package it.stockmanager.oggetto;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OggettoRepository extends JpaRepository<Oggetto, UUID> {

    Optional<Oggetto> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Oggetto> findByOwnerId(UUID ownerId);

    long countByMagazzinoId(UUID magazzinoId);

    /** Blocca la riga dell'articolo: due prelievi simultanei non possono superare la giacenza insieme. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oggetto o where o.id = :id and o.ownerId = :ownerId")
    Optional<Oggetto> bloccaPerMovimento(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
