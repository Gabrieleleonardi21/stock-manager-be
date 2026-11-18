package it.stockmanager.magazzino;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MagazzinoRepository extends JpaRepository<Magazzino, UUID> {

    Optional<Magazzino> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Magazzino> findByOwnerId(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    Optional<Magazzino> findByOwnerIdAndImportKey(UUID ownerId, String importKey);
}
