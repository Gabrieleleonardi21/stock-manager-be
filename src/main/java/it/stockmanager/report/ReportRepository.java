package it.stockmanager.report;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ReportMensile, UUID> {

    Optional<ReportMensile> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<ReportMensile> findByOwnerIdAndMagazzinoIdAndMese(UUID ownerId, UUID magazzinoId, String mese);

    Optional<ReportMensile> findByOwnerIdAndImportKey(UUID ownerId, String importKey);

    Page<ReportMensile> findByOwnerIdOrderByMeseDescCreatedAtDesc(UUID ownerId, Pageable pageable);
}
