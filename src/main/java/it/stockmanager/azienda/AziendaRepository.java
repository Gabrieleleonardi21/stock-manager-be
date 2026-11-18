package it.stockmanager.azienda;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AziendaRepository extends JpaRepository<Azienda, UUID> {
}
